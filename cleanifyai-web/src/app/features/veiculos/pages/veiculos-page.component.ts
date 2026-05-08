import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { forkJoin } from 'rxjs';

import { Cliente } from '../../../core/models/cliente.model';
import { Veiculo, VeiculoRequest } from '../../../core/models/veiculo.model';
import { ClientesApiService } from '../../../core/services/clientes-api.service';
import { HttpErrorService } from '../../../core/services/http-error.service';
import { ToastService } from '../../../core/services/toast.service';
import { VeiculosApiService } from '../../../core/services/veiculos-api.service';

@Component({
  selector: 'app-veiculos-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './veiculos-page.component.html',
  styleUrl: './veiculos-page.component.scss'
})
export class VeiculosPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(VeiculosApiService);
  private readonly clientesApi = inject(ClientesApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly httpErrorService = inject(HttpErrorService);
  private readonly toastService = inject(ToastService);

  readonly form = this.fb.nonNullable.group({
    clienteId: [0, [Validators.required, Validators.min(1)]],
    marca: ['', [Validators.required, Validators.maxLength(60)]],
    modelo: ['', [Validators.required, Validators.maxLength(80)]],
    placa: ['', Validators.maxLength(10)],
    cor: ['', Validators.maxLength(30)],
    anoModelo: [null as number | null],
    observacoes: ['', Validators.maxLength(500)]
  });

  clientes: Cliente[] = [];
  veiculos: Veiculo[] = [];
  filtroClienteId: number | null = null;

  carregando = false;
  salvando = false;
  veiculoEmEdicaoId: number | null = null;
  veiculoProcessandoId: number | null = null;
  erro = '';

  ngOnInit(): void {
    const clienteIdInicial = Number(this.route.snapshot.queryParamMap.get('clienteId'));
    if (clienteIdInicial > 0) {
      this.filtroClienteId = clienteIdInicial;
      this.form.patchValue({ clienteId: clienteIdInicial });
    }
    this.carregarDadosIniciais();
  }

  carregarDadosIniciais(): void {
    this.carregando = true;
    this.erro = '';

    forkJoin({
      clientes: this.clientesApi.listar(),
      veiculos: this.api.listar(this.filtroClienteId ?? undefined)
    }).subscribe({
      next: ({ clientes, veiculos }) => {
        this.clientes = [...clientes].sort((a, b) => a.nome.localeCompare(b.nome));
        this.veiculos = veiculos;
        this.carregando = false;
      },
      error: (error) => {
        this.erro = this.httpErrorService.obterMensagem(error, 'Nao foi possivel carregar os veiculos.');
        this.carregando = false;
      }
    });
  }

  filtrarPorCliente(clienteId: number | null): void {
    this.filtroClienteId = clienteId && clienteId > 0 ? clienteId : null;
    this.recarregar();
  }

  recarregar(): void {
    this.carregando = true;
    this.api.listar(this.filtroClienteId ?? undefined).subscribe({
      next: (veiculos) => {
        this.veiculos = veiculos;
        this.carregando = false;
      },
      error: (error) => {
        this.erro = this.httpErrorService.obterMensagem(error, 'Nao foi possivel atualizar a lista.');
        this.carregando = false;
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

    const payload = this.montarPayload();
    const editando = this.veiculoEmEdicaoId !== null;
    const request$ = editando
      ? this.api.atualizar(this.veiculoEmEdicaoId!, payload)
      : this.api.criar(payload);

    request$.subscribe({
      next: (veiculo) => {
        this.salvando = false;
        this.aplicarVeiculoSalvo(veiculo);
        this.toastService.success(editando ? 'Veiculo atualizado.' : 'Veiculo cadastrado.');
        this.cancelarEdicao();
      },
      error: (error) => {
        this.salvando = false;
        const mensagem = this.httpErrorService.obterMensagem(error, 'Nao foi possivel salvar o veiculo.');
        this.erro = mensagem;
        this.toastService.error(mensagem);
      }
    });
  }

  editar(veiculo: Veiculo): void {
    this.veiculoEmEdicaoId = veiculo.id;
    this.form.patchValue({
      clienteId: veiculo.clienteId,
      marca: veiculo.marca,
      modelo: veiculo.modelo,
      placa: veiculo.placa ?? '',
      cor: veiculo.cor ?? '',
      anoModelo: veiculo.anoModelo ?? null,
      observacoes: veiculo.observacoes ?? ''
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  excluir(veiculo: Veiculo): void {
    if (!window.confirm(`Inativar o veiculo ${veiculo.marca} ${veiculo.modelo} (${veiculo.placa ?? 'sem placa'})?`)) {
      return;
    }
    this.veiculoProcessandoId = veiculo.id;
    this.api.excluir(veiculo.id).subscribe({
      next: () => {
        this.veiculoProcessandoId = null;
        this.veiculos = this.veiculos.filter((v) => v.id !== veiculo.id);
        this.toastService.success('Veiculo inativado.');
      },
      error: (error) => {
        this.veiculoProcessandoId = null;
        this.toastService.error(this.httpErrorService.obterMensagem(error, 'Nao foi possivel inativar o veiculo.'));
      }
    });
  }

  cancelarEdicao(): void {
    this.veiculoEmEdicaoId = null;
    this.form.reset({
      clienteId: this.filtroClienteId ?? 0,
      marca: '',
      modelo: '',
      placa: '',
      cor: '',
      anoModelo: null,
      observacoes: ''
    });
  }

  campoInvalido(campo: 'clienteId' | 'marca' | 'modelo'): boolean {
    const control = this.form.controls[campo];
    return control.invalid && (control.touched || control.dirty);
  }

  private montarPayload(): VeiculoRequest {
    const value = this.form.getRawValue();
    return {
      clienteId: Number(value.clienteId),
      marca: value.marca.trim(),
      modelo: value.modelo.trim(),
      placa: this.normalizar(value.placa),
      cor: this.normalizar(value.cor),
      anoModelo: value.anoModelo ? Number(value.anoModelo) : null,
      observacoes: this.normalizar(value.observacoes)
    };
  }

  private normalizar(valor: string | null | undefined): string | null {
    if (!valor) {
      return null;
    }
    const n = valor.trim();
    return n ? n : null;
  }

  private aplicarVeiculoSalvo(veiculo: Veiculo): void {
    const idx = this.veiculos.findIndex((v) => v.id === veiculo.id);
    if (idx >= 0) {
      this.veiculos[idx] = veiculo;
    } else {
      this.veiculos = [veiculo, ...this.veiculos];
    }
    this.veiculos = [...this.veiculos];
  }
}
