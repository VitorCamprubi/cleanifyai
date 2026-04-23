import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { ApiErrorResponse } from '../models/api-error.model';

@Injectable({ providedIn: 'root' })
export class HttpErrorService {
  obterMensagem(error: unknown, fallback: string): string {
    if (!(error instanceof HttpErrorResponse)) {
      return fallback;
    }

    if (error.status === 0) {
      return 'Nao foi possivel conectar ao backend. Verifique a API e tente novamente.';
    }

    if (typeof error.error === 'string' && error.error.trim()) {
      return error.error;
    }

    const apiError = error.error as Partial<ApiErrorResponse> | null;
    if (apiError?.message) {
      return apiError.message;
    }

    if (error.status === 404) {
      return 'Registro nao encontrado.';
    }

    if (error.status === 409) {
      return 'Operacao bloqueada por integridade de dados.';
    }

    if (error.status >= 500) {
      return 'O backend encontrou um erro interno. Tente novamente em instantes.';
    }

    return fallback;
  }
}
