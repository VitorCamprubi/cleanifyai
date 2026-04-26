import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CategoriaFinanceira,
  CategoriaFinanceiraRequest,
  Lancamento,
  LancamentoRequest,
  ResumoFinanceiro,
  TipoCategoria
} from '../models/financeiro.model';

@Injectable({ providedIn: 'root' })
export class FinanceiroApiService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${environment.apiUrl}/financeiro`;

  listar(inicio?: string, fim?: string): Observable<Lancamento[]> {
    return this.http.get<Lancamento[]>(`${this.endpoint}/lancamentos`, { params: this.buildParams(inicio, fim) });
  }

  resumo(inicio?: string, fim?: string): Observable<ResumoFinanceiro> {
    return this.http.get<ResumoFinanceiro>(`${this.endpoint}/resumo`, { params: this.buildParams(inicio, fim) });
  }

  registrar(payload: LancamentoRequest): Observable<Lancamento> {
    return this.http.post<Lancamento>(`${this.endpoint}/lancamentos`, payload);
  }

  estornar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/lancamentos/${id}`);
  }

  listarCategorias(tipo?: TipoCategoria): Observable<CategoriaFinanceira[]> {
    let params = new HttpParams();
    if (tipo) {
      params = params.set('tipo', tipo);
    }
    return this.http.get<CategoriaFinanceira[]>(`${this.endpoint}/categorias`, { params });
  }

  criarCategoria(payload: CategoriaFinanceiraRequest): Observable<CategoriaFinanceira> {
    return this.http.post<CategoriaFinanceira>(`${this.endpoint}/categorias`, payload);
  }

  atualizarCategoria(id: number, payload: CategoriaFinanceiraRequest): Observable<CategoriaFinanceira> {
    return this.http.put<CategoriaFinanceira>(`${this.endpoint}/categorias/${id}`, payload);
  }

  excluirCategoria(id: number): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/categorias/${id}`);
  }

  private buildParams(inicio?: string, fim?: string): HttpParams {
    let params = new HttpParams();
    if (inicio) {
      params = params.set('inicio', inicio);
    }
    if (fim) {
      params = params.set('fim', fim);
    }
    return params;
  }
}
