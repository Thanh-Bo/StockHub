import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ColumnDef } from '../../models/shared.models';
import { LoadingSpinnerComponent } from '../loading-spinner/loading-spinner.component';

export interface TableSort {
  active: string;
  direction: 'asc' | 'desc' | '';
}

@Component({
  selector: 'app-data-table',
  standalone: true,
  imports: [CommonModule, LoadingSpinnerComponent],
  templateUrl: './data-table.component.html',
  styleUrls: ['./data-table.component.scss'],
})
export class DataTableComponent {
  @Input() columns: ColumnDef[] = [];
  @Input() data: any[] = [];
  @Input() sortable: boolean = true;
  @Input() loading: boolean = false;

  @Output() sortChange = new EventEmitter<TableSort>();

  activeSort: string = '';
  sortDirection: 'asc' | 'desc' | '' = '';

  get displayedColumns(): string[] {
    return this.columns.map((col) => col.field);
  }

  onHeaderClick(field: string): void {
    if (!this.sortable) return;
    if (this.activeSort !== field) {
      this.activeSort = field;
      this.sortDirection = 'asc';
    } else {
      this.sortDirection =
        this.sortDirection === 'asc' ? 'desc' : this.sortDirection === 'desc' ? '' : 'asc';
      if (this.sortDirection === '') {
        this.activeSort = '';
      }
    }
    this.sortChange.emit({ active: this.activeSort, direction: this.sortDirection });
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
