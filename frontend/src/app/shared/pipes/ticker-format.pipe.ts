import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'tickerFormat',
  standalone: true,
})
export class TickerFormatPipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    if (!value) {
      return '';
    }
    return value.toUpperCase();
  }
}
