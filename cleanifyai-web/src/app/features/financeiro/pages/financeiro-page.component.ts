import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';

import {
  CategoriaFinanceira,
  FORMA_PAGAMENTO_LABEL,
  FORMA_PAGAMENTO_OPTIONS,
  FormaPagamento,
  Lancamento,
  LancamentoRequest,
  ResumoFinanceiro,
  TIPO_LANCAMENTO_LABEL,
  TipoLancamento
} from '../../../core/models/financeiro.model';
import { OrdemServico } from '../../../core/models/ordem.model';
import { AuthService } from '../../../core/services/auth.service';
import { FinanceiroApiService } from '../../../core/services/financeiro-api.service';
import { HttpErrorService } from '../../../core/services/http-error.service';
import { OrdensApiService } from '../../../core/services/ordens-api.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-financeiro-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './financeiro-page.component.html',
  styleUrl: './financeiro-page.component.scss'
})
export class FinanceiroPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(FinanceiroApiService);
  private readonly ordensApi = inject(OrdensApiService);
  private readonly httpErrorService = inject(HttpErrorService);
  private readonly toastService = inject(ToastService);
  private readonly authService = inject(AuthService);

  readonly formaLabel = FORMA_PAGAMENTO_LABEL;
  readonly tipoLabel = TIPO_LANCAMENTO_LABEL;
  readonly formasPagamento = FORMA_PAGAMENTO_OPTIONS;

  readonly hoje = this.formatarHoje();

  readonly filtroForm = this.fb.nonNullable.group({
    inicio: [this.hoje, Validators.required],
    fim: [this.hoje, Validators.required]
  });

  readonly lancamentoForm = this.fb.nonNullable.group({
    tipo: ['ENTRADA' as TipoLancamento, Validators.required],
    valor: [0, [Validators.required, Validators.min(0.01)]],
    formaPagamento: ['DINHEIRO' as FormaPagamento, Validators.required],
    dataLancamento: [this.hoje, Validators.required],
    descricao: ['', [Validators.required, Validators.maxLength(200)]],
    ordemId: [null as number | null],
    categoriaId: [null as number | null]
  });

  resumo: ResumoFinanceiro | null = null;
  lancamentos: Lancamento[] = [];
  ordens: OrdemServico[] = [];
  categorias: CategoriaFinanceira[] = [];

  carregando = false;
  salvando = false;
  estornandoId: number | null = null;
  erro = '';

  ngOnInit(): void {
    this.carregar();
  }

  get isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  carregar(): void {
    if (this.filtroForm.invalid) {
      this.filtroForm.markAllAsTouched();
      return;
    }

    const { inicio, fim } = this.filtroForm.getRawValue();
    this.carregando = true;
    this.erro = '';

    forkJoin({
      lancamentos: this.api.listar(inicio, fim),
      resumo: this.api.resumo(inicio, fim),
      ordens: this.ordensApi.listar(),
      categorias: this.api.listarCategorias()
    }).subscribe({
      next: ({ lancamentos, resumo, ordens, categorias }) => {
        this.lancamentos = lancamentos;
        this.resumo = resumo;
        this.ordens = ordens.filter((o) => o.status === 'CONCLUIDA' || o.status === 'ENTREGUE' || o.status === 'EM_EXECUCAO');
        this.categorias = categorias;
        this.carregando = false;
      },
      error: (error) => {
        this.erro = this.httpErrorService.obterMensagem(error, 'Nao foi possivel carregar o financeiro.');
        this.carregando = false;
      }
    });
  }

  categoriasParaTipo(): CategoriaFinanceira[] {
    const tipo = this.lancamentoForm.controls.tipo.value;
    return this.categorias.filter((c) => {
      if (c.tipo === 'AMBOS') return true;
      return tipo === 'ENTRADA' ? c.tipo === 'RECEITA' : c.tipo === 'DESPESA';
    });
  }

  aoSelecionarOrdem(event: Event): void {
    const target = event.target as HTMLSelectElement;
    const valor = target.value;
    if (!valor || valor === 'null') {
      return;
    }
    const id = Number(valor);
    const ordem = this.ordens.find((o) => o.id === id);
    if (!ordem) {
      return;
    }
    this.lancamentoForm.patchValue({
      valor: Number(ordem.valorTotal),
      descricao: this.lancamentoForm.controls.descricao.value || `Pagamento OS #${ordem.id} - ${ordem.clienteNome}`
    });
  }

  registrar(): void {
    if (!this.isAdmin) {
      this.toastService.error('Apenas administradores podem registrar lancamentos.');
      return;
    }
    if (this.lancamentoForm.invalid) {
      this.lancamentoForm.markAllAsTouched();
      return;
    }

    const value = this.lancamentoForm.getRawValue();
    const payload: LancamentoRequest = {
      tipo: value.tipo,
      valor: Number(value.valor),
      formaPagamento: value.formaPagamento,
      dataLancamento: value.dataLancamento,
      descricao: value.descricao.trim(),
      ordemId: value.ordemId ? Number(value.ordemId) : null,
      categoriaId: value.categoriaId ? Number(value.categoriaId) : null
    };

    this.salvando = true;
    this.erro = '';

    this.api.registrar(payload).subscribe({
      next: () => {
        this.salvando = false;
        this.toastService.success('Lancamento registrado.');
        this.lancamentoForm.reset({
          tipo: 'ENTRADA',
          valor: 0,
          formaPagamento: 'DINHEIRO',
          dataLancamento: this.hoje,
          descricao: '',
          ordemId: null,
          categoriaId: null
        });
        this.carregar();
      },
      error: (error) => {
        this.salvando = false;
        const mensagem = this.httpErrorService.obterMensagem(error, 'Nao foi possivel registrar o lancamento.');
        this.erro = mensagem;
        this.toastService.error(mensagem);
      }
    });
  }

  estornar(lancamento: Lancamento): void {
    if (!this.isAdmin) {
      this.toastService.error('Apenas administradores podem estornar.');
      return;
    }
    if (!window.confirm(`Estornar o lancamento "${lancamento.descricao}"?`)) {
      return;
    }
    this.estornandoId = lancamento.id;
    this.api.estornar(lancamento.id).subscribe({
      next: () => {
        this.estornandoId = null;
        this.toastService.success('Lancamento estornado.');
        this.carregar();
      },
      error: (error) => {
        this.estornandoId = null;
        this.toastService.error(this.httpErrorService.obterMensagem(error, 'Nao foi possivel estornar.'));
      }
    });
  }

  setHoje(): void {
    this.filtroForm.patchValue({ inicio: this.hoje, fim: this.hoje });
    this.carregar();
  }

  setMesAtual(): void {
    const hoje = new Date();
    const primeiro = new Date(hoje.getFullYear(), hoje.getMonth(), 1);
    this.filtroForm.patchValue({
      inicio: this.formatarData(primeiro),
      fim: this.hoje
    });
    this.carregar();
  }

  totalForma(forma: FormaPagamento): number {
    if (!this.resumo) {
      return 0;
    }
    const item = this.resumo.porForma.find((p) => p.formaPagamento === forma);
    if (!item) {
      return 0;
    }
    return Number(item.entradas) - Number(item.saidas);
  }

  private formatarHoje(): string {
    return this.formatarData(new Date());
  }

  private formatarData(date: Date): string {
    const tzOffset = date.getTimezoneOffset() * 60000;
    return new Date(date.getTime() - tzOffset).toISOString().slice(0, 10);
  }
}
