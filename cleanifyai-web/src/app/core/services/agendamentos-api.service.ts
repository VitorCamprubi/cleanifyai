import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Agendamento, AgendamentoPayload, StatusAgendamento } from '../models/agendamento.model';

@Injectable({ providedIn: 'root' })
export class AgendamentosApiService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${environment.apiUrl}/agendamentos`;

  listar(): Observable<Agendamento[]> {
    return this.http.get<Agendamento[]>(this.endpoint);
  }

  criar(payload: AgendamentoPayload): Observable<Agendamento> {
    return this.http.post<Agendamento>(this.endpoint, payload);
  }

  atualizar(id: number, payload: AgendamentoPayload): Observable<Agendamento> {
    return this.http.put<Agendamento>(`${this.endpoint}/${id}`, payload);
  }

  atualizarStatus(id: number, status: StatusAgendamento): Observable<Agendamento> {
    return this.http.patch<Agendamento>(`${this.endpoint}/${id}/status`, { status });
  }

  cancelar(id: number): Observable<Agendamento> {
    return this.http.patch<Agendamento>(`${this.endpoint}/${id}/cancelar`, {});
  }
}
