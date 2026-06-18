import { Component, inject, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSliderModule } from '@angular/material/slider';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatBadgeModule } from '@angular/material/badge';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ScreenerStore } from '../store/screener.store';
import { ScreenerResultItem, FilterCriteria, SortConfig } from '../models/screener.models';
import { LoadingSpinnerComponent } from '@app/shared/components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state.component';
import { BigNumberPipe } from '@app/shared/pipes/big-number.pipe';
import { StockPercentPipe } from '@app/shared/pipes/percent.pipe';

@Component({
  selector: 'app-stock-screener',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatCheckboxModule,
    MatSliderModule,
    MatSelectModule,
    MatFormFieldModule,
    MatExpansionModule,
    MatPaginatorModule,
    MatBadgeModule,
    MatChipsModule,
    MatTooltipModule,
    LoadingSpinnerComponent,
    EmptyStateComponent,
    BigNumberPipe,
    StockPercentPipe,
  ],
  templateUrl: './stock-screener.component.html',
  styleUrls: ['./stock-screener.component.scss'],
})
export class StockScreenerComponent implements OnInit {
  readonly store = inject(ScreenerStore);

  readonly leftPanelOpen = signal(true);

  // Slider filter state
  readonly marketCapRange = signal<[number, number]>([0, 3_000_000_000_000]);
  readonly peRange = signal<[number, number]>([0, 1000]);
  readonly revenueGrowthRange = signal<[number, number]>([-100, 1000]);
  readonly dividendYieldRange = signal<[number, number]>([0, 20]);
  readonly roeRange = signal<[number, number]>([-500, 500]);
  readonly debtToEquityRange = signal<[number, number]>([0, 100]);
  readonly netMarginRange = signal<[number, number]>([-100, 100]);

  // Sector checkbox state
  readonly sectors = [
    { label: 'Technology', selected: false },
    { label: 'Healthcare', selected: false },
    { label: 'Financial', selected: false },
    { label: 'Consumer Cyclical', selected: false },
    { label: 'Industrials', selected: false },
    { label: 'Consumer Defensive', selected: false },
    { label: 'Energy', selected: false },
    { label: 'Utilities', selected: false },
    { label: 'Real Estate', selected: false },
    { label: 'Basic Materials', selected: false },
    { label: 'Communication Services', selected: false },
  ];

  readonly sortFields: { field: string; label: string }[] = [
    { field: 'marketCap', label: 'Market Cap' },
    { field: 'peRatio', label: 'P/E Ratio' },
    { field: 'revenueGrowthYoY', label: 'Revenue Growth' },
    { field: 'roe', label: 'ROE' },
    { field: 'dividendYield', label: 'Dividend Yield' },
    { field: 'netMargin', label: 'Net Margin' },
    { field: 'debtToEquity', label: 'Debt to Equity' },
  ];

  readonly sortDirections: { value: 'ASC' | 'DESC'; label: string }[] = [
    { value: 'ASC', label: 'Ascending' },
    { value: 'DESC', label: 'Descending' },
  ];

  // Columns for the results table
  readonly columns = [
    { field: 'ticker', header: 'Ticker' },
    { field: 'name', header: 'Name' },
    { field: 'sector', header: 'Sector' },
    { field: 'marketCap', header: 'Market Cap', format: 'bigNumber' },
    { field: 'peRatio', header: 'P/E', format: 'decimal' },
    { field: 'revenueGrowthYoY', header: 'Rev Growth', format: 'percent' },
    { field: 'roe', header: 'ROE', format: 'percent' },
    { field: 'dividendYield', header: 'Div Yield', format: 'percent' },
    { field: 'netMargin', header: 'Net Margin', format: 'percent' },
    { field: 'debtToEquity', header: 'D/E', format: 'decimal' },
  ];

  ngOnInit(): void {
    // Trigger initial search with defaults
    this.applyFilters();
  }

  toggleLeftPanel(): void {
    this.leftPanelOpen.update((v) => !v);
  }

  applyFilters(): void {
    const filters: FilterCriteria[] = [];

    // Range filters
    if (this.marketCapRange()[0] > 0 || this.marketCapRange()[1] < 3_000_000_000_000) {
      filters.push({
        field: 'marketCap',
        operator: 'BETWEEN',
        minValue: this.marketCapRange()[0],
        maxValue: this.marketCapRange()[1],
      });
    }
    if (this.peRange()[0] > 0 || this.peRange()[1] < 1000) {
      filters.push({
        field: 'peRatio',
        operator: 'BETWEEN',
        minValue: this.peRange()[0],
        maxValue: this.peRange()[1],
      });
    }
    if (this.revenueGrowthRange()[0] > -100 || this.revenueGrowthRange()[1] < 1000) {
      filters.push({
        field: 'revenueGrowthYoY',
        operator: 'BETWEEN',
        minValue: this.revenueGrowthRange()[0],
        maxValue: this.revenueGrowthRange()[1],
      });
    }
    if (this.dividendYieldRange()[0] > 0 || this.dividendYieldRange()[1] < 20) {
      filters.push({
        field: 'dividendYield',
        operator: 'BETWEEN',
        minValue: this.dividendYieldRange()[0],
        maxValue: this.dividendYieldRange()[1],
      });
    }
    if (this.roeRange()[0] > -500 || this.roeRange()[1] < 500) {
      filters.push({
        field: 'roe',
        operator: 'BETWEEN',
        minValue: this.roeRange()[0],
        maxValue: this.roeRange()[1],
      });
    }
    if (this.debtToEquityRange()[0] > 0 || this.debtToEquityRange()[1] < 100) {
      filters.push({
        field: 'debtToEquity',
        operator: 'BETWEEN',
        minValue: this.debtToEquityRange()[0],
        maxValue: this.debtToEquityRange()[1],
      });
    }
    if (this.netMarginRange()[0] > -100 || this.netMarginRange()[1] < 100) {
      filters.push({
        field: 'netMargin',
        operator: 'BETWEEN',
        minValue: this.netMarginRange()[0],
        maxValue: this.netMarginRange()[1],
      });
    }

    // Sector filter
    const selectedSectors = this.sectors.filter((s) => s.selected).map((s) => s.label);
    if (selectedSectors.length > 0 && selectedSectors.length < this.sectors.length) {
      filters.push({
        field: 'sector',
        operator: 'IN',
        values: selectedSectors,
      });
    }

    this.store.clearFilters();
    for (const filter of filters) {
      this.store.addFilter(filter);
    }
    this.store.search();
  }

  clearAllFilters(): void {
    this.marketCapRange.set([0, 3_000_000_000_000]);
    this.peRange.set([0, 1000]);
    this.revenueGrowthRange.set([-100, 1000]);
    this.dividendYieldRange.set([0, 20]);
    this.roeRange.set([-500, 500]);
    this.debtToEquityRange.set([0, 100]);
    this.netMarginRange.set([-100, 100]);
    this.sectors.forEach((s) => (s.selected = false));
    this.store.clearFilters();
    this.store.search();
  }

  onSortFieldChange(field: string): void {
    const currentSort = this.store.sort();
    this.store.setSort({ field, direction: currentSort.direction });
    this.store.search();
  }

  onSortDirectionChange(direction: 'ASC' | 'DESC'): void {
    const currentSort = this.store.sort();
    this.store.setSort({ field: currentSort.field, direction });
    this.store.search();
  }

  onPageChange(event: PageEvent): void {
    this.store.setPage(event.pageIndex);
    this.store.search();
  }

  getMarketCapSliderLabel(value: number): string {
    if (value >= 1e12) return `$${(value / 1e12).toFixed(1)}T`;
    if (value >= 1e9) return `$${(value / 1e9).toFixed(0)}B`;
    if (value >= 1e6) return `$${(value / 1e6).toFixed(0)}M`;
    return `$${value}`;
  }

  formatCellValue(item: ScreenerResultItem, field: string): string {
    const value = (item as any)[field];
    if (value == null) return 'N/A';
    switch (field) {
      case 'marketCap':
        return this.getMarketCapSliderLabel(value);
      case 'peRatio':
      case 'debtToEquity':
        return value.toFixed(2);
      case 'revenueGrowthYoY':
      case 'roe':
      case 'dividendYield':
      case 'netMargin':
        return `${value.toFixed(2)}%`;
      default:
        return String(value);
    }
  }

  getCellColorClass(item: ScreenerResultItem, field: string): string {
    const value = (item as any)[field];
    if (value == null) return '';
    switch (field) {
      case 'revenueGrowthYoY':
      case 'roe':
      case 'dividendYield':
      case 'netMargin':
        return value >= 0 ? 'positive' : 'negative';
      case 'peRatio':
      case 'debtToEquity':
        return value > 0 ? 'neutral' : 'negative';
      default:
        return '';
    }
  }
}
