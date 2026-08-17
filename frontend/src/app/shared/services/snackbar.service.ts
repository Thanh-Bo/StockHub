import { Injectable, signal } from '@angular/core';

export type SnackbarType = 'info' | 'success' | 'error';

export interface SnackbarConfig {
  duration?: number;
  type?: SnackbarType;
  action?: string;
}

export interface SnackbarData extends Required<Pick<SnackbarConfig, 'type' | 'action'>> {
  id: number;
  message: string;
  duration: number;
}

/**
 * Lightweight replacement for Angular Material's MatSnackBar.
 * Keeps the same `open(message, action, config)` signature for easy migration.
 */
@Injectable({ providedIn: 'root' })
export class SnackbarService {
  private readonly toasts = signal<SnackbarData[]>([]);
  private nextId = 1;

  readonly toasts$ = this.toasts.asReadonly();

  open(message: string, action = '', config?: SnackbarConfig): void {
    const id = this.nextId++;
    const toast: SnackbarData = {
      id,
      message,
      action,
      type: config?.type ?? 'info',
      duration: config?.duration ?? 4000,
    };
    this.toasts.update((t) => [...t, toast]);
    if (toast.duration > 0) {
      setTimeout(() => this.dismiss(id), toast.duration);
    }
  }

  success(message: string, duration = 3000): void {
    this.open(message, '', { type: 'success', duration });
  }

  error(message: string, duration = 5000): void {
    this.open(message, '', { type: 'error', duration });
  }

  dismiss(id: number): void {
    this.toasts.update((t) => t.filter((x) => x.id !== id));
  }

  clear(): void {
    this.toasts.set([]);
  }
}
