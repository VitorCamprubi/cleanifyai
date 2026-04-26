export type TipoLancamento = 'ENTRADA' | 'SAIDA';

export type FormaPagamento = 'DINHEIRO' | 'PIX' | 'DEBITO' | 'CREDITO' | 'BOLETO' | 'OUTROS';

export type TipoCategoria = 'RECEITA' | 'DESPESA' | 'AMBOS';

export interface CategoriaFinanceiraRequest {
  nome: string;
  tipo: TipoCategoria;
  cor?: string | null;
}

export interface CategoriaFinanceira {
  id: number;
  nome: string;
  tipo: TipoCategoria;
  cor?: string | null;
}

export interface LancamentoRequest {
  tipo: TipoLancamento;
  valor: number;
  formaPagamento: FormaPagamento;
  dataLancamento: string; // yyyy-MM-dd
  descricao: string;
  ordemId?: number | null;
  categoriaId?: number | null;
}

export interface Lancamento {
  id: number;
  tipo: TipoLancamento;
  valor: number;
  formaPagamento: FormaPagamento;
  dataLancamento: string;
  descricao: string;
  ordemId?: number | null;
  categoriaId?: number | null;
  categoriaNome?: string | null;
  categoriaCor?: string | null;
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

export const TIPO_CATEGORIA_LABEL: Record<TipoCategoria, string> = {
  RECEITA: 'Receita',
  DESPESA: 'Despesa',
  AMBOS: 'Ambos'
};

export const TIPO_CATEGORIA_OPTIONS: TipoCategoria[] = ['RECEITA', 'DESPESA', 'AMBOS'];
