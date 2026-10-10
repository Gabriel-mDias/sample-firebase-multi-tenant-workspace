import { Component, inject } from '@angular/core';
import { GemsNavigationService } from '@gabriel-mdias/angular-gems-sdk';
import { AuthService } from '../../core/auth/auth.service';
import { Acoes } from '../../core/auth/acoes';
@Component({
  selector: 'app-tenants',
  imports: [],
  templateUrl: './tenants.component.html',
  styleUrl: './tenants.component.css',
})
export class TenantsComponent {
  protected readonly auth = inject(AuthService);
  private readonly navigation = inject(GemsNavigationService);
  protected async select(alias: string): Promise<void> {
    // A seleção invalida e fixa o alias de forma síncrona: o snapshot imediatamente posterior
    // identifica geração, UID, sessão e destino desta operação.
    const selection = this.auth.selectTenant(alias);
    const snapshot = this.auth.capture();
    try {
      await selection;
      if (this.auth.isCurrent(snapshot) && this.auth.currentTenant()?.alias === alias) {
        const requested = this.auth.consumeReturnPath();
        this.navigation.navigate([
          requested !== '/' && requested !== '/tenants'
            ? requested
            : this.auth.isAuthorized([Acoes.CONSULTAR_DEMO], 'tenant')
              ? '/demo-registro/list'
              : '/',
        ]);
      }
    } catch {
      /* A fachada anuncia falha sanitizada e mantém seleção vazia. */
    }
  }
}
