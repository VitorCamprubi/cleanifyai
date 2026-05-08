import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, interval, of } from 'rxjs';

import { DashboardResumo } from '../../../core/models/dashboard.model';
import { Lancamento, ResumoFinanceiro } from '../../../core/models/financeiro.model';
import { OrdemServico, StatusOrdem } from '../../../core/models/ordem.model';
import { DashboardApiService } from '../../../core/services/dashboard-api.service';
import { DashboardRefreshService } from '../../../core/services/dashboard-refresh.service';
import { FinanceiroApiService } from '../../../core/services/financeiro-api.service';
import { HttpErrorService } from '../../../core/services/http-error.service';
import { OrdensApiService } from '../../../core/services/ordens-api.service';

type MetricIcon = 'orders' | 'calendar' | 'revenue' | 'services';

interface DashboardMetric {
  label: string;
  value: string;
  hint: string;
  icon: MetricIcon;
  tone?: 'violet' | 'mint';
  route: string;
}

interface DashboardOrderRow {
  id: number;
  title: string;
  subtitle: string;
  status: string;
  statusClass: string;
  time: string;
}

interface ChartDot {
  x: number;
  y: number;
}

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.scss'
})
export class DashboardPageComponent implements OnInit {
  private readonly dashboardApi = inject(DashboardApiService);
  private readonly dashboardRefreshService = inject(DashboardRefreshService);
  private readonly financeiroApi = inject(FinanceiroApiService);
  private readonly ordensApi = inject(OrdensApiService);
  private readonly httpErrorService = inject(HttpErrorService);
  private readonly destroyRef = inject(DestroyRef);

  resumo: DashboardResumo | null = null;
  financeiroMes: ResumoFinanceiro | null = null;
  lancamentosGrafico: Lancamento[] = [];
  ordens: OrdemServico[] = [];
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

  get metricas(): DashboardMetric[] {
    const ordensAbertas = this.ordens.filter((ordem) => ordem.status === 'ABERTA').length;
    const servicosEmAndamento = this.ordens.filter((ordem) => ordem.status === 'EM_EXECUCAO').length;
    const agendamentosHoje = this.resumo?.totalAgendamentosDoDia ?? 0;
    const faturamentoMes = Number(this.financeiroMes?.totalEntradas ?? 0);

    return [
      {
        label: 'Ordens em aberto',
        value: this.formatarNumero(ordensAbertas),
        hint: servicosEmAndamento ? `${servicosEmAndamento} em execucao` : 'sem execucao ativa',
        icon: 'orders',
        route: '/ordens'
      },
      {
        label: 'Agendamentos hoje',
        value: this.formatarNumero(agendamentosHoje).padStart(2, '0'),
        hint: agendamentosHoje === 1 ? '1 horario reservado' : `${agendamentosHoje} horarios reservados`,
        icon: 'calendar',
        tone: 'violet',
        route: '/agendamentos'
      },
      {
        label: 'Faturamento do mes',
        value: this.formatarMoeda(faturamentoMes),
        hint: `${this.financeiroMes?.quantidadeLancamentos ?? 0} lancamentos`,
        icon: 'revenue',
        tone: 'mint',
        route: '/financeiro'
      },
      {
        label: 'OS em execucao',
        value: this.formatarNumero(servicosEmAndamento).padStart(2, '0'),
        hint: servicosEmAndamento ? 'ativo' : 'sem OS ativa',
        icon: 'services',
        tone: 'mint',
        route: '/ordens'
      }
    ];
  }

  get faturamentoTotal(): string {
    return this.formatarMoeda(this.totalEntradasUltimos30Dias);
  }

  get chartLabels(): string[] {
    return this.datasGrafico
      .filter((_, index) => index % 4 === 0 || index === this.datasGrafico.length - 1)
      .map((data) => this.formatarLabelGrafico(data));
  }

  get chartAxisY(): string[] {
    const max = this.maximoGrafico;
    const steps = [1, 0.8, 0.6, 0.4, 0.2];
    return steps.map((step) => this.formatarCompacto(max * step));
  }

  get chartLinePoints(): string {
    return this.chartDots.map((dot) => `${dot.x},${dot.y}`).join(' ');
  }

  get chartAreaPoints(): string {
    return `${this.chartLinePoints} 760,150 0,150`;
  }

  get chartDots(): ChartDot[] {
    const valores = this.valoresGrafico;
    const max = this.maximoGrafico;
    const count = Math.max(valores.length - 1, 1);

    return valores.map((valor, index) => ({
      x: Number(((760 / count) * index).toFixed(2)),
      y: Number((132 - (valor / max) * 118).toFixed(2))
    }));
  }

  get temDadosGrafico(): boolean {
    return this.totalEntradasUltimos30Dias > 0;
  }

  get ordensRecentes(): DashboardOrderRow[] {
    return [...this.ordens]
      .sort((a, b) => b.abertaEm.localeCompare(a.abertaEm))
      .slice(0, 3)
      .map((ordem) => ({
        id: ordem.id,
        title: this.formatarTituloOrdem(ordem),
        subtitle: ordem.itens[0]?.servicoNome ?? 'Servico automotivo',
        status: this.rotuloStatusOrdem(ordem.status),
        statusClass: this.classeStatusOrdem(ordem.status),
        time: this.formatarHorarioOrdem(ordem.abertaEm)
      }));
  }

  carregarResumo(exibirErro = true): void {
    if (this.carregando) {
      return;
    }

    this.carregando = true;
    this.erro = '';

    const hoje = this.formatarData(new Date());
    const inicioMes = this.formatarData(new Date(new Date().getFullYear(), new Date().getMonth(), 1));
    const inicioGrafico = this.formatarData(this.adicionarDias(new Date(), -29));

    forkJoin({
      resumo: this.dashboardApi.obterResumo(),
      financeiroMes: this.financeiroApi.resumo(inicioMes, hoje),
      lancamentosGrafico: this.financeiroApi.listar(inicioGrafico, hoje),
      ordens: this.ordensApi.listar()
    })
      .pipe(
        catchError((error) => {
          if (exibirErro) {
            this.erro = this.httpErrorService.obterMensagem(error, 'Nao foi possivel carregar o dashboard.');
          }
          return of({
            resumo: null,
            financeiroMes: null,
            lancamentosGrafico: [] as Lancamento[],
            ordens: [] as OrdemServico[]
          });
        })
      )
      .subscribe(({ resumo, financeiroMes, lancamentosGrafico, ordens }) => {
        this.resumo = resumo;
        this.financeiroMes = financeiroMes;
        this.lancamentosGrafico = lancamentosGrafico;
        this.ordens = ordens;
        this.carregando = false;
      });
  }

  private get datasGrafico(): string[] {
    return Array.from({ length: 30 }, (_, index) => this.formatarData(this.adicionarDias(new Date(), index - 29)));
  }

  private get valoresGrafico(): number[] {
    const totaisPorData = new Map<string, number>();
    for (const lancamento of this.lancamentosGrafico) {
      if (lancamento.tipo !== 'ENTRADA') {
        continue;
      }
      const atual = totaisPorData.get(lancamento.dataLancamento) ?? 0;
      totaisPorData.set(lancamento.dataLancamento, atual + Number(lancamento.valor));
    }

    let acumulado = 0;
    return this.datasGrafico.map((data) => {
      acumulado += totaisPorData.get(data) ?? 0;
      return acumulado;
    });
  }

  private get totalEntradasUltimos30Dias(): number {
    return this.lancamentosGrafico
      .filter((lancamento) => lancamento.tipo === 'ENTRADA')
      .reduce((total, lancamento) => total + Number(lancamento.valor), 0);
  }

  private get maximoGrafico(): number {
    const max = Math.max(...this.valoresGrafico, 0);
    return max > 0 ? max * 1.08 : 1000;
  }

  private formatarTituloOrdem(ordem: OrdemServico): string {
    const veiculo = ordem.veiculoDescricao ?? ordem.clienteNome;
    const placa = ordem.veiculoPlaca;
    return placa ? `${veiculo} - ${placa}` : veiculo;
  }

  private rotuloStatusOrdem(status: StatusOrdem): string {
    const rotulos: Record<StatusOrdem, string> = {
      ABERTA: 'Aguardando',
      EM_EXECUCAO: 'Em andamento',
      CONCLUIDA: 'Concluido',
      ENTREGUE: 'Entregue',
      CANCELADA: 'Cancelado'
    };

    return rotulos[status];
  }

  private classeStatusOrdem(status: StatusOrdem): string {
    const classes: Record<StatusOrdem, string> = {
      ABERTA: 'aguardando',
      EM_EXECUCAO: 'em_andamento',
      CONCLUIDA: 'concluido',
      ENTREGUE: 'concluido',
      CANCELADA: 'cancelado'
    };

    return classes[status];
  }

  private formatarHorarioOrdem(value: string): string {
    const data = new Date(value);
    if (Number.isNaN(data.getTime())) {
      return 'Data invalida';
    }

    const hoje = new Date();
    const ontem = this.adicionarDias(hoje, -1);
    const prefixo =
      data.toDateString() === hoje.toDateString()
        ? 'Hoje'
        : data.toDateString() === ontem.toDateString()
          ? 'Ontem'
          : data.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' });
    const horario = data.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });

    return `${prefixo}, ${horario}`;
  }

  private formatarNumero(value: number): string {
    return new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 0 }).format(value);
  }

  private formatarMoeda(value: number): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
      maximumFractionDigits: 0
    })
      .format(value)
      .replace(/\u00a0/g, ' ');
  }

  private formatarCompacto(value: number): string {
    if (value >= 1000) {
      return `${Math.round(value / 1000)}k`;
    }
    return this.formatarNumero(value);
  }

  private formatarLabelGrafico(dataIso: string): string {
    const [ano, mes, dia] = dataIso.split('-').map(Number);
    return new Date(ano, mes - 1, dia).toLocaleDateString('pt-BR', { day: '2-digit', month: 'short' }).replace('.', '');
  }

  private adicionarDias(data: Date, dias: number): Date {
    const clone = new Date(data);
    clone.setDate(clone.getDate() + dias);
    return clone;
  }

  private formatarData(date: Date): string {
    const tzOffset = date.getTimezoneOffset() * 60000;
    return new Date(date.getTime() - tzOffset).toISOString().slice(0, 10);
  }
}
