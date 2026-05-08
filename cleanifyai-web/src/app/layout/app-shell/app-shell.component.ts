import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Subscription, filter, interval } from 'rxjs';

import { AuthUser } from '../../core/models/auth.model';
import { AuthService } from '../../core/services/auth.service';
import { PingApiService } from '../../core/services/ping-api.service';
import { Theme, ThemeService } from '../../core/services/theme.service';
import { ToastService } from '../../core/services/toast.service';

type IconKey =
  | 'dashboard'
  | 'clientes'
  | 'veiculos'
  | 'servicos'
  | 'agendamentos'
  | 'ordens'
  | 'ajustes'
  | 'mais';

interface NavItem {
  label: string;
  route: string;
  caption: string;
  icon: IconKey;
  exact?: boolean;
}

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.scss'
})
export class AppShellComponent implements OnInit, OnDestroy {
  private readonly pingApi = inject(PingApiService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly themeService = inject(ThemeService);
  private readonly toastService = inject(ToastService);

  readonly nomeEmpresa = 'Cleanify Detailing';

  readonly navItems: NavItem[] = [
    { label: 'Dashboard', route: '/dashboard', caption: 'Visao geral', icon: 'dashboard' },
    { label: 'Clientes', route: '/clientes', caption: 'Base de relacionamento', icon: 'clientes' },
    { label: 'Ve\u00edculos', route: '/veiculos', caption: 'Frota dos clientes', icon: 'veiculos' },
    { label: 'Ordens', route: '/ordens', caption: 'Execucao operacional', icon: 'ordens' },
    { label: 'Agenda', route: '/agendamentos', caption: 'Agenda do dia', icon: 'agendamentos' },
    { label: 'Servi\u00e7os', route: '/servicos', caption: 'Catalogo e precificacao', icon: 'servicos' },
    { label: 'Ajustes', route: '/ajustes', caption: 'Preferencias', icon: 'ajustes' }
  ];

  readonly mobileNavItems: NavItem[] = [
    { label: 'Agenda', route: '/agendamentos', caption: 'Agenda do dia', icon: 'agendamentos' },
    { label: 'Ordens', route: '/ordens', caption: 'Execucao operacional', icon: 'ordens' },
    { label: 'Clientes', route: '/clientes', caption: 'Base de relacionamento', icon: 'clientes' },
    { label: 'Servi\u00e7os', route: '/servicos', caption: 'Catalogo e precificacao', icon: 'servicos' },
    { label: 'Mais', route: '/mais', caption: 'Mais opcoes', icon: 'mais' }
  ];

  apiOnline = false;
  pageTitle = 'Dashboard';
  pageCaption = 'Visao geral';
  menuMobileAberto = false;
  notificacoesAbertas = false;
  menuUsuarioAberto = false;
  temaAtual: Theme = 'dark';

  private routerSub?: Subscription;
  private themeSub?: Subscription;
  private pingSub?: Subscription;

  ngOnInit(): void {
    this.verificarApi(false);
    this.pingSub = interval(30000).subscribe(() => this.verificarApi(false));

    this.themeSub = this.themeService.theme$.subscribe((tema) => {
      this.temaAtual = tema;
    });

    this.atualizarTituloPorRota(this.router.url);
    this.routerSub = this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe((evento) => {
        this.atualizarTituloPorRota(evento.urlAfterRedirects);
        this.menuMobileAberto = false;
        this.notificacoesAbertas = false;
        this.menuUsuarioAberto = false;
      });
  }

  ngOnDestroy(): void {
    this.routerSub?.unsubscribe();
    this.themeSub?.unsubscribe();
    this.pingSub?.unsubscribe();
  }

  get usuarioAtual(): AuthUser | null {
    return this.authService.usuarioAtual;
  }

  get roleLabel(): string {
    return this.usuarioAtual?.role === 'ADMIN' ? 'Administrador' : 'Atendente';
  }

  toggleMenuMobile(): void {
    this.menuMobileAberto = !this.menuMobileAberto;
  }

  fecharMenuMobile(): void {
    this.menuMobileAberto = false;
  }

  alternarTema(): void {
    this.themeService.toggle();
  }

  toggleNotificacoes(): void {
    this.notificacoesAbertas = !this.notificacoesAbertas;
    this.menuUsuarioAberto = false;
  }

  toggleMenuUsuario(): void {
    this.menuUsuarioAberto = !this.menuUsuarioAberto;
    this.notificacoesAbertas = false;
  }

  fecharPaineis(): void {
    this.notificacoesAbertas = false;
    this.menuUsuarioAberto = false;
  }

  sincronizarAgora(): void {
    this.verificarApi(true);
  }

  logout(): void {
    this.authService.logout();
  }

  get notificacoes(): Array<{ title: string; message: string; tone: 'success' | 'warning' | 'info' }> {
    return [
      {
        title: this.apiOnline ? 'Sistema sincronizado' : 'API offline',
        message: this.apiOnline ? 'Conexao com o backend ativa.' : 'Nao foi possivel falar com o backend agora.',
        tone: this.apiOnline ? 'success' : 'warning'
      },
      {
        title: 'Sessao ativa',
        message: `${this.usuarioAtual?.nome ?? 'Usuario'} - ${this.roleLabel}`,
        tone: 'info'
      },
      {
        title: 'Tema atual',
        message: this.temaAtual === 'dark' ? 'Modo escuro habilitado.' : 'Modo claro habilitado.',
        tone: 'info'
      }
    ];
  }

  private atualizarTituloPorRota(url: string): void {
    const path = url.split('?')[0];
    const ordenado = [...this.navItems, ...this.mobileNavItems].sort((a, b) => b.route.length - a.route.length);
    const item = ordenado.find((n) => path === n.route || (!n.exact && path.startsWith(n.route)));

    this.pageTitle = item?.label ?? 'Dashboard';
    this.pageCaption = item?.caption ?? 'Visao geral';
  }

  private verificarApi(exibirToast: boolean): void {
    this.pingApi.ping().subscribe({
      next: () => {
        this.apiOnline = true;
        if (exibirToast) {
          this.toastService.success('Sincronizacao com a API confirmada.');
        }
      },
      error: () => {
        this.apiOnline = false;
        if (exibirToast) {
          this.toastService.error('API offline ou indisponivel.');
        }
      }
    });
  }
}
