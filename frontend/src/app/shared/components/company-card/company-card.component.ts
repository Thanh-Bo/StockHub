import { Component, Input } from '@angular/core';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { CommonModule } from '@angular/common';
import { BigNumberPipe } from '../../pipes/big-number.pipe';
import { StockPercentPipe } from '../../pipes/percent.pipe';
import { TickerFormatPipe } from '../../pipes/ticker-format.pipe';

@Component({
  selector: 'app-company-card',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatChipsModule,
    BigNumberPipe,
    StockPercentPipe,
    TickerFormatPipe,
  ],
  templateUrl: './company-card.component.html',
  styleUrls: ['./company-card.component.scss'],
})
export class CompanyCardComponent {
  @Input() ticker: string = '';
  @Input() name: string = '';
  @Input() sector: string = '';
  @Input() marketCap: number = 0;
  @Input() price: number = 0;
  @Input() priceChangePercent: number = 0;

  get priceColor(): string {
    if (this.priceChangePercent > 0) {
      return '#22c55e';
    }
    if (this.priceChangePercent < 0) {
      return '#ef4444';
    }
    return '#6b7280';
  }
}
