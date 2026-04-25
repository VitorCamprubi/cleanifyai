export type StatusOrdem = 'ABERTA' | 'EM_EXECUCAO' | 'CONCLUIDA' | 'ENTREGUE' | 'CANCELADA';

export interface ItemOrdemRequest {
  servicoId: number;
  descricao?: string | null;
  quantidade: number;
  valorUnitario: number;
}

export interface ItemOrdem {
  id: number;
  servicoId: number;
  servicoNome: string;
  descricao: string;
  quantidade: number;
  valorUnitario: number;
  valorTotal: number;
}

export interface OrdemServicoRequest {
  clienteId: number;
  agendamentoId?: number | null;
  observacoes?: string | null;
  itens: ItemOrdemRequest[];
}

export interface OrdemServico {
  id: number;
  clienteId: number;
  clienteNome: string;
  clientePlaca?: string | null;
  clienteVeiculo?: string | null;
  agendamentoId?: number | null;
  status: StatusOrdem;
  valorTotal: number;
  abertaEm: string;
  fechadaEm?: string | null;
  observacoes?: string | null;
  itens: ItemOrdem[];
}

export interface AtualizarStatusOrdemRequest {
  status: StatusOrdem;
}

export const STATUS_ORDEM_LABEL: Record<StatusOrdem, string> = {
  ABERTA: 'Aberta',
  EM_EXECUCAO: 'Em execucao',
  CONCLUIDA: 'Concluida',
  ENTREGUE: 'Entregue',
  CANCELADA: 'Cancelada'
};

export const STATUS_ORDEM_NEXT: Record<StatusOrdem, StatusOrdem[]> = {
  ABERTA: ['EM_EXECUCAO', 'CANCELADA'],
  EM_EXECUCAO: ['CONCLUIDA', 'CANCELADA'],
  CONCLUIDA: ['ENTREGUE', 'CANCELADA'],
  ENTREGUE: [],
  CANCELADA: []
};
