import { GemsSideMenuItem } from '@gabriel-mdias/angular-gems-sdk';
import { Acoes, globalAuthorization } from '../auth/acoes';
import { MenuAuthContext } from '../auth/auth.service';

/**
 * Monta os itens do side-menu a partir das ações do token.
 *
 * Aplicação single-tenant: não há grupos por organização. A home (`/`) é uma tela vazia de
 * boas-vindas e não tem entrada de menu. Cada item depende só da ação de **consulta** do seu
 * recurso — um perfil sem nenhuma delas vê o menu vazio, e isso é o comportamento correto.
 *
 * <p>Um item de menu que dependesse da ação de escrita esconderia a tela de quem pode lê-la; um
 * que não dependesse de ação nenhuma mostraria a tela para quem toma 403 ao abri-la. A ação de
 * consulta é a única que responde "esta pessoa consegue abrir isto".
 *
 * Função pura (sem injeção): recebe um {@link MenuAuthContext} para ser testável sem Keycloak.
 * Princípio VI — o construtor de menu é o caso canônico de função de UI que MUST ter teste.
 */
export function buildMenu(auth: MenuAuthContext): GemsSideMenuItem[] {
  const items: GemsSideMenuItem[] = [];

  if (auth.isAuthorized(globalAuthorization(Acoes.CONSULTAR_PARCEIRO))) {
    items.push({ label: 'Parceiros', icon: 'fa-solid fa-handshake', route: '/parceiros/list' });
  }
  if (auth.isAuthorized(globalAuthorization(Acoes.CONSULTAR_SOLICITACAO))) {
    items.push({
      label: 'Solicitações',
      icon: 'fa-solid fa-list-check',
      route: '/solicitacoes/list',
    });
  }
  if (auth.isAuthorized(globalAuthorization(Acoes.CONSULTAR_AUDITORIA))) {
    items.push({ label: 'Auditoria', icon: 'fa-solid fa-clock-rotate-left', route: '/auditoria' });
  }

  return items;
}
