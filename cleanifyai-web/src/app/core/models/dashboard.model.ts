import { StatusAgendamento } from './agendamento.model';

export interface ProximoAgendamento {
  id: number;
  clienteNome: string;
  servicoNome: string;
  data: string;
  horario: string;
  status: StatusAgendamento;
}

export interface DashboardResumo {
  totalClientes: number;
  totalServicosAtivos: number;
  totalAgendamentosDoDia: number;
  proximosAgendamentos: ProximoAgendamento[];
}
