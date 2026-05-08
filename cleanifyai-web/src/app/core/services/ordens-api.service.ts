import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { OrdemServico, OrdemServicoRequest, StatusOrdem } from '../models/ordem.model';

@Injectable({ providedIn: 'root' })
export class OrdensApiService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${environment.apiUrl}/ordens`;

  listar(status?: StatusOrdem): Observable<OrdemServico[]> {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<OrdemServico[]>(this.endpoint, { params });
  }

  buscar(id: number): Observable<OrdemServico> {
    return this.http.get<OrdemServico>(`${this.endpoint}/${id}`);
  }

  criar(payload: OrdemServicoRequest): Observable<OrdemServico> {
    return this.http.post<OrdemServico>(this.endpoint, payload);
  }

  criarAPartirDeAgendamento(agendamentoId: number): Observable<OrdemServico> {
    return this.http.post<OrdemServico>(`${this.endpoint}/from-agendamento/${agendamentoId}`, {});
  }

  atualizar(id: number, payload: OrdemServicoRequest): Observable<OrdemServico> {
    return this.http.put<OrdemServico>(`${this.endpoint}/${id}`, payload);
  }

  atualizarStatus(id: number, status: StatusOrdem): Observable<OrdemServico> {
    return this.http.patch<OrdemServico>(`${this.endpoint}/${id}/status`, { status });
  }

  cancelar(id: number): Observable<OrdemServico> {
    return this.http.patch<OrdemServico>(`${this.endpoint}/${id}/cancelar`, {});
  }
}
