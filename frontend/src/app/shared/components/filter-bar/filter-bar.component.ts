import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnChanges,
  SimpleChanges,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FilterConfig, FilterOption } from '../../models/shared.models';
import { AppIconComponent } from '../app-icon/app-icon.component';

@Component({
  selector: 'app-filter-bar',
  standalone: true,
  imports: [CommonModule, FormsModule, AppIconComponent],
  templateUrl: './filter-bar.component.html',
  styleUrls: ['./filter-bar.component.scss'],
})
export class FilterBarComponent implements OnChanges {
  @Input() filters: FilterConfig[] = [];
  @Output() filterChange = new EventEmitter<Record<string, any>>();

  filterValues: Record<string, any> = {};
  openMenuIndex: number | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['filters']) {
      this.initializeFilterValues();
    }
  }

  toggleMenu(index: number): void {
    this.openMenuIndex = this.openMenuIndex === index ? null : index;
  }

  onRangeChange(field: string, value: number): void {
    this.filterValues[field] = value;
    this.emitChanges();
  }

  onCheckboxChange(field: string, option: FilterOption, checked: boolean): void {
    if (!this.filterValues[field]) {
      this.filterValues[field] = [];
    }
    if (checked) {
      this.filterValues[field].push(option.value);
    } else {
      this.filterValues[field] = this.filterValues[field].filter(
        (v: any) => v !== option.value
      );
    }
    this.emitChanges();
  }

  onClearAll(): void {
    this.initializeFilterValues();
    this.emitChanges();
  }

  hasActiveFilters(): boolean {
    return this.filters.some((f) => {
      const val = this.filterValues[f.field];
      if (f.type === 'range') {
        return val !== f.min;
      }
      return val && val.length > 0;
    });
  }

  getActiveCount(): number {
    let count = 0;
    for (const f of this.filters) {
      const val = this.filterValues[f.field];
      if (f.type === 'range' && val !== f.min) {
        count++;
      }
      if (f.type === 'select' && val && val.length > 0) {
        count++;
      }
    }
    return count;
  }

  getFilterLabel(filter: FilterConfig): string {
    const val = this.filterValues[filter.field];
    if (filter.type === 'range') {
      return `${filter.label}: ${val}`;
    }
    if (filter.type === 'select' && val?.length > 0) {
      return `${filter.label} (${val.length})`;
    }
    return filter.label;
  }

  private initializeFilterValues(): void {
    for (const f of this.filters) {
      if (f.type === 'range') {
        this.filterValues[f.field] = f.min ?? 0;
      } else {
        this.filterValues[f.field] = [];
      }
    }
  }

  private emitChanges(): void {
    this.filterChange.emit({ ...this.filterValues });
  }
}
