import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import {
  CategoriaFinanceira,
  CategoriaFinanceiraRequest,
  TIPO_CATEGORIA_LABEL,
  TIPO_CATEGORIA_OPTIONS,
  TipoCategoria
} from '../../../core/models/financeiro.model';
import { FinanceiroApiService } from '../../../core/services/financeiro-api.service';
import { HttpErrorService } from '../../../core/services/http-error.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-categorias-financeiras-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './categorias-page.component.html',
  styleUrl: './categorias-page.component.scss'
})
export class CategoriasPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(FinanceiroApiService);
  private readonly httpErrorService = inject(HttpErrorService);
  private readonly toastService = inject(ToastService);

  readonly tipoLabel = TIPO_CATEGORIA_LABEL;
  readonly tipoOptions = TIPO_CATEGORIA_OPTIONS;

  readonly form = this.fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(80)]],
    tipo: ['RECEITA' as TipoCategoria, Validators.required],
    cor: ['#18E4D3']
  });

  categorias: CategoriaFinanceira[] = [];
  filtroTipo: TipoCategoria | null = null;

  carregando = false;
  salvando = false;
  emEdicaoId: number | null = null;
  processandoId: number | null = null;
  erro = '';

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.carregando = true;
    this.erro = '';
    this.api.listarCategorias().subscribe({
      next: (categorias) => {
        this.categorias = categorias;
        this.carregando = false;
      },
      error: (error) => {
        this.erro = this.httpErrorService.obterMensagem(error, 'Nao foi possivel carregar categorias.');
        this.carregando = false;
      }
    });
  }

  filtrarPorTipo(tipo: TipoCategoria | null): void {
    this.filtroTipo = tipo;
  }

  categoriasFiltradas(): CategoriaFinanceira[] {
    if (!this.filtroTipo) {
      return this.categorias;
    }
    return this.categorias.filter((c) => c.tipo === this.filtroTipo || c.tipo === 'AMBOS');
  }

  salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.salvando = true;
    this.erro = '';

    const payload: CategoriaFinanceiraRequest = {
      nome: this.form.controls.nome.value.trim(),
      tipo: this.form.controls.tipo.value,
      cor: this.form.controls.cor.value || null
    };

    const editando = this.emEdicaoId !== null;
    const request$ = editando
      ? this.api.atualizarCategoria(this.emEdicaoId!, payload)
      : this.api.criarCategoria(payload);

    request$.subscribe({
      next: (categoria) => {
        this.salvando = false;
        this.aplicarSalvo(categoria);
        this.toastService.success(editando ? 'Categoria atualizada.' : 'Categoria criada.');
        this.cancelarEdicao();
      },
      error: (error) => {
        this.salvando = false;
        const m = this.httpErrorService.obterMensagem(error, 'Nao foi possivel salvar a categoria.');
        this.erro = m;
        this.toastService.error(m);
      }
    });
  }

  editar(c: CategoriaFinanceira): void {
    this.emEdicaoId = c.id;
    this.form.patchValue({
      nome: c.nome,
      tipo: c.tipo,
      cor: c.cor ?? '#18E4D3'
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  excluir(c: CategoriaFinanceira): void {
    if (!window.confirm(`Inativar a categoria "${c.nome}"?`)) {
      return;
    }
    this.processandoId = c.id;
    this.api.excluirCategoria(c.id).subscribe({
      next: () => {
        this.processandoId = null;
        this.categorias = this.categorias.filter((x) => x.id !== c.id);
        this.toastService.success('Categoria inativada.');
      },
      error: (error) => {
        this.processandoId = null;
        this.toastService.error(this.httpErrorService.obterMensagem(error, 'Nao foi possivel inativar.'));
      }
    });
  }

  cancelarEdicao(): void {
    this.emEdicaoId = null;
    this.form.reset({ nome: '', tipo: 'RECEITA', cor: '#18E4D3' });
  }

  private aplicarSalvo(c: CategoriaFinanceira): void {
    const idx = this.categorias.findIndex((x) => x.id === c.id);
    if (idx >= 0) {
      this.categorias[idx] = c;
    } else {
      this.categorias = [c, ...this.categorias];
    }
    this.categorias = [...this.categorias].sort((a, b) => a.nome.localeCompare(b.nome));
  }
}
