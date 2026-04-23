import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';

export interface PingResponse {
  status: string;
  service: string;
}

@Injectable({ providedIn: 'root' })
export class PingApiService {
  private readonly http = inject(HttpClient);

  ping(): Observable<PingResponse> {
    return this.http.get<PingResponse>(`${environment.apiUrl}/ping`);
  }
}
