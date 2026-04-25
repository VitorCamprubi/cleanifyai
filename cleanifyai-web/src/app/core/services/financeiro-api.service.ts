import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Lancamento, LancamentoRequest, ResumoFinanceiro } from '../models/financeiro.model';

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
