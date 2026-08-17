import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppIconComponent } from '../app-icon/app-icon.component';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [CommonModule, AppIconComponent],
  templateUrl: './empty-state.component.html',
  styleUrls: ['./empty-state.component.scss'],
})
export class EmptyStateComponent {
  @Input() icon: string = 'search';
  @Input() title: string = '';
  @Input() message: string = '';
  @Input() actionLabel: string = '';
  @Output() action = new EventEmitter<void>();
}
