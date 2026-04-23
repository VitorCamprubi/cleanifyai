import { Routes } from '@angular/router';

import { authChildGuard, authGuard } from './core/guards/auth.guard';
import { loginGuard } from './core/guards/login.guard';
import { AppShellComponent } from './layout/app-shell/app-shell.component';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [loginGuard],
    loadComponent: () => import('./features/auth/pages/login-page.component').then((m) => m.LoginPageComponent)
  },
  {
    path: '',
    canActivate: [authGuard],
    canActivateChild: [authChildGuard],
    component: AppShellComponent,
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard'
      },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/pages/dashboard-page.component').then((m) => m.DashboardPageComponent)
      },
      {
        path: 'clientes',
        loadComponent: () => import('./features/clientes/pages/clientes-page.component').then((m) => m.ClientesPageComponent)
      },
      {
        path: 'servicos',
        loadComponent: () => import('./features/servicos/pages/servicos-page.component').then((m) => m.ServicosPageComponent)
      },
      {
        path: 'agendamentos',
        loadComponent: () => import('./features/agendamentos/pages/agendamentos-page.component').then((m) => m.AgendamentosPageComponent)
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'login'
  }
];
