export interface Servico {
  id: number;
  nome: string;
  descricao: string | null;
  preco: number;
  duracaoMinutos: number;
  ativo: boolean;
}

export type ServicoPayload = Omit<Servico, 'id'>;
