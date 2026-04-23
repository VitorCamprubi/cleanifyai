import { of } from 'rxjs';

import { TestBed } from '@angular/core/testing';

import { Servico } from '../../../core/models/servico.model';
import { AuthService } from '../../../core/services/auth.service';
import { DashboardRefreshService } from '../../../core/services/dashboard-refresh.service';
import { HttpErrorService } from '../../../core/services/http-error.service';
import { ServicosApiService } from '../../../core/services/servicos-api.service';
import { ToastService } from '../../../core/services/toast.service';
import { ServicosPageComponent } from './servicos-page.component';

describe('ServicosPageComponent', () => {
  const servico: Servico = {
    id: 1,
    nome: 'Lavagem Premium',
    descricao: 'Lavagem externa completa',
    preco: 89.9,
    duracaoMinutos: 90,
    ativo: true
  };

  function configure(isAdmin: boolean) {
    const servicosApi = jasmine.createSpyObj<ServicosApiService>('ServicosApiService', ['listar', 'criar', 'atualizar', 'excluir']);
    const authService = jasmine.createSpyObj<AuthService>('AuthService', ['isAdmin']);
    const httpErrorService = jasmine.createSpyObj<HttpErrorService>('HttpErrorService', ['obterMensagem']);
    const toastService = jasmine.createSpyObj<ToastService>('ToastService', ['success', 'error']);
    const dashboardRefreshService = jasmine.createSpyObj<DashboardRefreshService>('DashboardRefreshService', ['solicitarAtualizacao']);

    servicosApi.listar.and.returnValue(of([servico]));
    authService.isAdmin.and.returnValue(isAdmin);
    httpErrorService.obterMensagem.and.returnValue('Erro ao carregar servicos.');

    TestBed.configureTestingModule({
      imports: [ServicosPageComponent],
      providers: [
        { provide: ServicosApiService, useValue: servicosApi },
        { provide: AuthService, useValue: authService },
        { provide: HttpErrorService, useValue: httpErrorService },
        { provide: ToastService, useValue: toastService },
        { provide: DashboardRefreshService, useValue: dashboardRefreshService }
      ]
    });

    const fixture = TestBed.createComponent(ServicosPageComponent);
    fixture.detectChanges();

    return fixture;
  }

  it('shows a read-only state for atendente users', () => {
    const fixture = configure(false);
    const text = fixture.nativeElement.textContent as string;

    expect(fixture.nativeElement.querySelector('form')).toBeNull();
    expect(text).toContain('acesso somente de leitura');
    expect(text).not.toContain('Excluir');
  });

  it('shows form and row actions for admin users', () => {
    const fixture = configure(true);
    const text = fixture.nativeElement.textContent as string;

    expect(fixture.nativeElement.querySelector('form')).not.toBeNull();
    expect(text).toContain('Cadastrar servico');
    expect(text).toContain('Editar');
    expect(text).toContain('Excluir');
  });
});
