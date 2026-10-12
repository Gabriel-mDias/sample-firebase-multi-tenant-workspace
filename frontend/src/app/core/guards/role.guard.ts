import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { ProductAuthorizationData } from '../auth/auth.model';
export const roleGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.sessionReady())
    return router.createUrlTree(['/login'], { queryParams: { returnTo: state.url } });
  const data = route.data as Partial<ProductAuthorizationData>;
  if (
    !Array.isArray(data.roles) ||
    !data.roles.length ||
    !['global', 'tenant'].includes(data.authorizationScope ?? '') ||
    (data.roleMode !== undefined && !['all', 'any'].includes(data.roleMode))
  )
    return router.createUrlTree(['/acesso-negado']);
  return (
    auth.isAuthorized(data.roles, data.authorizationScope!, data.roleMode) ||
    router.createUrlTree(['/acesso-negado'])
  );
};
