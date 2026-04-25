export type TipoLancamento = 'ENTRADA' | 'SAIDA';

export type FormaPagamento = 'DINHEIRO' | 'PIX' | 'DEBITO' | 'CREDITO' | 'BOLETO' | 'OUTROS';

export interface LancamentoRequest {
  tipo: TipoLancamento;
  valor: number;
  formaPagamento: FormaPagamento;
  dataLancamento: string; // yyyy-MM-dd
  descricao: string;
  ordemId?: number | null;
}

export interface Lancamento {
  id: number;
  tipo: TipoLancamento;
  valor: number;
  formaPagamento: FormaPagamento;
  dataLancamento: string;
  descricao: string;
  ordemId?: number | null;
  registradoEm: string;
}

export interface TotalPorForma {
  formaPagamento: FormaPagamento;
  entradas: number;
  saidas: number;
}

export interface ResumoFinanceiro {
  inicio: string;
  fim: string;
  totalEntradas: number;
  totalSaidas: number;
  saldo: number;
  quantidadeLancamentos: number;
  porForma: TotalPorForma[];
}

export const FORMA_PAGAMENTO_LABEL: Record<FormaPagamento, string> = {
  DINHEIRO: 'Dinheiro',
  PIX: 'PIX',
  DEBITO: 'Debito',
  CREDITO: 'Credito',
  BOLETO: 'Boleto',
  OUTROS: 'Outros'
};

export const FORMA_PAGAMENTO_OPTIONS: FormaPagamento[] = [
  'DINHEIRO',
  'PIX',
  'DEBITO',
  'CREDITO',
  'BOLETO',
  'OUTROS'
];

export const TIPO_LANCAMENTO_LABEL: Record<TipoLancamento, string> = {
  ENTRADA: 'Entrada',
  SAIDA: 'Saida'
};
