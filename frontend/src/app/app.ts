import { Component, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { GemsSideMenuComponent, GemsSideMenuConfig } from '@gabriel-mdias/angular-gems-sdk';

import { AuthService } from './core/auth/auth.service';
import { AppFooterComponent } from './core/footer/app-footer.component';
import { buildMenu } from './core/menu/menu.builder';

/** Nome genérico exibido na casca autenticada (sem branding de terceiros). */
const APP_NAME = 'App';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, GemsSideMenuComponent, AppFooterComponent],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly appName = APP_NAME;

  private readonly currentUrl = signal(this.router.url);

  /** Estado de colapso do menu lateral, refletido em `.app-shell.menu-collapsed`
   * (app.css) para que o rodapé fixo (que consome `--app-menu-width`)
   * acompanhe a largura real do menu. */
  protected readonly menuCollapsed = signal(false);

  constructor() {
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationEnd) {
        this.currentUrl.set(event.urlAfterRedirects);
      }
    });
  }

  readonly showShell = computed(
    () => this.auth.isLoggedIn() && !this.currentUrl().startsWith('/acesso-negado'),
  );

  readonly menuConfig = computed<GemsSideMenuConfig>(() => ({
    headerTitle: this.appName,
    items: buildMenu(this.auth),
  }));

  onLogout(): void {
    this.auth.logout();
  }

  onMenuCollapsedChange(collapsed: boolean): void {
    this.menuCollapsed.set(collapsed);
  }
}
