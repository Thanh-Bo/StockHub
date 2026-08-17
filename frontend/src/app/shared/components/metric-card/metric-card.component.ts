import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppIconComponent } from '../app-icon/app-icon.component';
import { BigNumberPipe } from '../../pipes/big-number.pipe';
import { StockPercentPipe } from '../../pipes/percent.pipe';

@Component({
  selector: 'app-metric-card',
  standalone: true,
  imports: [CommonModule, AppIconComponent],
  templateUrl: './metric-card.component.html',
  styleUrls: ['./metric-card.component.scss'],
})
export class MetricCardComponent {
  @Input() label: string = '';
  @Input() value: number | string | null = 0;
  @Input() format: string = 'decimal';
  @Input() trend?: 'up' | 'down' | 'neutral';
  @Input() tooltip: string = '';

  get formattedValue(): string {
    const num = typeof this.value === 'string' ? parseFloat(this.value) : this.value;
    if (num == null || isNaN(num)) {
      return String(this.value);
    }
    switch (this.format) {
      case 'bigNumber':
        return new BigNumberPipe().transform(num);
      case 'percent':
        return new StockPercentPipe().transform(num);
      case 'decimal':
        return num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
      default:
        return String(this.value);
    }
  }

  get trendIcon(): string {
    switch (this.trend) {
      case 'up':
        return 'trending_up';
      case 'down':
        return 'trending_down';
      case 'neutral':
        return 'trending_flat';
      default:
        return 'trending_flat';
    }
  }
}
