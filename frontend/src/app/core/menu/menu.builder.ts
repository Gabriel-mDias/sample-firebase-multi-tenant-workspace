import { GemsSideMenuItem } from '@gabriel-mdias/angular-gems-sdk';
import { Acoes, Acao } from '../auth/acoes';
import { AuthorizationScope } from '../auth/auth.model';
interface MenuAuthContext {
  isAuthorized(actions: readonly Acao[], scope: AuthorizationScope): boolean;
}
export function buildMenu(auth: MenuAuthContext): GemsSideMenuItem[] {
  return auth.isAuthorized([Acoes.CONSULTAR_DEMO], 'tenant')
    ? [
        {
          label: 'Registros de exemplo',
          icon: 'fa-solid fa-layer-group',
          route: '/demo-registro/list',
        },
      ]
    : [];
}
