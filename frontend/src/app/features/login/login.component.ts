import { Component, DestroyRef, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import {
  GemsFieldErrorComponent,
  GemsInputPasswordComponent,
  GemsNavigationService,
} from '@gabriel-mdias/angular-gems-sdk';
import { AuthService } from '../../core/auth/auth.service';
import { safeReturnPath } from '../../core/auth/safe-return-path';
import { StaleSessionError } from '../../core/auth/stale-session.error';
import { PasswordAccessibilityDirective } from '../../shared/forms/password-accessibility.directive';

@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule,
    GemsFieldErrorComponent,
    GemsInputPasswordComponent,
    PasswordAccessibilityDirective,
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly navigation = inject(GemsNavigationService);
  private readonly route = inject(ActivatedRoute);
  readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });
  protected readonly pending = signal(false);
  protected readonly message = signal('');
  protected readonly error = signal('');
  private operation = 0;
  private navigatedGeneration = -1;
  private readonly destroy = inject(DestroyRef);

  constructor() {
    let previousUid = this.auth.authenticatedUser()?.uid;
    this.auth.invalidated.pipe(takeUntilDestroyed(this.destroy)).subscribe(() => {
      const uid = this.auth.authenticatedUser()?.uid;
      if (this.auth.invalidationReason() !== 'identity' || (previousUid && uid !== previousUid)) {
        this.operation++;
        this.pending.set(false);
        this.error.set('');
        this.message.set('');
        this.form.reset();
      }
      previousUid = uid;
    });
    this.destroy.onDestroy(() => this.operation++);
    effect(() => {
      this.auth.generation();
      this.form.controls.password.reset();
    });
    effect(() => {
      if (this.auth.sessionReady()) this.finish();
    });
    effect(() => {
      if (this.pending()) this.form.disable();
      else this.form.enable();
    });
  }

  async submit(): Promise<void> {
    if (this.pending()) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      this.error.set('Confira os campos para continuar.');
      return;
    }
    await this.login(() =>
      this.auth.signInWithEmailAndPassword(
        this.form.controls.email.value.trim(),
        this.form.controls.password.value,
      ),
    );
  }

  async google(): Promise<void> {
    if (!this.pending()) await this.login(() => this.auth.signInWithGoogle());
  }

  async resetPassword(): Promise<void> {
    if (this.pending()) return;
    this.form.controls.email.markAsTouched();
    if (this.form.controls.email.invalid) {
      this.error.set('Informe um e-mail válido para recuperar o acesso.');
      return;
    }
    const id = ++this.operation;
    const snapshot = this.auth.capture();
    const email = this.form.controls.email.value.trim();
    this.pending.set(true);
    this.error.set('');
    try {
      await this.auth.resetPassword(email);
    } catch {
      /* Mesma mensagem para qualquer resultado. */
    } finally {
      if (id !== this.operation || this.destroy.destroyed || !this.auth.isCurrent(snapshot)) return;
      this.message.set(
        'Se a conta estiver habilitada, você receberá um e-mail com as instruções para definir sua senha.',
      );
      this.pending.set(false);
    }
  }

  private async login(operation: () => Promise<void>): Promise<void> {
    const id = ++this.operation;
    this.pending.set(true);
    this.error.set('');
    this.message.set('');
    try {
      await operation();
      if (id === this.operation && this.auth.sessionReady()) this.finish();
    } catch (error) {
      if (error instanceof StaleSessionError) return;
      if (id === this.operation && !this.destroy.destroyed)
        this.error.set('Não foi possível entrar. Confira seu acesso ou tente novamente.');
    } finally {
      if (id === this.operation && !this.destroy.destroyed) {
        this.pending.set(false);
        this.form.controls.password.reset();
      }
    }
  }

  private finish(): void {
    if (this.destroy.destroyed || this.navigatedGeneration === this.auth.generation()) return;
    this.navigatedGeneration = this.auth.generation();
    const target = safeReturnPath(this.route.snapshot.queryParamMap.get('returnTo'));
    if (this.auth.currentTenant() || !this.auth.tenants().length)
      this.navigation.navigate([target]);
    else {
      this.auth.rememberReturnPath(target);
      this.navigation.navigate(['/tenants']);
    }
  }
}
