import { Component, computed, inject } from '@angular/core';
import { AuthService } from '../../core/auth/auth.service';
import { buildMenu } from '../../core/menu/menu.builder';

/**
 * Home vazia de boas-vindas — destino de `/` após login.
 * Sem widgets, indicadores ou dados de exemplo: os domínios reais do App
 * (dashboards, indicadores) ainda não existem.
 *
 * Role aqui não é nível (ADMIN/USER) e sim ação — um perfil pode não ter nenhuma ação de consulta,
 * e nesse caso o menu lateral fica vazio. Sem aviso explícito, essa tela em branco se lê como
 * defeito.
 */
@Component({
  selector: 'app-home',
  imports: [],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css',
})
export class HomeComponent {
  private readonly auth = inject(AuthService);

  protected readonly semAcesso = computed(() => buildMenu(this.auth).length === 0);
}
