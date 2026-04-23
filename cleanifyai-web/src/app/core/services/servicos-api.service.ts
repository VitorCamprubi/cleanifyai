import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Servico, ServicoPayload } from '../models/servico.model';

@Injectable({ providedIn: 'root' })
export class ServicosApiService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${environment.apiUrl}/servicos`;

  listar(): Observable<Servico[]> {
    return this.http.get<Servico[]>(this.endpoint);
  }

  criar(payload: ServicoPayload): Observable<Servico> {
    return this.http.post<Servico>(this.endpoint, payload);
  }

  atualizar(id: number, payload: ServicoPayload): Observable<Servico> {
    return this.http.put<Servico>(`${this.endpoint}/${id}`, payload);
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }
}
