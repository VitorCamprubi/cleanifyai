import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const isLoginRequest = request.url.endsWith('/auth/login');

  const authorizedRequest = !isLoginRequest && authService.token
    ? request.clone({
        setHeaders: {
          Authorization: `Bearer ${authService.token}`
        }
      })
    : request;

  return next(authorizedRequest).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !isLoginRequest) {
        authService.logout(false);
        void router.navigate(['/login'], { queryParams: { session: 'expired' } });
      }

      return throwError(() => error);
    })
  );
};
