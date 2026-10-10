import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  return (
    auth.sessionReady() ||
    inject(Router).createUrlTree(['/login'], { queryParams: { returnTo: state.url } })
  );
};
