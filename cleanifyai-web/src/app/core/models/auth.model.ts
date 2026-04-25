export type UserRole = 'ADMIN' | 'ATENDENTE';

export interface AuthUser {
  id: number;
  empresaId: number;
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
