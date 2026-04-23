import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthUser, LoginPayload, LoginResponse, UserRole } from '../models/auth.model';

const AUTH_STORAGE_KEY = 'cleanifyai.auth.session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly endpoint = `${environment.apiUrl}/auth/login`;
  private readonly sessionSubject = new BehaviorSubject<LoginResponse | null>(this.carregarSessao());

  readonly session$ = this.sessionSubject.asObservable();

  login(payload: LoginPayload): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(this.endpoint, payload).pipe(
      tap((response) => this.salvarSessao(response))
    );
  }

  logout(redirecionar = true): void {
    localStorage.removeItem(AUTH_STORAGE_KEY);
    this.sessionSubject.next(null);

    if (redirecionar) {
      void this.router.navigate(['/login']);
    }
  }

  isAuthenticated(): boolean {
    const session = this.sessionSubject.value;
    if (!session) {
      return false;
    }

    if (this.tokenExpirado(session.token)) {
      this.logout(false);
      return false;
    }

    return true;
  }

  isAdmin(): boolean {
    return this.hasRole('ADMIN');
  }

  hasRole(role: UserRole): boolean {
    return this.usuarioAtual?.role === role;
  }

  get token(): string | null {
    return this.sessionSubject.value?.token ?? null;
  }

  get usuarioAtual(): AuthUser | null {
    return this.sessionSubject.value?.user ?? null;
  }

  private salvarSessao(response: LoginResponse): void {
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(response));
    this.sessionSubject.next(response);
  }

  private carregarSessao(): LoginResponse | null {
    const raw = localStorage.getItem(AUTH_STORAGE_KEY);
    if (!raw) {
      return null;
    }

    try {
      const session = JSON.parse(raw) as LoginResponse;
      if (this.tokenExpirado(session.token)) {
        localStorage.removeItem(AUTH_STORAGE_KEY);
        return null;
      }

      return session;
    } catch {
      localStorage.removeItem(AUTH_STORAGE_KEY);
      return null;
    }
  }

  private tokenExpirado(token: string): boolean {
    const payload = this.decodingPayload(token);
    if (!payload || typeof payload['exp'] !== 'number') {
      return true;
    }

    return payload['exp'] * 1000 <= Date.now();
  }

  private decodingPayload(token: string): Record<string, unknown> | null {
    const partes = token.split('.');
    if (partes.length < 2) {
      return null;
    }

    try {
      const base64 = partes[1].replace(/-/g, '+').replace(/_/g, '/');
      const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
      return JSON.parse(window.atob(padded)) as Record<string, unknown>;
    } catch {
      return null;
    }
  }
}
