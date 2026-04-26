import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Veiculo, VeiculoRequest } from '../models/veiculo.model';

@Injectable({ providedIn: 'root' })
export class VeiculosApiService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${environment.apiUrl}/veiculos`;

  listar(clienteId?: number): Observable<Veiculo[]> {
    let params = new HttpParams();
    if (clienteId) {
      params = params.set('clienteId', String(clienteId));
    }
    return this.http.get<Veiculo[]>(this.endpoint, { params });
  }

  buscar(id: number): Observable<Veiculo> {
    return this.http.get<Veiculo>(`${this.endpoint}/${id}`);
  }

  criar(payload: VeiculoRequest): Observable<Veiculo> {
    return this.http.post<Veiculo>(this.endpoint, payload);
  }

  atualizar(id: number, payload: VeiculoRequest): Observable<Veiculo> {
    return this.http.put<Veiculo>(`${this.endpoint}/${id}`, payload);
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }
}
