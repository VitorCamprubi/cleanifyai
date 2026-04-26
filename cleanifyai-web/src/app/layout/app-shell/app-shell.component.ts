import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthUser } from '../../core/models/auth.model';
import { AuthService } from '../../core/services/auth.service';
import { PingApiService } from '../../core/services/ping-api.service';

interface NavItem {
  label: string;
  route: string;
  caption: string;
}

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.scss'
})
export class AppShellComponent implements OnInit {
  private readonly pingApi = inject(PingApiService);
  private readonly authService = inject(AuthService);

  readonly logoMarkPath = 'brand/cleanifyai-logo-mark.svg';

  readonly navItems: NavItem[] = [
    { label: 'Dashboard', route: '/dashboard', caption: 'Resumo da operacao' },
    { label: 'Clientes', route: '/clientes', caption: 'CRM operacional basico' },
    { label: 'Veiculos', route: '/veiculos', caption: 'Frota dos clientes' },
    { label: 'Servicos', route: '/servicos', caption: 'Catalogo e precificacao' },
    { label: 'Agendamentos', route: '/agendamentos', caption: 'Agenda e status' },
    { label: 'Ordens de Servico', route: '/ordens', caption: 'OS e execucao' },
    { label: 'Financeiro', route: '/financeiro', caption: 'Caixa e lancamentos' },
    { label: 'Categorias', route: '/financeiro/categorias', caption: 'Receita e despesa' }
  ];

  apiOnline = false;

  ngOnInit(): void {
    this.pingApi.ping().subscribe({
      next: () => {
        this.apiOnline = true;
      },
      error: () => {
        this.apiOnline = false;
      }
    });
  }

  get usuarioAtual(): AuthUser | null {
    return this.authService.usuarioAtual;
  }

  get roleLabel(): string {
    return this.usuarioAtual?.role === 'ADMIN' ? 'Administrador' : 'Atendente';
  }

  logout(): void {
    this.authService.logout();
  }
}
