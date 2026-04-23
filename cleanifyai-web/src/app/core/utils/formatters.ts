import { StatusAgendamento } from '../models/agendamento.model';

export function formatarTelefoneBr(telefone: string | null | undefined): string {
  if (!telefone) {
    return 'Sem telefone';
  }

  const digitos = telefone.replace(/\D/g, '');
  const nacional = digitos.startsWith('55') && digitos.length >= 12 ? digitos.slice(2) : digitos;

  if (nacional.length === 11) {
    return `(${nacional.slice(0, 2)}) ${nacional.slice(2, 7)}-${nacional.slice(7)}`;
  }

  if (nacional.length === 10) {
    return `(${nacional.slice(0, 2)}) ${nacional.slice(2, 6)}-${nacional.slice(6)}`;
  }

  return telefone;
}

export function formatarStatusAgendamento(status: StatusAgendamento): string {
  return status.replaceAll('_', ' ');
}
