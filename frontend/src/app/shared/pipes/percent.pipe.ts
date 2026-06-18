import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'stockPercent',
  standalone: true,
})
export class StockPercentPipe implements PipeTransform {
  transform(value: number | null | undefined, decimals: number = 2): string {
    if (value == null || isNaN(value)) {
      return 'N/A';
    }
    return `${value.toFixed(decimals)}%`;
  }
}
