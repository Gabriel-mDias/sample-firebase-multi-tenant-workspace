import { Injectable, inject, signal } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';
import { AuthorizationRequirement } from './acoes';

/**
 * Fachada fina sobre o {@link KeycloakService} para o restante da aplicação (casca, menu.builder,
 * guards e as features). Concentra num único ponto a leitura do token — nenhum componente deve
 * ler `tokenParsed` diretamente.
 *
 * Aplicação single-tenant: não há claim de organização/tenant a resolver aqui.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly keycloak = inject(KeycloakService);

  /**
   * Sinal de "revisão" do token. Como o `KeycloakService` não é reativo, incrementamos este
   * contador em pontos de virada (refresh de token) para reavaliar os `computed` derivados.
   * No fluxo atual (login redireciona e recarrega a app) a leitura direta já basta, mas manter
   * a dependência num signal permite reatividade futura sem reescrever os consumidores.
   */
  private readonly rev = signal(0);

  /** Força reavaliação dos derivados (chamar após refresh de token, se implementado). */
  refresh(): void {
    this.rev.update((v) => v + 1);
  }

  private get token(): Record<string, unknown> | undefined {
    return this.keycloak.getKeycloakInstance()?.tokenParsed as Record<string, unknown> | undefined;
  }

  isLoggedIn(): boolean {
    return this.keycloak.isLoggedIn();
  }

  /** Redireciona ao Keycloak; ao voltar, o browser recarrega em `redirectUri` (default: URL atual). */
  login(redirectUri?: string): Promise<void> {
    return this.keycloak.login(redirectUri ? { redirectUri } : undefined);
  }

  /** Encerra a sessão e volta à raiz — o authGuard então dispara novo login. */
  logout(): Promise<void> {
    return this.keycloak.logout(window.location.origin);
  }

  /** Realm roles do token (`realm_access.roles`). */
  roles(): string[] {
    this.rev();
    return this.keycloak.getUserRoles(true) ?? [];
  }

  isAuthorized(requirement: AuthorizationRequirement): boolean {
    const owned = this.roles();
    return requirement.actions.every((action) => owned.includes(action));
  }

  username(): string {
    this.rev();
    const t = this.token;
    return (t?.['name'] as string) || (t?.['preferred_username'] as string) || '';
  }

  email(): string {
    this.rev();
    return (this.token?.['email'] as string) || '';
  }

  /** Iniciais para o avatar do menu (1–2 letras a partir do nome; fallback: inicial do email). */
  initials(): string {
    const name = this.username().trim();
    if (name) {
      const parts = name.split(/\s+/).filter(Boolean);
      const first = parts[0]?.[0] ?? '';
      const last = parts.length > 1 ? parts[parts.length - 1][0] : '';
      return (first + last).toUpperCase();
    }
    const mail = this.email().trim();
    return mail ? mail[0].toUpperCase() : '';
  }
}

/**
 * Forma mínima que o {@link buildMenu} consome do {@link AuthService}. Permite testar a montagem
 * do menu com um stub simples, sem depender do Keycloak.
 */
export interface MenuAuthContext {
  isAuthorized(requirement: AuthorizationRequirement): boolean;
}
