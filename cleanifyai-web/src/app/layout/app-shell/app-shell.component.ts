import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

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

  readonly navItems: NavItem[] = [
    { label: 'Dashboard', route: '/dashboard', caption: 'Resumo da operacao' },
    { label: 'Clientes', route: '/clientes', caption: 'CRM operacional basico' },
    { label: 'Servicos', route: '/servicos', caption: 'Catalogo e precificacao' },
    { label: 'Agendamentos', route: '/agendamentos', caption: 'Agenda e status' }
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
}
