import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { DashboardResumo } from '../models/dashboard.model';

@Injectable({ providedIn: 'root' })
export class DashboardApiService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${environment.apiUrl}/dashboard`;

  obterResumo(): Observable<DashboardResumo> {
    return this.http.get<DashboardResumo>(this.endpoint);
  }
}
