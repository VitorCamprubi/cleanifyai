import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';

import { Cliente } from '../../../core/models/cliente.model';
import {
  ItemOrdemRequest,
  OrdemServico,
  OrdemServicoRequest,
  STATUS_ORDEM_LABEL,
  STATUS_ORDEM_NEXT,
  StatusOrdem
} from '../../../core/models/ordem.model';
import { Servico } from '../../../core/models/servico.model';
import { ClientesApiService } from '../../../core/services/clientes-api.service';
import { HttpErrorService } from '../../../core/services/http-error.service';
import { OrdensApiService } from '../../../core/services/ordens-api.service';
import { ServicosApiService } from '../../../core/services/servicos-api.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-ordens-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './ordens-page.component.html',
  styleUrl: './ordens-page.component.scss'
})
export class OrdensPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly ordensApi = inject(OrdensApiService);
  private readonly clientesApi = inject(ClientesApiService);
  private readonly servicosApi = inject(ServicosApiService);
  private readonly httpErrorService = inject(HttpErrorService);
  private readonly toastService = inject(ToastService);

  readonly statusLabel = STATUS_ORDEM_LABEL;
  readonly statusFiltros: Array<{ value: StatusOrdem | ''; label: string }> = [
    { value: '', label: 'Todas' },
    { value: 'ABERTA', label: 'Abertas' },
    { value: 'EM_EXECUCAO', label: 'Em execucao' },
    { value: 'CONCLUIDA', label: 'Concluidas' },
    { value: 'ENTREGUE', label: 'Entregues' },
    { value: 'CANCELADA', label: 'Canceladas' }
  ];

  readonly form = this.fb.nonNullable.group({
    clienteId: [0, [Validators.required, Validators.min(1)]],
    observacoes: ['', Validators.maxLength(500)],
    itens: this.fb.array([] as FormGroup[])
  });

  ordens: OrdemServico[] = [];
  clientes: Cliente[] = [];
  servicos: Servico[] = [];

  filtroAtual: StatusOrdem | '' = '';
  carregando = false;
  salvando = false;
  ordemEmEdicaoId: number | null = null;
  ordemProcessandoId: number | null = null;
  erro = '';

  ngOnInit(): void {
    this.carregarDadosIniciais();
  }

  get itensFormArray(): FormArray<FormGroup> {
    return this.form.controls.itens as FormArray<FormGroup>;
  }

  get totalCalculado(): number {
    return this.itensFormArray.controls.reduce((acc, group) => {
      const qtd = Number(group.get('quantidade')?.value) || 0;
      const unit = Number(group.get('valorUnitario')?.value) || 0;
      return acc + qtd * unit;
    }, 0);
  }

  carregarDadosIniciais(): void {
    this.carregando = true;
    this.erro = '';

    forkJoin({
      clientes: this.clientesApi.listar(),
      servicos: this.servicosApi.listar(),
      ordens: this.ordensApi.listar(this.filtroAtual || undefined)
    }).subscribe({
      next: ({ clientes, servicos, ordens }) => {
        this.clientes = [...clientes].sort((a, b) => a.nome.localeCompare(b.nome));
        this.servicos = [...servicos].filter((s) => s.ativo).sort((a, b) => a.nome.localeCompare(b.nome));
        this.ordens = ordens;
        this.carregando = false;
      },
      error: (error) => {
        const mensagem = this.httpErrorService.obterMensagem(error, 'Nao foi possivel carregar as ordens.');
        this.erro = mensagem;
        this.carregando = false;
      }
    });
  }

  trocarFiltro(status: StatusOrdem | ''): void {
    this.filtroAtual = status;
    this.recarregarOrdens();
  }

  recarregarOrdens(): void {
    this.carregando = true;
    this.ordensApi.listar(this.filtroAtual || undefined).subscribe({
      next: (ordens) => {
        this.ordens = ordens;
        this.carregando = false;
      },
      error: (error) => {
        this.erro = this.httpErrorService.obterMensagem(error, 'Nao foi possivel atualizar a lista.');
        this.carregando = false;
      }
    });
  }

  adicionarItem(): void {
    const grupo = this.fb.nonNullable.group({
      servicoId: [0, [Validators.required, Validators.min(1)]],
      descricao: ['', Validators.maxLength(200)],
      quantidade: [1, [Validators.required, Validators.min(1)]],
      valorUnitario: [0, [Validators.required, Validators.min(0.01)]]
    });

    grupo.get('servicoId')?.valueChanges.subscribe((id) => {
      const servico = this.servicos.find((s) => s.id === Number(id));
      if (servico) {
        if (!grupo.get('descricao')?.value) {
          grupo.patchValue({ descricao: servico.nome });
        }
        if (Number(grupo.get('valorUnitario')?.value) <= 0) {
          grupo.patchValue({ valorUnitario: Number(servico.preco) });
        }
      }
    });

    this.itensFormArray.push(grupo);
  }

  removerItem(index: number): void {
    this.itensFormArray.removeAt(index);
  }

  salvar(): void {
    if (this.form.invalid || this.itensFormArray.length === 0) {
      this.form.markAllAsTouched();
      this.itensFormArray.markAllAsTouched();
      this.erro = this.itensFormArray.length === 0 ? 'Adicione ao menos um item a ordem.' : '';
      return;
    }

    this.salvando = true;
    this.erro = '';

    const payload = this.montarPayload();
    const atualizando = this.ordemEmEdicaoId !== null;
    const request$ = atualizando
      ? this.ordensApi.atualizar(this.ordemEmEdicaoId!, payload)
      : this.ordensApi.criar(payload);

    request$.subscribe({
      next: (ordem) => {
        this.salvando = false;
        this.aplicarOrdemSalva(ordem);
        this.toastService.success(atualizando ? 'OS atualizada.' : 'OS criada.');
        this.cancelarEdicao();
      },
      error: (error) => {
        this.salvando = false;
        const mensagem = this.httpErrorService.obterMensagem(error, 'Nao foi possivel salvar a OS.');
        this.erro = mensagem;
        this.toastService.error(mensagem);
      }
    });
  }

  editar(ordem: OrdemServico): void {
    if (ordem.status !== 'ABERTA' && ordem.status !== 'EM_EXECUCAO') {
      this.toastService.error('Esta OS nao pode mais ser editada.');
      return;
    }

    this.ordemEmEdicaoId = ordem.id;
    this.itensFormArray.clear();
    this.form.patchValue({
      clienteId: ordem.clienteId,
      observacoes: ordem.observacoes ?? ''
    });

    ordem.itens.forEach((item) => {
      const grupo = this.fb.nonNullable.group({
        servicoId: [item.servicoId, [Validators.required, Validators.min(1)]],
        descricao: [item.descricao, Validators.maxLength(200)],
        quantidade: [item.quantidade, [Validators.required, Validators.min(1)]],
        valorUnitario: [Number(item.valorUnitario), [Validators.required, Validators.min(0.01)]]
      });
      this.itensFormArray.push(grupo);
    });

    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  cancelarEdicao(): void {
    this.ordemEmEdicaoId = null;
    this.itensFormArray.clear();
    this.form.reset({
      clienteId: 0,
      observacoes: '',
      itens: []
    });
  }

  acoesProximas(status: StatusOrdem): StatusOrdem[] {
    return STATUS_ORDEM_NEXT[status];
  }

  alterarStatus(ordem: OrdemServico, status: StatusOrdem): void {
    this.ordemProcessandoId = ordem.id;
    this.ordensApi.atualizarStatus(ordem.id, status).subscribe({
      next: (ordemAtualizada) => {
        this.ordemProcessandoId = null;
        this.aplicarOrdemSalva(ordemAtualizada);
        this.toastService.success(`Status alterado para ${this.statusLabel[status]}.`);
      },
      error: (error) => {
        this.ordemProcessandoId = null;
        const mensagem = this.httpErrorService.obterMensagem(error, 'Nao foi possivel atualizar o status.');
        this.toastService.error(mensagem);
      }
    });
  }

  cancelarOrdem(ordem: OrdemServico): void {
    if (!window.confirm(`Cancelar a OS #${ordem.id} de ${ordem.clienteNome}?`)) {
      return;
    }
    this.ordemProcessandoId = ordem.id;
    this.ordensApi.cancelar(ordem.id).subscribe({
      next: (ordemAtualizada) => {
        this.ordemProcessandoId = null;
        this.aplicarOrdemSalva(ordemAtualizada);
        this.toastService.success('OS cancelada.');
      },
      error: (error) => {
        this.ordemProcessandoId = null;
        this.toastService.error(this.httpErrorService.obterMensagem(error, 'Nao foi possivel cancelar a OS.'));
      }
    });
  }

  itemProcessando(id: number): boolean {
    return this.ordemProcessandoId === id;
  }

  trackById(_index: number, item: { id: number }): number {
    return item.id;
  }

  private montarPayload(): OrdemServicoRequest {
    const value = this.form.getRawValue();
    const itens: ItemOrdemRequest[] = this.itensFormArray.controls.map((grupo) => ({
      servicoId: Number(grupo.get('servicoId')?.value),
      descricao: this.normalizar(grupo.get('descricao')?.value as string),
      quantidade: Number(grupo.get('quantidade')?.value),
      valorUnitario: Number(grupo.get('valorUnitario')?.value)
    }));

    return {
      clienteId: Number(value.clienteId),
      observacoes: this.normalizar(value.observacoes),
      itens
    };
  }

  private aplicarOrdemSalva(ordem: OrdemServico): void {
    const idx = this.ordens.findIndex((o) => o.id === ordem.id);
    if (idx >= 0) {
      this.ordens[idx] = ordem;
    } else {
      this.ordens = [ordem, ...this.ordens];
    }
    this.ordens = [...this.ordens].sort((a, b) => b.abertaEm.localeCompare(a.abertaEm));
  }

  private normalizar(valor: string | null | undefined): string | null {
    if (!valor) {
      return null;
    }
    const normalizado = valor.trim();
    return normalizado ? normalizado : null;
  }
}
