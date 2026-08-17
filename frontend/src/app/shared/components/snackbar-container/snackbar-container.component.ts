import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppIconComponent } from '../app-icon/app-icon.component';
import { SnackbarService } from '../../services/snackbar.service';

@Component({
  selector: 'app-snackbar-container',
  standalone: true,
  imports: [CommonModule, AppIconComponent],
  template: `
    <div class="fixed bottom-6 right-6 z-[100] flex w-[min(24rem,90vw)] flex-col gap-2">
      @for (toast of service.toasts$(); track toast.id) {
        <div
          class="flex items-center gap-3 rounded-lg border px-4 py-3 text-sm shadow-lg"
          [ngClass]="{
            'border-red-200 bg-red-50 text-red-800': toast.type === 'error',
            'border-green-200 bg-green-50 text-green-800': toast.type === 'success',
            'border-blue-200 bg-blue-50 text-blue-800': toast.type === 'info'
          }"
        >
          <app-icon
            [name]="toast.type === 'error' ? 'error_outline' : toast.type === 'success' ? 'check' : 'info'"
            [size]="18"
          />
          <span class="flex-1">{{ toast.message }}</span>
          @if (toast.action) {
            <button
              type="button"
              class="text-xs font-semibold uppercase tracking-wide opacity-80 hover:opacity-100"
              (click)="service.dismiss(toast.id)"
            >
              {{ toast.action }}
            </button>
          }
          <button
            type="button"
            class="rounded p-0.5 opacity-50 transition hover:opacity-100"
            (click)="service.dismiss(toast.id)"
            aria-label="Dismiss"
          >
            <app-icon name="close" [size]="14" />
          </button>
        </div>
      }
    </div>
  `,
})
export class SnackbarContainerComponent {
  readonly service = inject(SnackbarService);
}
