import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { environment } from '../../../environments/environment';
import { LoginResponse } from '../models/auth.model';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  const storageKey = 'cleanifyai.auth.session';
  let httpMock: HttpTestingController;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    localStorage.clear();
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    router.navigate.and.resolveTo(true);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: router }
      ]
    });

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('persists the session after a successful login', () => {
    const service = TestBed.inject(AuthService);
    const response = buildLoginResponse('ADMIN', 3600);

    let actualResponse: LoginResponse | undefined;
    service.login({ email: 'admin@cleanifyai.local', senha: 'admin123' }).subscribe((value) => {
      actualResponse = value;
    });

    const request = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
    expect(request.request.method).toBe('POST');
    request.flush(response);

    expect(actualResponse).toEqual(response);
    expect(service.isAuthenticated()).toBeTrue();
    expect(service.token).toBe(response.token);
    expect(service.refreshToken).toBe(response.refreshToken);
    expect(service.usuarioAtual?.role).toBe('ADMIN');
    expect(localStorage.getItem(storageKey)).toContain('admin@cleanifyai.local');
  });

  it('clears an expired session during bootstrap', () => {
    localStorage.setItem(storageKey, JSON.stringify(buildLoginResponse('ATENDENTE', -60, -60)));

    const service = TestBed.inject(AuthService);

    expect(service.isAuthenticated()).toBeFalse();
    expect(service.token).toBeNull();
    expect(localStorage.getItem(storageKey)).toBeNull();
  });

  it('keeps the session while the refresh token is still valid', () => {
    localStorage.setItem(storageKey, JSON.stringify(buildLoginResponse('ATENDENTE', -60, 3600)));

    const service = TestBed.inject(AuthService);

    expect(service.isAuthenticated()).toBeTrue();
    expect(service.token).toBeTruthy();
    expect(service.refreshToken).toBeTruthy();
  });

  it('refreshes and persists a rotated session', () => {
    localStorage.setItem(storageKey, JSON.stringify(buildLoginResponse('ADMIN', -60, 3600)));
    const service = TestBed.inject(AuthService);
    const refreshed = buildLoginResponse('ADMIN', 3600, 7200);

    let actualResponse: LoginResponse | undefined;
    service.refreshSession().subscribe((value) => {
      actualResponse = value;
    });

    const request = httpMock.expectOne(`${environment.apiUrl}/auth/refresh`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body.refreshToken).toBe('refresh-ADMIN');
    request.flush(refreshed);

    expect(actualResponse).toEqual(refreshed);
    expect(service.token).toBe(refreshed.token);
    expect(service.refreshToken).toBe(refreshed.refreshToken);
    expect(localStorage.getItem(storageKey)).toContain(refreshed.refreshToken);
  });

  it('logs out and redirects to login', () => {
    localStorage.setItem(storageKey, JSON.stringify(buildLoginResponse('ADMIN', 3600)));
    const service = TestBed.inject(AuthService);

    service.logout();

    const request = httpMock.expectOne(`${environment.apiUrl}/auth/logout`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body.refreshToken).toBe('refresh-ADMIN');
    request.flush(null);

    expect(service.isAuthenticated()).toBeFalse();
    expect(localStorage.getItem(storageKey)).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});

function buildLoginResponse(
  role: 'ADMIN' | 'ATENDENTE',
  expiresInSeconds: number,
  refreshExpiresInSeconds = 2592000
): LoginResponse {
  return {
    token: buildJwt(expiresInSeconds),
    tokenType: 'Bearer',
    expiresAt: new Date(Date.now() + expiresInSeconds * 1000).toISOString(),
    refreshToken: `refresh-${role}`,
    refreshExpiresAt: new Date(Date.now() + refreshExpiresInSeconds * 1000).toISOString(),
    user: {
      id: role === 'ADMIN' ? 1 : 2,
      empresaId: 1,
      empresaNome: 'CleanifyAI Demo',
      nome: role === 'ADMIN' ? 'Administrador' : 'Atendente',
      email: role === 'ADMIN' ? 'admin@cleanifyai.local' : 'atendente@cleanifyai.local',
      role
    }
  };
}

function buildJwt(expiresInSeconds: number): string {
  const header = encodeBase64Url({ alg: 'HS256', typ: 'JWT' });
  const payload = encodeBase64Url({
    sub: 'usuario@cleanifyai.local',
    exp: Math.floor(Date.now() / 1000) + expiresInSeconds
  });

  return `${header}.${payload}.assinatura`;
}

function encodeBase64Url(value: Record<string, unknown>): string {
  return btoa(JSON.stringify(value))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/g, '');
}
