import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Cliente, ClientePayload } from '../models/cliente.model';

@Injectable({ providedIn: 'root' })
export class ClientesApiService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${environment.apiUrl}/clientes`;

  listar(): Observable<Cliente[]> {
    return this.http.get<Cliente[]>(this.endpoint);
  }

  criar(payload: ClientePayload): Observable<Cliente> {
    return this.http.post<Cliente>(this.endpoint, payload);
  }

  atualizar(id: number, payload: ClientePayload): Observable<Cliente> {
    return this.http.put<Cliente>(`${this.endpoint}/${id}`, payload);
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }
}
