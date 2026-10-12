import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { Acoes } from './core/auth/acoes';
import { ProductAuthorizationData } from './core/auth/auth.model';
export const routes: Routes = [
  { path: 'login', loadChildren: () => import('./features/login/login.routes') },
  {
    path: 'acesso-negado',
    loadChildren: () => import('./features/acesso-negado/acesso-negado.routes'),
  },
  {
    path: 'tenants',
    canActivate: [authGuard],
    loadChildren: () => import('./features/tenants/tenants.routes'),
  },
  {
    path: 'demo-registro',
    canActivate: [roleGuard],
    data: {
      roles: [Acoes.CONSULTAR_DEMO],
      authorizationScope: 'tenant',
    } satisfies ProductAuthorizationData,
    loadChildren: () => import('./features/demo-registro/demo-registro.routes'),
  },
  {
    path: '',
    canActivate: [authGuard],
    pathMatch: 'full',
    loadChildren: () => import('./features/home/home.routes'),
  },
  { path: '**', redirectTo: '' },
];
