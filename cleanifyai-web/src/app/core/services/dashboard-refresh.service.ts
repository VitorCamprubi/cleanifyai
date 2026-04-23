import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class DashboardRefreshService {
  private readonly refreshSubject = new Subject<void>();

  readonly refresh$ = this.refreshSubject.asObservable();

  solicitarAtualizacao(): void {
    this.refreshSubject.next();
  }
}
