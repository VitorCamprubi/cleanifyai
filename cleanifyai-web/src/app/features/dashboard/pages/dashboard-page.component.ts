import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';

import { StatusAgendamento } from '../../../core/models/agendamento.model';
import { DashboardResumo } from '../../../core/models/dashboard.model';
import { DashboardApiService } from '../../../core/services/dashboard-api.service';
import { DashboardRefreshService } from '../../../core/services/dashboard-refresh.service';
import { HttpErrorService } from '../../../core/services/http-error.service';
import { ToastService } from '../../../core/services/toast.service';
import { formatarStatusAgendamento } from '../../../core/utils/formatters';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.scss'
})
export class DashboardPageComponent implements OnInit {
  private readonly dashboardApi = inject(DashboardApiService);
  private readonly dashboardRefreshService = inject(DashboardRefreshService);
  private readonly httpErrorService = inject(HttpErrorService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  resumo: DashboardResumo | null = null;
  carregando = false;
  erro = '';

  ngOnInit(): void {
    this.carregarResumo();

    interval(60000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.carregarResumo(false));

    this.dashboardRefreshService.refresh$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.carregarResumo(false));
  }

  carregarResumo(exibirToastErro = true): void {
    if (this.carregando) {
      return;
    }

    this.carregando = true;
    this.erro = '';

    this.dashboardApi.obterResumo().subscribe({
      next: (resumo) => {
        this.resumo = resumo;
        this.carregando = false;
      },
      error: (error) => {
        const mensagem = this.httpErrorService.obterMensagem(error, 'Nao foi possivel carregar o dashboard.');
        this.erro = mensagem;
        this.carregando = false;
        if (exibirToastErro) {
          this.toastService.error(mensagem);
        }
      }
    });
  }

  rotuloStatus(status: StatusAgendamento): string {
    return formatarStatusAgendamento(status);
  }
}
