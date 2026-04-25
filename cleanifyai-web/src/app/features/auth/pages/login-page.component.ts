import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { AuthService } from '../../../core/services/auth.service';
import { HttpErrorService } from '../../../core/services/http-error.service';

type LoginField = 'email' | 'senha';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.scss'
})
export class LoginPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly httpErrorService = inject(HttpErrorService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly logoMarkPath = 'brand/cleanifyai-logo-mark.svg';

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    senha: ['', [Validators.required, Validators.minLength(6)]]
  });

  carregando = false;
  erro = '';
  returnUrl = '/dashboard';

  ngOnInit(): void {
    this.returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/dashboard';

    if (this.route.snapshot.queryParamMap.get('session') === 'expired') {
      this.erro = 'Sua sessao expirou. Faca login novamente.';
    }
  }

  entrar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.carregando = true;
    this.erro = '';

    const payload = this.form.getRawValue();
    this.authService.login(payload).subscribe({
      next: () => {
        this.carregando = false;
        void this.router.navigateByUrl(this.returnUrl);
      },
      error: (error) => {
        this.carregando = false;
        this.erro = this.httpErrorService.obterMensagem(error, 'Nao foi possivel realizar o login.');
      }
    });
  }

  campoInvalido(campo: LoginField): boolean {
    const control = this.form.controls[campo];
    return control.invalid && (control.touched || control.dirty);
  }

  mensagemErro(campo: LoginField): string {
    const control = this.form.controls[campo];
    if (control.hasError('required')) {
      return 'Campo obrigatorio.';
    }
    if (control.hasError('email')) {
      return 'Informe um email valido.';
    }
    if (control.hasError('minlength')) {
      return 'A senha deve ter pelo menos 6 caracteres.';
    }
    return '';
  }
}
