export interface Cliente {
  id: number;
  nome: string;
  telefone: string | null;
  email: string | null;
  observacoes: string | null;
}

export type ClientePayload = Omit<Cliente, 'id'>;
