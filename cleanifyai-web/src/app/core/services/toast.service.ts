import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export type ToastType = 'success' | 'error' | 'info';

export interface ToastItem {
  id: number;
  title: string;
  message: string;
  type: ToastType;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly toastsSubject = new BehaviorSubject<ToastItem[]>([]);
  private nextId = 1;

  readonly toasts$ = this.toastsSubject.asObservable();

  success(message: string, title = 'Sucesso'): void {
    this.adicionarToast('success', title, message);
  }

  error(message: string, title = 'Erro'): void {
    this.adicionarToast('error', title, message);
  }

  info(message: string, title = 'Aviso'): void {
    this.adicionarToast('info', title, message);
  }

  remover(id: number): void {
    this.toastsSubject.next(this.toastsSubject.value.filter((toast) => toast.id !== id));
  }

  private adicionarToast(type: ToastType, title: string, message: string): void {
    const id = this.nextId++;
    const toast: ToastItem = { id, title, message, type };
    this.toastsSubject.next([...this.toastsSubject.value, toast]);

    window.setTimeout(() => this.remover(id), 4200);
  }
}
