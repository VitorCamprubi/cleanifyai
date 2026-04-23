import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { Servico, ServicoPayload } from '../../../core/models/servico.model';
import { AuthService } from '../../../core/services/auth.service';
import { DashboardRefreshService } from '../../../core/services/dashboard-refresh.service';
import { HttpErrorService } from '../../../core/services/http-error.service';
import { ServicosApiService } from '../../../core/services/servicos-api.service';
import { ToastService } from '../../../core/services/toast.service';

type ServicoFormField = 'nome' | 'descricao' | 'preco' | 'duracaoMinutos' | 'ativo';

@Component({
  selector: 'app-servicos-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './servicos-page.component.html',
  styleUrl: './servicos-page.component.scss'
})
export class ServicosPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly servicosApi = inject(ServicosApiService);
  private readonly authService = inject(AuthService);
  private readonly httpErrorService = inject(HttpErrorService);
  private readonly toastService = inject(ToastService);
  private readonly dashboardRefreshService = inject(DashboardRefreshService);

  readonly form = this.fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(120)]],
    descricao: ['', Validators.maxLength(500)],
    preco: [0, [Validators.required, Validators.min(0.01)]],
    duracaoMinutos: [60, [Validators.required, Validators.min(1)]],
    ativo: [true, Validators.required]
  });

  servicos: Servico[] = [];
  carregando = false;
  salvando = false;
  erro = '';
  servicoEmEdicaoId: number | null = null;
  servicoProcessandoId: number | null = null;

  ngOnInit(): void {
    this.carregarServicos();
  }

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  carregarServicos(exibirToastErro = false): void {
    this.carregando = true;
    this.erro = '';

    this.servicosApi.listar().subscribe({
      next: (servicos) => {
        this.servicos = this.ordenarServicos(servicos);
        this.carregando = false;
      },
      error: (error) => {
        const mensagem = this.httpErrorService.obterMensagem(error, 'Nao foi possivel carregar os servicos.');
        this.erro = mensagem;
        this.carregando = false;
        if (exibirToastErro) {
          this.toastService.error(mensagem);
        }
      }
    });
  }

  salvar(): void {
    if (!this.isAdmin()) {
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.salvando = true;
    this.erro = '';

    const payload = this.criarPayload();
    const atualizando = this.servicoEmEdicaoId !== null;
    const request$ = atualizando
      ? this.servicosApi.atualizar(this.servicoEmEdicaoId!, payload)
      : this.servicosApi.criar(payload);

    request$.subscribe({
      next: (servico) => {
        this.salvando = false;
        this.aplicarServicoSalvo(servico);
        this.dashboardRefreshService.solicitarAtualizacao();
        this.toastService.success(atualizando ? 'Servico atualizado com sucesso.' : 'Servico criado com sucesso.');
        this.cancelarEdicao();
      },
      error: (error) => {
        const mensagem = this.httpErrorService.obterMensagem(error, 'Nao foi possivel salvar o servico.');
        this.salvando = false;
        this.erro = mensagem;
        this.toastService.error(mensagem);
      }
    });
  }

  editar(servico: Servico): void {
    if (!this.isAdmin()) {
      return;
    }

    this.servicoEmEdicaoId = servico.id;
    this.erro = '';
    this.form.patchValue({
      nome: servico.nome,
      descricao: servico.descricao ?? '',
      preco: servico.preco,
      duracaoMinutos: servico.duracaoMinutos,
      ativo: servico.ativo
    });
  }

  excluir(servico: Servico): void {
    if (!this.isAdmin()) {
      return;
    }

    if (!window.confirm(`Excluir o servico ${servico.nome}?`)) {
      return;
    }

    this.servicoProcessandoId = servico.id;
    this.erro = '';

    this.servicosApi.excluir(servico.id).subscribe({
      next: () => {
        this.servicoProcessandoId = null;
        this.servicos = this.servicos.filter((item) => item.id !== servico.id);
        if (this.servicoEmEdicaoId === servico.id) {
          this.cancelarEdicao();
        }
        this.dashboardRefreshService.solicitarAtualizacao();
        this.toastService.success('Servico excluido com sucesso.');
      },
      error: (error) => {
        const mensagem = this.httpErrorService.obterMensagem(error, 'Nao foi possivel excluir o servico.');
        this.servicoProcessandoId = null;
        this.erro = mensagem;
        this.toastService.error(mensagem);
      }
    });
  }

  cancelarEdicao(): void {
    this.servicoEmEdicaoId = null;
    this.form.reset({
      nome: '',
      descricao: '',
      preco: 0,
      duracaoMinutos: 60,
      ativo: true
    });
    this.form.markAsPristine();
    this.form.markAsUntouched();
  }

  campoInvalido(campo: ServicoFormField): boolean {
    const control = this.form.controls[campo];
    return control.invalid && (control.touched || control.dirty);
  }

  mensagemErro(campo: ServicoFormField): string {
    const control = this.form.controls[campo];
    if (control.hasError('required')) {
      return 'Campo obrigatorio.';
    }
    if (control.hasError('maxlength')) {
      return 'Valor acima do limite permitido.';
    }
    if (control.hasError('min')) {
      return campo === 'duracaoMinutos' ? 'Duracao deve ser maior que zero.' : 'Preco deve ser maior que zero.';
    }
    return '';
  }

  itemProcessando(id: number): boolean {
    return this.servicoProcessandoId === id;
  }

  private criarPayload(): ServicoPayload {
    const value = this.form.getRawValue();
    return {
      nome: value.nome.trim(),
      descricao: this.normalizarTexto(value.descricao),
      preco: Number(value.preco),
      duracaoMinutos: Number(value.duracaoMinutos),
      ativo: value.ativo
    };
  }

  private normalizarTexto(value: string): string | null {
    const normalized = value.trim();
    return normalized ? normalized : null;
  }

  private aplicarServicoSalvo(servico: Servico): void {
    const indiceAtual = this.servicos.findIndex((item) => item.id === servico.id);
    if (indiceAtual >= 0) {
      this.servicos[indiceAtual] = servico;
    } else {
      this.servicos = [...this.servicos, servico];
    }

    this.servicos = this.ordenarServicos(this.servicos);
  }

  private ordenarServicos(servicos: Servico[]): Servico[] {
    return [...servicos].sort((a, b) => a.nome.localeCompare(b.nome));
  }
}
