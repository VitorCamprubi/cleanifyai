import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';
import { PingApiService } from '../../core/services/ping-api.service';
import { ThemeService } from '../../core/services/theme.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-settings-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './settings-page.component.html',
  styleUrl: './settings-page.component.scss'
})
export class SettingsPageComponent {
  private readonly authService = inject(AuthService);
  private readonly themeService = inject(ThemeService);
  private readonly pingApi = inject(PingApiService);
  private readonly toastService = inject(ToastService);

  verificandoApi = false;
  apiOnline: boolean | null = null;

  get usuarioAtual() {
    return this.authService.usuarioAtual;
  }

  get temaAtual() {
    return this.themeService.current;
  }

  alternarTema(): void {
    this.themeService.toggle();
  }

  verificarApi(): void {
    if (this.verificandoApi) {
      return;
    }

    this.verificandoApi = true;
    this.pingApi.ping().subscribe({
      next: () => {
        this.apiOnline = true;
        this.verificandoApi = false;
        this.toastService.success('API conectada.');
      },
      error: () => {
        this.apiOnline = false;
        this.verificandoApi = false;
        this.toastService.error('API offline ou indisponivel.');
      }
    });
  }

  logout(): void {
    this.authService.logout();
  }
}
