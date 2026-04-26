import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';

import { Agendamento, AgendamentoPayload, STATUS_AGENDAMENTO_OPTIONS, StatusAgendamento } from '../../../core/models/agendamento.model';
import { Cliente } from '../../../core/models/cliente.model';
import { Servico } from '../../../core/models/servico.model';
import { Veiculo } from '../../../core/models/veiculo.model';
import { AgendamentosApiService } from '../../../core/services/agendamentos-api.service';
import { ClientesApiService } from '../../../core/services/clientes-api.service';
import { DashboardRefreshService } from '../../../core/services/dashboard-refresh.service';
import { HttpErrorService } from '../../../core/services/http-error.service';
import { ServicosApiService } from '../../../core/services/servicos-api.service';
import { ToastService } from '../../../core/services/toast.service';
import { VeiculosApiService } from '../../../core/services/veiculos-api.service';
import { formatarStatusAgendamento } from '../../../core/utils/formatters';

type AgendamentoFormField = 'clienteId' | 'servicoId' | 'veiculoId' | 'data' | 'horario' | 'status' | 'observacoes';
type TipoAcaoAgenda = 'status' | 'cancelamento' | null;

@Component({
  selector: 'app-agendamentos-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './agendamentos-page.component.html',
  styleUrl: './agendamentos-page.component.scss'
})
export class AgendamentosPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly agendamentosApi = inject(AgendamentosApiService);
  private readonly clientesApi = inject(ClientesApiService);
  private readonly servicosApi = inject(ServicosApiService);
  private readonly veiculosApi = inject(VeiculosApiService);
  private readonly httpErrorService = inject(HttpErrorService);
  private readonly toastService = inject(ToastService);
  private readonly dashboardRefreshService = inject(DashboardRefreshService);

  readonly statusOptions = STATUS_AGENDAMENTO_OPTIONS;
  readonly dataMinima = this.obterDataMinima();
  readonly form = this.fb.nonNullable.group({
    clienteId: [0, [Validators.required, Validators.min(1)]],
    servicoId: [0, [Validators.required, Validators.min(1)]],
    veiculoId: [null as number | null],
    data: ['', Validators.required],
    horario: ['', Validators.required],
    status: ['AGENDADO' as StatusAgendamento, Validators.required],
    observacoes: ['', Validators.maxLength(500)]
  });

  clientes: Cliente[] = [];
  servicos: Servico[] = [];
  agendamentos: Agendamento[] = [];
  veiculosDoCliente: Veiculo[] = [];
  carregando = false;
  salvando = false;
  erro = '';
  agendamentoEmEdicaoId: number | null = null;
  agendamentoProcessandoId: number | null = null;
  tipoAcaoProcessando: TipoAcaoAgenda = null;

  ngOnInit(): void {
    this.carregarDadosIniciais();
    this.form.controls.clienteId.valueChanges.subscribe((clienteId) => {
      this.carregarVeiculosDoCliente(Number(clienteId));
    });
  }

  private carregarVeiculosDoCliente(clienteId: number): void {
    if (!clienteId || clienteId <= 0) {
      this.veiculosDoCliente = [];
      this.form.patchValue({ veiculoId: null }, { emitEvent: false });
      return;
    }
    this.veiculosApi.listar(clienteId).subscribe({
      next: (veiculos) => {
        this.veiculosDoCliente = veiculos;
        const atual = this.form.controls.veiculoId.value;
        if (atual && !veiculos.some((v) => v.id === atual)) {
          this.form.patchValue({ veiculoId: null }, { emitEvent: false });
        }
      },
      error: () => {
        this.veiculosDoCliente = [];
      }
    });
  }

  carregarDadosIniciais(exibirToastErro = false): void {
    this.carregando = true;
    this.erro = '';

    forkJoin({
      clientes: this.clientesApi.listar(),
      servicos: this.servicosApi.listar(),
      agendamentos: this.agendamentosApi.listar()
    }).subscribe({
      next: ({ clientes, servicos, agendamentos }) => {
        this.clientes = [...clientes].sort((a, b) => a.nome.localeCompare(b.nome));
        this.servicos = [...servicos].sort((a, b) => a.nome.localeCompare(b.nome));
        this.agendamentos = this.ordenarAgendamentos(agendamentos);
        this.carregando = false;
      },
      error: (error) => {
        const mensagem = this.httpErrorService.obterMensagem(error, 'Nao foi possivel carregar os dados da agenda.');
        this.erro = mensagem;
        this.carregando = false;
        if (exibirToastErro) {
          this.toastService.error(mensagem);
        }
      }
    });
  }

  salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.salvando = true;
    this.erro = '';

    const payload = this.criarPayload();
    const atualizando = this.agendamentoEmEdicaoId !== null;
    const request$ = atualizando
      ? this.agendamentosApi.atualizar(this.agendamentoEmEdicaoId!, payload)
      : this.agendamentosApi.criar(payload);

    request$.subscribe({
      next: (agendamento) => {
        this.salvando = false;
        this.aplicarAgendamentoSalvo(agendamento);
        this.dashboardRefreshService.solicitarAtualizacao();
        this.toastService.success(atualizando ? 'Agendamento atualizado com sucesso.' : 'Agendamento criado com sucesso.');
        this.cancelarEdicao();
      },
      error: (error) => {
        const mensagem = this.httpErrorService.obterMensagem(error, 'Nao foi possivel salvar o agendamento.');
        this.salvando = false;
        this.erro = mensagem;
        this.toastService.error(mensagem);
      }
    });
  }

  editar(agendamento: Agendamento): void {
    this.agendamentoEmEdicaoId = agendamento.id;
    this.erro = '';
    this.form.patchValue({
      clienteId: agendamento.cliente.id,
      servicoId: agendamento.servico.id,
      veiculoId: agendamento.veiculo?.id ?? null,
      data: agendamento.data,
      horario: this.formatarHorarioParaInput(agendamento.horario),
      status: agendamento.status,
      observacoes: agendamento.observacoes ?? ''
    });
  }

  alterarStatus(agendamento: Agendamento, status: StatusAgendamento): void {
    this.agendamentoProcessandoId = agendamento.id;
    this.tipoAcaoProcessando = 'status';
    this.erro = '';

    this.agendamentosApi.atualizarStatus(agendamento.id, status).subscribe({
      next: (agendamentoAtualizado) => {
        this.agendamentoProcessandoId = null;
        this.tipoAcaoProcessando = null;
        this.aplicarAgendamentoSalvo(agendamentoAtualizado);
        if (this.agendamentoEmEdicaoId === agendamentoAtualizado.id) {
          if (agendamentoAtualizado.status === 'CONCLUIDO' || agendamentoAtualizado.status === 'CANCELADO') {
            this.cancelarEdicao();
          } else {
            this.form.patchValue({ status: agendamentoAtualizado.status });
          }
        }
        this.dashboardRefreshService.solicitarAtualizacao();
        this.toastService.success(`Status alterado para ${this.rotuloStatus(status)}.`);
      },
      error: (error) => {
        const mensagem = this.httpErrorService.obterMensagem(error, 'Nao foi possivel alterar o status.');
        this.agendamentoProcessandoId = null;
        this.tipoAcaoProcessando = null;
        this.erro = mensagem;
        this.toastService.error(mensagem);
      }
    });
  }

  cancelar(agendamento: Agendamento): void {
    if (!window.confirm(`Cancelar o agendamento de ${agendamento.cliente.nome}?`)) {
      return;
    }

    this.agendamentoProcessandoId = agendamento.id;
    this.tipoAcaoProcessando = 'cancelamento';
    this.erro = '';

    this.agendamentosApi.cancelar(agendamento.id).subscribe({
      next: (agendamentoAtualizado) => {
        this.agendamentoProcessandoId = null;
        this.tipoAcaoProcessando = null;
        this.aplicarAgendamentoSalvo(agendamentoAtualizado);
        if (this.agendamentoEmEdicaoId === agendamentoAtualizado.id) {
          this.cancelarEdicao();
        }
        this.dashboardRefreshService.solicitarAtualizacao();
        this.toastService.success('Agendamento cancelado com sucesso.');
      },
      error: (error) => {
        const mensagem = this.httpErrorService.obterMensagem(error, 'Nao foi possivel cancelar o agendamento.');
        this.agendamentoProcessandoId = null;
        this.tipoAcaoProcessando = null;
        this.erro = mensagem;
        this.toastService.error(mensagem);
      }
    });
  }

  cancelarEdicao(): void {
    this.agendamentoEmEdicaoId = null;
    this.form.reset({
      clienteId: 0,
      servicoId: 0,
      veiculoId: null,
      data: '',
      horario: '',
      status: 'AGENDADO',
      observacoes: ''
    });
    this.veiculosDoCliente = [];
    this.form.markAsPristine();
    this.form.markAsUntouched();
  }

  campoInvalido(campo: AgendamentoFormField): boolean {
    const control = this.form.controls[campo];
    return control.invalid && (control.touched || control.dirty);
  }

  mensagemErro(campo: AgendamentoFormField): string {
    const control = this.form.controls[campo];
    if (control.hasError('required')) {
      return 'Campo obrigatorio.';
    }
    if (control.hasError('min')) {
      return 'Selecione uma opcao valida.';
    }
    if (control.hasError('maxlength')) {
      return 'Observacoes devem ter ate 500 caracteres.';
    }
    return '';
  }

  rotuloStatus(status: StatusAgendamento): string {
    return formatarStatusAgendamento(status);
  }

  itemProcessando(id: number): boolean {
    return this.agendamentoProcessandoId === id;
  }

  processandoStatus(id: number): boolean {
    return this.agendamentoProcessandoId === id && this.tipoAcaoProcessando === 'status';
  }

  processandoCancelamento(id: number): boolean {
    return this.agendamentoProcessandoId === id && this.tipoAcaoProcessando === 'cancelamento';
  }

  private criarPayload(): AgendamentoPayload {
    const value = this.form.getRawValue();
    return {
      clienteId: Number(value.clienteId),
      servicoId: Number(value.servicoId),
      veiculoId: value.veiculoId ? Number(value.veiculoId) : null,
      data: value.data,
      horario: value.horario,
      status: value.status,
      observacoes: this.normalizarTexto(value.observacoes)
    };
  }

  private formatarHorarioParaInput(value: string): string {
    return value.slice(0, 5);
  }

  private normalizarTexto(value: string): string | null {
    const normalized = value.trim();
    return normalized ? normalized : null;
  }

  private aplicarAgendamentoSalvo(agendamento: Agendamento): void {
    const indiceAtual = this.agendamentos.findIndex((item) => item.id === agendamento.id);
    if (indiceAtual >= 0) {
      this.agendamentos[indiceAtual] = agendamento;
    } else {
      this.agendamentos = [...this.agendamentos, agendamento];
    }

    this.agendamentos = this.ordenarAgendamentos(this.agendamentos);
  }

  private ordenarAgendamentos(agendamentos: Agendamento[]): Agendamento[] {
    return [...agendamentos].sort((a, b) => `${a.data}T${a.horario}`.localeCompare(`${b.data}T${b.horario}`));
  }

  private obterDataMinima(): string {
    const hoje = new Date();
    const local = new Date(hoje.getTime() - hoje.getTimezoneOffset() * 60000);
    return local.toISOString().slice(0, 10);
  }
}
