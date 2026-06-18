import { Component, inject, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ComparisonService } from '../services/comparison.service';
import {
  ComparisonResponse,
  CompanyComparisonRow,
} from '../models/comparison.models';
import { LoadingSpinnerComponent } from '@app/shared/components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state.component';
import { BigNumberPipe } from '@app/shared/pipes/big-number.pipe';
import { StockPercentPipe } from '@app/shared/pipes/percent.pipe';
import { finalize } from 'rxjs';

interface MetricCategory {
  name: string;
  label: string;
  metrics: string[];
}

@Component({
  selector: 'app-company-comparison',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule,
    MatTooltipModule,
    MatSnackBarModule,
    LoadingSpinnerComponent,
    EmptyStateComponent,
    BigNumberPipe,
    StockPercentPipe,
  ],
  templateUrl: './company-comparison.component.html',
  styleUrls: ['./company-comparison.component.scss'],
})
export class CompanyComparisonComponent implements OnInit {
  private readonly comparisonService = inject(ComparisonService);
  private readonly snackBar = inject(MatSnackBar);

  readonly tickers = signal<string[]>([]);
  readonly tickerInput = signal('');
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly showIndustryAverages = signal(false);
  readonly response = signal<ComparisonResponse | null>(null);

  readonly metricCategories: MetricCategory[] = [
    {
      name: 'VALUATION',
      label: 'Valuation',
      metrics: ['peRatio', 'pegRatio', 'pbRatio', 'dividendYield'],
    },
    {
      name: 'PROFITABILITY',
      label: 'Profitability',
      metrics: ['roe', 'roa', 'grossMargin', 'operatingMargin', 'netMargin'],
    },
    {
      name: 'GROWTH',
      label: 'Growth',
      metrics: ['revenueGrowthYoY', 'epsGrowthYoY', 'fcfGrowthYoY'],
    },
    {
      name: 'FINANCIAL_HEALTH',
      label: 'Financial Health',
      metrics: ['debtToEquity', 'currentRatio'],
    },
    {
      name: 'SIZE',
      label: 'Size',
      metrics: ['marketCap'],
    },
  ];

  readonly metricLabels: Record<string, string> = {
    peRatio: 'P/E Ratio',
    pegRatio: 'PEG Ratio',
    pbRatio: 'P/B Ratio',
    dividendYield: 'Dividend Yield',
    roe: 'ROE',
    roa: 'ROA',
    grossMargin: 'Gross Margin',
    operatingMargin: 'Operating Margin',
    netMargin: 'Net Margin',
    revenueGrowthYoY: 'Revenue Growth YoY',
    epsGrowthYoY: 'EPS Growth YoY',
    fcfGrowthYoY: 'FCF Growth YoY',
    debtToEquity: 'Debt to Equity',
    currentRatio: 'Current Ratio',
    marketCap: 'Market Cap',
  };

  ngOnInit(): void {
    // Initialize empty
  }

  addTicker(value: string): void {
    const ticker = value.trim().toUpperCase();
    if (!ticker) return;

    if (this.tickers().length >= 5) {
      this.snackBar.open('Maximum 5 tickers allowed for comparison.', 'Close', {
        duration: 3000,
      });
      return;
    }

    if (this.tickers().includes(ticker)) {
      this.snackBar.open(`"${ticker}" is already added.`, 'Close', {
        duration: 2000,
      });
      return;
    }

    this.tickers.update((t) => [...t, ticker]);
    this.tickerInput.set('');
  }

  removeTicker(ticker: string): void {
    this.tickers.update((t) => t.filter((v) => v !== ticker));
  }

  onTickerInputKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      event.preventDefault();
      this.addTicker(this.tickerInput());
    }
  }

  compare(): void {
    if (this.tickers().length < 2) return;

    this.loading.set(true);
    this.error.set(null);
    this.response.set(null);

    this.comparisonService
      .compare(this.tickers())
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (res) => this.response.set(res),
        error: (err) => {
          this.error.set(
            err?.error?.message ?? 'Comparison failed. Please try again.'
          );
        },
      });
  }

  getMetricValue(company: CompanyComparisonRow, metric: string): number | null {
    return company.metrics[metric] ?? null;
  }

  getIndustryAvg(metric: string): number | null {
    const ia = this.response()?.industryAverages;
    if (!ia?.metrics) return null;
    return ia.metrics[metric] ?? null;
  }

  getRowBestValue(companies: CompanyComparisonRow[], metric: string): number | null {
    const values = companies
      .map((c) => this.getMetricValue(c, metric))
      .filter((v): v is number => v !== null && !isNaN(v));

    if (values.length === 0) return null;

    // For these metrics, lower is better
    const lowerIsBetter = ['peRatio', 'pegRatio', 'pbRatio', 'debtToEquity'];
    if (lowerIsBetter.includes(metric)) {
      return Math.min(...values);
    }
    return Math.max(...values);
  }

  getRowWorstValue(companies: CompanyComparisonRow[], metric: string): number | null {
    const values = companies
      .map((c) => this.getMetricValue(c, metric))
      .filter((v): v is number => v !== null && !isNaN(v));

    if (values.length === 0) return null;

    const lowerIsBetter = ['peRatio', 'pegRatio', 'pbRatio', 'debtToEquity'];
    if (lowerIsBetter.includes(metric)) {
      return Math.max(...values);
    }
    return Math.min(...values);
  }

  getCellClass(
    companies: CompanyComparisonRow[],
    company: CompanyComparisonRow,
    metric: string
  ): string {
    const value = this.getMetricValue(company, metric);
    if (value == null) return '';

    const best = this.getRowBestValue(companies, metric);
    const worst = this.getRowWorstValue(companies, metric);

    if (best !== null && worst !== null && best !== worst) {
      if (value === best) return 'cell-best';
      if (value === worst) return 'cell-worst';
    }
    return '';
  }

  formatMetricValue(metric: string, value: number | null): string {
    if (value == null || isNaN(value)) return 'N/A';

    switch (metric) {
      case 'marketCap':
        if (value >= 1e12) return `$${(value / 1e12).toFixed(2)}T`;
        if (value >= 1e9) return `$${(value / 1e9).toFixed(2)}B`;
        if (value >= 1e6) return `$${(value / 1e6).toFixed(2)}M`;
        return `$${value.toFixed(2)}`;
      case 'peRatio':
      case 'pegRatio':
      case 'pbRatio':
      case 'debtToEquity':
      case 'currentRatio':
        return value.toFixed(2);
      case 'roe':
      case 'roa':
      case 'grossMargin':
      case 'operatingMargin':
      case 'netMargin':
      case 'revenueGrowthYoY':
      case 'epsGrowthYoY':
      case 'fcfGrowthYoY':
      case 'dividendYield':
        return `${value.toFixed(2)}%`;
      default:
        return value.toFixed(2);
    }
  }

  getTickerColor(idx: number): string {
    const colors = ['#3b82f6', '#22c55e', '#f59e0b', '#ef4444', '#8b5cf6'];
    return colors[idx % colors.length];
  }
}
