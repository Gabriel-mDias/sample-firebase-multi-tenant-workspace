import { Routes } from '@angular/router';
import { Acoes } from '../../core/auth/acoes';
import { roleGuard } from '../../core/guards/role.guard';
import { ProductAuthorizationData } from '../../core/auth/auth.model';
export default [
  {
    path: 'list',
    canActivate: [roleGuard],
    data: {
      roles: [Acoes.CONSULTAR_DEMO],
      authorizationScope: 'tenant',
    } satisfies ProductAuthorizationData,
    loadComponent: () =>
      import('./list/demo-registro-list.component').then(
        (module) => module.DemoRegistroListComponent,
      ),
  },
  {
    path: 'form',
    canActivate: [roleGuard],
    data: {
      roles: [Acoes.INSERIR_DEMO],
      authorizationScope: 'tenant',
    } satisfies ProductAuthorizationData,
    loadComponent: () =>
      import('./form/demo-registro-form.component').then(
        (module) => module.DemoRegistroFormComponent,
      ),
  },
  {
    path: 'form/:id',
    canActivate: [roleGuard],
    data: {
      roles: [Acoes.ALTERAR_DEMO],
      authorizationScope: 'tenant',
    } satisfies ProductAuthorizationData,
    loadComponent: () =>
      import('./form/demo-registro-form.component').then(
        (module) => module.DemoRegistroFormComponent,
      ),
  },
  {
    path: 'view/:id',
    canActivate: [roleGuard],
    data: {
      roles: [Acoes.CONSULTAR_DEMO],
      authorizationScope: 'tenant',
    } satisfies ProductAuthorizationData,
    loadComponent: () =>
      import('./view/demo-registro-view.component').then(
        (module) => module.DemoRegistroViewComponent,
      ),
  },
  { path: '', redirectTo: 'list', pathMatch: 'full' },
] satisfies Routes;
