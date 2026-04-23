export type UserRole = 'ADMIN' | 'ATENDENTE';

export interface AuthUser {
  id: number;
  nome: string;
  email: string;
  role: UserRole;
}

export interface LoginPayload {
  email: string;
  senha: string;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresAt: string;
  user: AuthUser;
}
