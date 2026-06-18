import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'bigNumber',
  standalone: true,
})
export class BigNumberPipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    if (value == null || isNaN(value)) {
      return 'N/A';
    }
    const abs = Math.abs(value);
    const sign = value < 0 ? '-' : '';

    if (abs >= 1e12) {
      return `${sign}$${(abs / 1e12).toFixed(2)}T`;
    }
    if (abs >= 1e9) {
      return `${sign}$${(abs / 1e9).toFixed(2)}B`;
    }
    if (abs >= 1e6) {
      return `${sign}$${(abs / 1e6).toFixed(2)}M`;
    }
    return `${sign}$${abs.toFixed(2)}`;
  }
}
