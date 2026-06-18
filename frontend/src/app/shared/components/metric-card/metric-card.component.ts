import { Component, Input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CommonModule } from '@angular/common';
import { BigNumberPipe } from '../../pipes/big-number.pipe';
import { StockPercentPipe } from '../../pipes/percent.pipe';

@Component({
  selector: 'app-metric-card',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule, MatTooltipModule, BigNumberPipe, StockPercentPipe],
  templateUrl: './metric-card.component.html',
  styleUrls: ['./metric-card.component.scss'],
})
export class MetricCardComponent {
  @Input() label: string = '';
  @Input() value: number | string = 0;
  @Input() format: 'bigNumber' | 'percent' | 'decimal' = 'decimal';
  @Input() trend: 'up' | 'down' | 'neutral' = 'neutral';
  @Input() tooltip: string = '';

  get formattedValue(): string {
    const num = typeof this.value === 'string' ? parseFloat(this.value) : this.value;
    if (isNaN(num)) {
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
