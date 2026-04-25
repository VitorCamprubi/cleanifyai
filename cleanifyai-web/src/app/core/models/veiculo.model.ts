export interface VeiculoRequest {
  clienteId: number;
  marca: string;
  modelo: string;
  placa?: string | null;
  cor?: string | null;
  anoModelo?: number | null;
  observacoes?: string | null;
}

export interface Veiculo {
  id: number;
  clienteId: number;
  clienteNome: string;
  marca: string;
  modelo: string;
  placa?: string | null;
  cor?: string | null;
  anoModelo?: number | null;
  observacoes?: string | null;
}
