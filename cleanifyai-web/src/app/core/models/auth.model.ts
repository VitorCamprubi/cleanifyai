export type UserRole = 'ADMIN' | 'ATENDENTE';

export interface AuthUser {
  id: number;
  empresaId: number;
  empresaNome: string;
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
  refreshToken: string;
  refreshExpiresAt: string;
  user: AuthUser;
}

export interface RegisterCompanyPayload {
  empresa: {
    nome: string;
    cnpj?: string;
    telefone?: string;
    email?: string;
  };
  admin: {
    nome: string;
    email: string;
    senha: string;
  };
}
