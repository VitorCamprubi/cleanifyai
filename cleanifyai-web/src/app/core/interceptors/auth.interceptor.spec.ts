import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let router: jasmine.SpyObj<Router>;
  let authService: AuthServiceStub;

  beforeEach(() => {
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    router.navigate.and.resolveTo(true);
    authService = new AuthServiceStub();

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router }
      ]
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('adds the bearer token to protected requests', () => {
    authService.token = 'jwt-token';

    http.get('/api/clientes').subscribe();

    const request = httpMock.expectOne('/api/clientes');
    expect(request.request.headers.get('Authorization')).toBe('Bearer jwt-token');
    request.flush([]);
  });

  it('does not add the bearer token to login requests', () => {
    authService.token = 'jwt-token';

    http.post('/api/auth/login', { email: 'admin@cleanifyai.local', senha: 'admin123' }).subscribe();

    const request = httpMock.expectOne('/api/auth/login');
    expect(request.request.headers.has('Authorization')).toBeFalse();
    request.flush({});
  });

  it('refreshes the session and retries once when the backend returns 401', () => {
    authService.token = 'jwt-token';
    authService.refreshToken = 'refresh-token';
    authService.refreshSession.and.returnValue(of({ token: 'new-jwt-token' }));

    http.get('/api/clientes').subscribe({
      next: (response) => {
        expect(response).toEqual([{ id: 1 }]);
      },
      error: fail
    });

    const firstRequest = httpMock.expectOne('/api/clientes');
    expect(firstRequest.request.headers.get('Authorization')).toBe('Bearer jwt-token');
    firstRequest.flush({ message: 'Token expirado' }, { status: 401, statusText: 'Unauthorized' });

    const retryRequest = httpMock.expectOne('/api/clientes');
    expect(retryRequest.request.headers.get('Authorization')).toBe('Bearer new-jwt-token');
    retryRequest.flush([{ id: 1 }]);

    expect(authService.refreshSession).toHaveBeenCalled();
    expect(authService.logout).not.toHaveBeenCalled();
  });

  it('logs out and redirects when the backend returns 401 without refresh token', () => {
    authService.token = 'jwt-token';
    authService.refreshToken = null;

    http.get('/api/clientes').subscribe({
      error: () => undefined
    });

    const request = httpMock.expectOne('/api/clientes');
    request.flush({ message: 'Token expirado' }, { status: 401, statusText: 'Unauthorized' });

    expect(authService.logout).toHaveBeenCalledWith(false);
    expect(router.navigate).toHaveBeenCalledWith(['/login'], { queryParams: { session: 'expired' } });
  });

  it('logs out and redirects when refresh fails', () => {
    authService.token = 'jwt-token';
    authService.refreshToken = 'refresh-token';
    authService.refreshSession.and.returnValue(throwError(() => new Error('refresh failed')));

    http.get('/api/clientes').subscribe({
      error: () => undefined
    });

    const request = httpMock.expectOne('/api/clientes');
    request.flush({ message: 'Token expirado' }, { status: 401, statusText: 'Unauthorized' });

    expect(authService.refreshSession).toHaveBeenCalled();
    expect(authService.logout).toHaveBeenCalledWith(false);
    expect(router.navigate).toHaveBeenCalledWith(['/login'], { queryParams: { session: 'expired' } });
  });
});

class AuthServiceStub {
  token: string | null = null;
  refreshToken: string | null = null;
  logout = jasmine.createSpy('logout');
  refreshSession = jasmine.createSpy('refreshSession');
}
