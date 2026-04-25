import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { AuthService } from '../../../core/services/auth.service';
import { HttpErrorService } from '../../../core/services/http-error.service';

@Component({
  selector: 'app-signup-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './signup-page.component.html',
  styleUrl: './signup-page.component.scss'
})
export class SignupPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly httpErrorService = inject(HttpErrorService);
  private readonly router = inject(Router);

  readonly logoMarkPath = 'brand/cleanifyai-logo-mark.svg';

  readonly form = this.fb.nonNullable.group({
    empresa: this.fb.nonNullable.group({
      nome: ['', [Validators.required, Validators.maxLength(160)]],
      cnpj: [''],
      telefone: [''],
      email: ['', [Validators.email]]
    }),
    admin: this.fb.nonNullable.group({
      nome: ['', [Validators.required, Validators.maxLength(120)]],
      email: ['', [Validators.required, Validators.email]],
      senha: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(64)]]
    })
  });

  carregando = false;
  erro = '';

  cadastrar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.carregando = true;
    this.erro = '';

    const payload = this.form.getRawValue();
    this.authService.registerCompany(payload).subscribe({
      next: () => {
        this.carregando = false;
        void this.router.navigate(['/dashboard']);
      },
      error: (error) => {
        this.carregando = false;
        this.erro = this.httpErrorService.obterMensagem(error, 'Nao foi possivel concluir o cadastro.');
      }
    });
  }

  empresaInvalida(campo: 'nome' | 'cnpj' | 'telefone' | 'email'): boolean {
    const control = this.form.controls.empresa.controls[campo];
    return control.invalid && (control.touched || control.dirty);
  }

  adminInvalido(campo: 'nome' | 'email' | 'senha'): boolean {
    const control = this.form.controls.admin.controls[campo];
    return control.invalid && (control.touched || control.dirty);
  }
}
