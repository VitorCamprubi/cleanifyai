export interface Cliente {
  id: number;
  nome: string;
  telefone: string | null;
  email: string | null;
  veiculo: string | null;
  placa: string | null;
  observacoes: string | null;
}

export type ClientePayload = Omit<Cliente, 'id'>;
