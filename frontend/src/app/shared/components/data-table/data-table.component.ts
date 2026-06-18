import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ColumnDef } from '../../models/shared.models';

@Component({
  selector: 'app-data-table',
  standalone: true,
  imports: [CommonModule, MatTableModule, MatSortModule, MatProgressSpinnerModule],
  templateUrl: './data-table.component.html',
  styleUrls: ['./data-table.component.scss'],
})
export class DataTableComponent {
  @Input() columns: ColumnDef[] = [];
  @Input() data: any[] = [];
  @Input() sortable: boolean = true;
  @Input() loading: boolean = false;

  @Output() sortChange = new EventEmitter<Sort>();

  get displayedColumns(): string[] {
    return this.columns.map((col) => col.field);
  }

  onSortChange(sort: Sort): void {
    this.sortChange.emit(sort);
  }

  getCellValue(row: any, column: ColumnDef): string | number {
    const value = row[column.field];
    if (value == null) {
      return 'N/A';
    }
    if (column.format === 'percent') {
      return `${(value as number).toFixed(2)}%`;
    }
    if (column.format === 'bigNumber') {
      const abs = Math.abs(value as number);
      const sign = (value as number) < 0 ? '-' : '';
      if (abs >= 1e12) return `${sign}$${(abs / 1e12).toFixed(2)}T`;
      if (abs >= 1e9) return `${sign}$${(abs / 1e9).toFixed(2)}B`;
      if (abs >= 1e6) return `${sign}$${(abs / 1e6).toFixed(2)}M`;
      return `${sign}$${abs.toFixed(2)}`;
    }
    if (column.format === 'decimal') {
      return (value as number).toLocaleString('en-US', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      });
    }
    return value;
  }
}
