import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const premiumGuard = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.hasRole('PREMIUM') || authService.hasRole('ADMIN')) {
    return true;
  }

  return router.createUrlTree(['/pricing']);
};
