import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  GemsNavigationService,
  GemsSideMenuComponent,
  GemsSideMenuConfig,
} from '@gabriel-mdias/angular-gems-sdk';
import { AuthService } from './core/auth/auth.service';
import { AppFooterComponent } from './core/footer/app-footer.component';
import { buildMenu } from './core/menu/menu.builder';
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, GemsSideMenuComponent, AppFooterComponent],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly navigation = inject(GemsNavigationService);
  protected readonly currentUrl = signal(this.router.url);
  protected readonly menuCollapsed = signal(false);
  protected readonly showShell = computed(
    () =>
      !!this.auth.me() &&
      !this.currentUrl().startsWith('/login') &&
      !this.currentUrl().startsWith('/acesso-negado'),
  );
  protected readonly menuConfig = computed<GemsSideMenuConfig>(() => ({
    headerTitle: 'G&Ms',
    items: buildMenu(this.auth),
  }));
  constructor() {
    this.router.events.pipe(takeUntilDestroyed(inject(DestroyRef))).subscribe((event) => {
      if (event instanceof NavigationEnd) this.currentUrl.set(event.urlAfterRedirects);
    });
    this.auth.invalidated.pipe(takeUntilDestroyed()).subscribe(() => {
      if (!this.auth.authenticatedUser() && !/^\/login(?:[?#]|$)/.test(this.router.url))
        this.navigation.navigate(['/login']);
    });
  }

  protected async onLogout(): Promise<void> {
    try {
      await this.auth.signOut();
    } catch {
      /* A sessão local já foi invalidada. */
    }
  }

  protected changeTenant(): void {
    this.navigation.navigate(['/tenants']);
  }
}
