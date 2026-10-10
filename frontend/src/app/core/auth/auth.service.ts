import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom, Subject } from 'rxjs';
import { IDENTITY_ADAPTER, IdentityUser } from './identity.adapter';
import { AuthStore } from './auth.store';
import { Acao, belongsToScope } from './acoes';
import { AuthContext, AuthMe, AuthorizationScope, SessionSnapshot, Tenant } from './auth.model';
import { StaleSessionError } from './stale-session.error';
import { safeReturnPath } from './safe-return-path';

const TENANT_UNAVAILABLE_MESSAGE = 'Não foi possível acessar esta empresa. Escolha novamente.';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly identity = inject(IDENTITY_ADAPTER);
  private readonly store = inject(AuthStore);
  private user: IdentityUser | null = null;
  private session = 0;
  private identityIntent = 0;
  private initialAnonymousAllowed = true;
  private restoreFromObserver = true;
  private readonly pendingSignIns = new Set<number>();
  private readonly cancelledLoginUids = new Set<string>();
  private pendingObservation: IdentityUser | null = null;
  private targetAlias: string | null = null;
  private returnPath = '/';
  private bootstrap: Promise<void> = Promise.resolve();
  readonly generation = signal(0);
  readonly invalidated = new Subject<void>();
  readonly invalidationReason = signal<'identity' | 'tenant' | 'logout'>('logout');
  readonly authenticatedUser = signal<IdentityUser | null>(null);
  readonly me = signal<AuthMe | null>(null);
  readonly currentTenant = signal<Tenant | null>(null);
  readonly context = signal<AuthContext | null>(null);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly tenants = computed(() => this.me()?.tenants ?? []);
  readonly effectiveActions = computed(
    () => this.context()?.actions.filter((action) => belongsToScope(action, 'tenant')) ?? [],
  );
  readonly sessionReady = computed(() => !!this.me() && !this.loading());

  constructor() {
    const stop = this.identity.observe((user) => this.observeIdentity(user));
    inject(DestroyRef).onDestroy(() => {
      stop();
      this.invalidated.complete();
    });
  }

  async signInWithEmailAndPassword(email: string, password: string): Promise<void> {
    await this.signIn(() => this.identity.signInWithEmailAndPassword(email, password));
  }

  async signInWithGoogle(): Promise<void> {
    await this.signIn(() => this.identity.signInWithGoogle());
  }

  async resetPassword(email: string): Promise<void> {
    try {
      await this.identity.resetPassword(email);
    } catch {
      /* Resposta genérica evita enumerar contas. */
    }
  }

  async signOut(): Promise<void> {
    this.cancelIdentityIntent();
    this.acceptIdentity(null);
    await this.identity.signOut();
  }

  async selectTenant(alias: string): Promise<void> {
    this.targetAlias = null;
    this.invalidate('tenant');
    if (!this.me() || !this.user || !this.tenants().some((tenant) => tenant.alias === alias)) {
      // A invalidação acima cancelou qualquer seleção pendente, cujo finally não limpa mais o loading.
      this.loading.set(false);
      this.error.set(TENANT_UNAVAILABLE_MESSAGE);
      throw new Error('tenant-denied');
    }
    this.targetAlias = alias;
    const snapshot = this.capture();
    this.loading.set(true);
    this.error.set('');
    try {
      const context = await firstValueFrom(this.store.getContext());
      if (!this.isCurrent(snapshot)) return;
      if (context.tenant.alias !== alias) throw new Error('tenant-mismatch');
      this.context.set(context);
      this.currentTenant.set(context.tenant);
    } catch {
      if (this.isCurrent(snapshot)) {
        this.targetAlias = null;
        this.context.set(null);
        this.currentTenant.set(null);
        this.error.set(TENANT_UNAVAILABLE_MESSAGE);
        throw new Error('tenant-unavailable');
      }
    } finally {
      if (snapshot.generation === this.generation()) this.loading.set(false);
    }
  }

  capture(): SessionSnapshot {
    return Object.freeze({
      generation: this.generation(),
      session: this.session,
      uid: this.user?.uid ?? '',
      alias: this.targetAlias,
      ready: !!this.currentTenant() && !this.loading(),
    });
  }

  isCurrent(snapshot: SessionSnapshot): boolean {
    return (
      snapshot.generation === this.generation() &&
      snapshot.session === this.session &&
      snapshot.uid === (this.user?.uid ?? '') &&
      snapshot.alias === this.targetAlias
    );
  }

  async idToken(snapshot: SessionSnapshot): Promise<string> {
    if (!this.isCurrent(snapshot) || !this.user) throw new StaleSessionError();
    return this.user.getIdToken();
  }

  expire(snapshot: SessionSnapshot): void {
    if (this.isCurrent(snapshot)) {
      this.cancelIdentityIntent();
      this.acceptIdentity(null);
    }
  }

  isAuthorized(
    actions: readonly Acao[],
    scope: AuthorizationScope,
    mode: 'all' | 'any' = 'any',
  ): boolean {
    if (
      !this.sessionReady() ||
      !actions.length ||
      !actions.every((action) => belongsToScope(action, scope))
    )
      return false;
    const owned =
      scope === 'global'
        ? (this.me()?.globalActions ?? [])
        : this.currentTenant()
          ? this.effectiveActions()
          : [];
    return mode === 'all'
      ? actions.every((action) => owned.includes(action))
      : actions.some((action) => owned.includes(action));
  }

  isLoggedIn(): boolean {
    return this.sessionReady();
  }
  rememberReturnPath(path: string): void {
    this.returnPath = safeReturnPath(path);
  }

  consumeReturnPath(): string {
    const path = this.returnPath;
    this.returnPath = '/';
    return safeReturnPath(path);
  }

  username(): string {
    return this.me()?.user.name ?? '';
  }

  email(): string {
    return this.me()?.user.email ?? '';
  }

  initials(): string {
    return this.username()
      .trim()
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0])
      .join('')
      .toUpperCase();
  }

  private async signIn(operation: () => Promise<IdentityUser>): Promise<void> {
    const intent = ++this.identityIntent;
    this.pendingSignIns.add(intent);
    this.pendingObservation = null;
    this.restoreFromObserver = false;
    this.targetAlias = null;
    this.me.set(null);
    this.loading.set(false);
    this.invalidate('identity');
    let user: IdentityUser;
    try {
      user = await operation();
    } catch (error) {
      this.pendingSignIns.delete(intent);
      if (intent !== this.identityIntent) {
        // Falha não revela UID: o evento ambíguo não confirma uma identidade.
        this.rejectAmbiguousObservation();
        throw new StaleSessionError();
      }
      throw error;
    }
    this.pendingSignIns.delete(intent);
    if (intent !== this.identityIntent) {
      this.cancelledLoginUids.add(user.uid);
      this.reconcileObservation();
      throw new StaleSessionError();
    }
    // O resultado da Promise confirma a tentativa, mas não pode substituir um
    // UID diferente já observado no Firebase durante essa mesma tentativa.
    if (this.hasConflictingObservation(user)) {
      this.identityIntent++;
      this.cancelledLoginUids.add(user.uid);
      this.restoreFromObserver = true;
      this.acceptIdentity(null);
      this.reconcileObservation();
      throw new StaleSessionError();
    }
    // Uma intenção explícita atual pode confirmar até um UID antes cancelado.
    this.cancelledLoginUids.delete(user.uid);
    this.pendingObservation = null;
    this.restoreFromObserver = true;
    this.acceptIdentity(user);
    const snapshot = this.capture();
    await this.bootstrap;
    if (intent !== this.identityIntent || !this.isCurrent(snapshot)) throw new StaleSessionError();
    if (!this.me()) throw new Error('account-unavailable');
  }

  private hasConflictingObservation(user: IdentityUser): boolean {
    return !!this.pendingObservation && this.pendingObservation.uid !== user.uid;
  }

  private cancelIdentityIntent(): void {
    this.initialAnonymousAllowed = false;
    this.identityIntent++;
    this.restoreFromObserver = false;
    this.pendingObservation = null;
  }

  private observeIdentity(user: IdentityUser | null): void {
    const initialAnonymous = this.initialAnonymousAllowed;
    this.initialAnonymousAllowed = false;
    if (!user) {
      // A primeira entrega anônima pode ter sido agendada antes do clique de
      // login. Sem identidade/logout/expiry anteriores, ela não revoga o clique.
      if (initialAnonymous && !this.user) return;
      this.pendingObservation = null;
      if (!this.user) {
        if (this.pendingSignIns.size || (this.restoreFromObserver && this.identityIntent > 0))
          this.cancelIdentityIntent();
        return;
      }
      this.cancelIdentityIntent();
      this.acceptIdentity(null);
      return;
    }
    // Revogar a identidade anterior precede qualquer classificação do evento.
    // Um UID cancelado não é confiável, mas tampouco permite manter outro UID.
    if (this.user && this.user.uid !== user.uid) {
      if (this.restoreFromObserver) this.identityIntent++;
      this.acceptIdentity(null);
    }
    // Firebase não atribui o callback a uma tentativa. Enquanto uma Promise pode
    // pertencer a login cancelado, ela precisa resolver para classificar seu UID.
    if (this.pendingSignIns.size) {
      this.pendingObservation = user;
      return;
    }
    if (!this.restoreFromObserver) return;
    if (this.user?.uid === user.uid) return;
    // Um UID conhecido de tentativa cancelada exige confirmação por novo login
    // explícito. Outros UIDs nativos continuam sincronizados entre abas.
    if (this.cancelledLoginUids.has(user.uid)) return;
    this.identityIntent++;
    this.acceptIdentity(user);
  }

  private reconcileObservation(): void {
    if (this.pendingSignIns.size || !this.pendingObservation) return;
    const user = this.pendingObservation;
    this.pendingObservation = null;
    this.observeIdentity(user);
  }

  private rejectAmbiguousObservation(): void {
    if (!this.pendingObservation) return;
    // A rejeição antiga reduz a confiança na proveniência, mas o UID nativo
    // continua sendo a última observação. Apagá-lo permitiria que a Promise de
    // uma intenção nova confirmasse outro UID sem um callback coerente.
    this.cancelledLoginUids.add(this.pendingObservation.uid);
    if (!this.user) this.restoreFromObserver = false;
  }

  private acceptIdentity(user: IdentityUser | null): void {
    if (user) this.initialAnonymousAllowed = false;
    if (user && this.user?.uid === user.uid && (this.me() || this.loading())) return;
    this.session++;
    this.user = user;
    this.targetAlias = null;
    this.authenticatedUser.set(user);
    this.me.set(null);
    this.invalidate(user ? 'identity' : 'logout');
    if (!user) {
      this.loading.set(false);
      this.bootstrap = Promise.resolve();
      return;
    }
    const snapshot = this.capture();
    this.loading.set(true);
    this.bootstrap = this.loadMe(snapshot);
  }

  private async loadMe(snapshot: SessionSnapshot): Promise<void> {
    try {
      const me = await firstValueFrom(this.store.getMe());
      if (this.isCurrent(snapshot)) this.me.set(me);
    } catch {
      if (this.isCurrent(snapshot)) {
        this.error.set('Não foi possível validar seu acesso. Tente novamente.');
        this.me.set(null);
      }
    } finally {
      if (this.isCurrent(snapshot)) this.loading.set(false);
    }
  }

  private invalidate(reason: 'identity' | 'tenant' | 'logout'): void {
    if (reason !== 'tenant') this.returnPath = '/';
    this.invalidationReason.set(reason);
    this.generation.update((value) => value + 1);
    this.currentTenant.set(null);
    this.context.set(null);
    this.invalidated.next();
  }
}
