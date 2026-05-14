import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const isAuthRequest = request.url.includes('/auth/');

  const authorizedRequest = !isAuthRequest && authService.token
    ? withBearer(request, authService.token)
    : request;

  return next(authorizedRequest).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !isAuthRequest && authService.refreshToken) {
        return authService.refreshSession().pipe(
          switchMap((session) => next(withBearer(request, session.token))),
          catchError((refreshError) => {
            encerrarSessao(authService, router);
            return throwError(() => refreshError);
          })
        );
      }

      if (error.status === 401 && !isAuthRequest) {
        encerrarSessao(authService, router);
      }

      return throwError(() => error);
    })
  );
};

function withBearer(request: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return request.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });
}

function encerrarSessao(authService: AuthService, router: Router): void {
  authService.logout(false);
  void router.navigate(['/login'], { queryParams: { session: 'expired' } });
}
