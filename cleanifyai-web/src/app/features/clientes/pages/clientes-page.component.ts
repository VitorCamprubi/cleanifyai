import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { Cliente, ClientePayload } from '../../../core/models/cliente.model';
import { Veiculo } from '../../../core/models/veiculo.model';
import { ClientesApiService } from '../../../core/services/clientes-api.service';
import { DashboardRefreshService } from '../../../core/services/dashboard-refresh.service';
import { HttpErrorService } from '../../../core/services/http-error.service';
import { ToastService } from '../../../core/services/toast.service';
import { VeiculosApiService } from '../../../core/services/veiculos-api.service';
import { formatarTelefoneBr } from '../../../core/utils/formatters';

type ClienteFormField = 'nome' | 'telefone' | 'email' | 'observacoes';

@Component({
  selector: 'app-clientes-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './clientes-page.component.html',
  styleUrl: './clientes-page.component.scss'
})
export class ClientesPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly clientesApi = inject(ClientesApiService);
  private readonly veiculosApi = inject(VeiculosApiService);
  private readonly httpErrorService = inject(HttpErrorService);
  private readonly toastService = inject(ToastService);
  private readonly dashboardRefreshService = inject(DashboardRefreshService);

  readonly form = this.fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(120)]],
    telefone: ['', [Validators.required, Validators.maxLength(20)]],
    email: ['', [Validators.email, Validators.maxLength(120)]],
    observacoes: ['', Validators.maxLength(500)]
  });

  clientes: Cliente[] = [];
  veiculos: Veiculo[] = [];
  carregando = false;
  salvando = false;
  erro = '';
  clienteEmEdicaoId: number | null = null;
  clienteProcessandoId: number | null = null;

  ngOnInit(): void {
    this.carregarClientes();
  }

  carregarClientes(exibirToastErro = false): void {
    this.carregando = true;
    this.erro = '';

    forkJoin({
      clientes: this.clientesApi.listar(),
      veiculos: this.veiculosApi.listar()
    }).subscribe({
      next: ({ clientes, veiculos }) => {
        this.clientes = this.ordenarClientes(clientes);
        this.veiculos = veiculos;
        this.carregando = false;
      },
      error: (error) => {
        const mensagem = this.httpErrorService.obterMensagem(error, 'Nao foi possivel carregar os clientes.');
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
    const atualizando = this.clienteEmEdicaoId !== null;
    const request$ = atualizando
      ? this.clientesApi.atualizar(this.clienteEmEdicaoId!, payload)
      : this.clientesApi.criar(payload);

    request$.subscribe({
      next: (cliente) => {
        this.salvando = false;
        this.aplicarClienteSalvo(cliente);
        this.dashboardRefreshService.solicitarAtualizacao();
        this.toastService.success(atualizando ? 'Cliente atualizado com sucesso.' : 'Cliente criado com sucesso.');
        this.cancelarEdicao();
      },
      error: (error) => {
        const mensagem = this.httpErrorService.obterMensagem(error, 'Nao foi possivel salvar o cliente.');
        this.salvando = false;
        this.erro = mensagem;
        this.toastService.error(mensagem);
      }
    });
  }

  editar(cliente: Cliente): void {
    this.clienteEmEdicaoId = cliente.id;
    this.erro = '';
    this.form.patchValue({
      nome: cliente.nome,
      telefone: formatarTelefoneBr(cliente.telefone),
      email: cliente.email ?? '',
      observacoes: cliente.observacoes ?? ''
    });
  }

  excluir(cliente: Cliente): void {
    if (!window.confirm(`Excluir o cliente ${cliente.nome}?`)) {
      return;
    }

    this.clienteProcessandoId = cliente.id;
    this.erro = '';

    this.clientesApi.excluir(cliente.id).subscribe({
      next: () => {
        this.clienteProcessandoId = null;
        this.clientes = this.clientes.filter((item) => item.id !== cliente.id);
        if (this.clienteEmEdicaoId === cliente.id) {
          this.cancelarEdicao();
        }
        this.dashboardRefreshService.solicitarAtualizacao();
        this.toastService.success('Cliente excluido com sucesso.');
      },
      error: (error) => {
        const mensagem = this.httpErrorService.obterMensagem(error, 'Nao foi possivel excluir o cliente.');
        this.clienteProcessandoId = null;
        this.erro = mensagem;
        this.toastService.error(mensagem);
      }
    });
  }

  cancelarEdicao(): void {
    this.clienteEmEdicaoId = null;
    this.form.reset({
      nome: '',
      telefone: '',
      email: '',
      observacoes: ''
    });
    this.form.markAsPristine();
    this.form.markAsUntouched();
  }

  campoInvalido(campo: ClienteFormField): boolean {
    const control = this.form.controls[campo];
    return control.invalid && (control.touched || control.dirty);
  }

  mensagemErro(campo: ClienteFormField): string {
    const control = this.form.controls[campo];
    if (control.hasError('required')) {
      return campo === 'telefone' ? 'Telefone e obrigatorio.' : 'Campo obrigatorio.';
    }
    if (control.hasError('email')) {
      return 'Informe um e-mail valido.';
    }
    if (control.hasError('maxlength')) {
      return 'Valor acima do limite permitido.';
    }
    return '';
  }

  formatarTelefone(telefone: string | null): string {
    return formatarTelefoneBr(telefone);
  }

  itemProcessando(id: number): boolean {
    return this.clienteProcessandoId === id;
  }

  veiculosDoCliente(clienteId: number): Veiculo[] {
    return this.veiculos.filter((veiculo) => veiculo.clienteId === clienteId);
  }

  resumoVeiculos(clienteId: number): string {
    const veiculos = this.veiculosDoCliente(clienteId);
    if (!veiculos.length) {
      return 'Nenhum veiculo vinculado';
    }
    return veiculos
      .slice(0, 2)
      .map((veiculo) => `${veiculo.marca} ${veiculo.modelo}${veiculo.placa ? ` - ${veiculo.placa}` : ''}`)
      .join(' | ');
  }

  private criarPayload(): ClientePayload {
    const value = this.form.getRawValue();
    return {
      nome: value.nome.trim(),
      telefone: value.telefone.trim(),
      email: this.normalizarTexto(value.email),
      observacoes: this.normalizarTexto(value.observacoes)
    };
  }

  private normalizarTexto(value: string): string | null {
    const normalized = value.trim();
    return normalized ? normalized : null;
  }

  private aplicarClienteSalvo(cliente: Cliente): void {
    const indiceAtual = this.clientes.findIndex((item) => item.id === cliente.id);
    if (indiceAtual >= 0) {
      this.clientes[indiceAtual] = cliente;
    } else {
      this.clientes = [...this.clientes, cliente];
    }

    this.clientes = this.ordenarClientes(this.clientes);
  }

  private ordenarClientes(clientes: Cliente[]): Cliente[] {
    return [...clientes].sort((a, b) => a.nome.localeCompare(b.nome));
  }
}
