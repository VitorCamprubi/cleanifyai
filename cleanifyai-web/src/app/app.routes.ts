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
    path: 'signup',
    canActivate: [loginGuard],
    loadComponent: () => import('./features/auth/pages/signup-page.component').then((m) => m.SignupPageComponent)
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
        path: 'veiculos',
        loadComponent: () => import('./features/veiculos/pages/veiculos-page.component').then((m) => m.VeiculosPageComponent)
      },
      {
        path: 'servicos',
        loadComponent: () => import('./features/servicos/pages/servicos-page.component').then((m) => m.ServicosPageComponent)
      },
      {
        path: 'agendamentos',
        loadComponent: () => import('./features/agendamentos/pages/agendamentos-page.component').then((m) => m.AgendamentosPageComponent)
      },
      {
        path: 'ordens',
        loadComponent: () => import('./features/ordens/pages/ordens-page.component').then((m) => m.OrdensPageComponent)
      },
      {
        path: 'financeiro',
        loadComponent: () => import('./features/financeiro/pages/financeiro-page.component').then((m) => m.FinanceiroPageComponent)
      },
      {
        path: 'financeiro/categorias',
        loadComponent: () => import('./features/financeiro/pages/categorias-page.component').then((m) => m.CategoriasPageComponent)
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'login'
  }
];
