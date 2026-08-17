import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { SnackbarService } from '@app/shared/services/snackbar.service';
import { throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const snackbar = inject(SnackbarService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      switch (error.status) {
        case 401:
          router.navigate(['/auth/login']);
          break;
        case 429:
          snackbar.open('Too many requests. Please try again later.', 'Close', {
            duration: 5000,
          });
          break;
        default:
          if (error.status >= 500) {
            snackbar.open(
              'A server error occurred. Please try again later.',
              'Close',
              { duration: 5000 }
            );
          }
          break;
      }
      return throwError(() => error);
    })
  );
};
