export type StatusAgendamento =
  | 'AGENDADO'
  | 'CONFIRMADO'
  | 'EM_ANDAMENTO'
  | 'CONCLUIDO'
  | 'CANCELADO';

export const STATUS_AGENDAMENTO_OPTIONS: StatusAgendamento[] = [
  'AGENDADO',
  'CONFIRMADO',
  'EM_ANDAMENTO',
  'CONCLUIDO',
  'CANCELADO'
];

export interface AgendamentoClienteResumo {
  id: number;
  nome: string;
  telefone: string | null;
  veiculo: string | null;
  placa: string | null;
}

export interface AgendamentoServicoResumo {
  id: number;
  nome: string;
  preco: number;
  duracaoMinutos: number;
}

export interface Agendamento {
  id: number;
  cliente: AgendamentoClienteResumo;
  servico: AgendamentoServicoResumo;
  data: string;
  horario: string;
  status: StatusAgendamento;
  observacoes: string | null;
}

export interface AgendamentoPayload {
  clienteId: number;
  servicoId: number;
  data: string;
  horario: string;
  status: StatusAgendamento;
  observacoes: string | null;
}
