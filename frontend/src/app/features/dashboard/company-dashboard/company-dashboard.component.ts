import { Component, inject, OnInit, OnDestroy, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTabsModule } from '@angular/material/tabs';
import { MatDividerModule } from '@angular/material/divider';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { FormsModule } from '@angular/forms';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Subject, takeUntil } from 'rxjs';
import { DashboardStore } from '../store/dashboard.store';
import { DashboardService } from '../services/dashboard.service';
import { FinancialStatementRow, PeerComparisonRow } from '../models/dashboard.models';
import { MetricCardComponent } from '@app/shared/components/metric-card/metric-card.component';
import { PriceChartComponent } from '@app/shared/components/price-chart/price-chart.component';
import { DataTableComponent } from '@app/shared/components/data-table/data-table.component';
import { LoadingSpinnerComponent } from '@app/shared/components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state.component';
import { BigNumberPipe } from '@app/shared/pipes/big-number.pipe';
import { StockPercentPipe } from '@app/shared/pipes/percent.pipe';
import { ColumnDef } from '@app/shared/models/shared.models';

@Component({
  selector: 'app-company-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatTabsModule,
    MatDividerModule,
    MatButtonToggleModule,
    MatSelectModule,
    MatFormFieldModule,
    MatSnackBarModule,
    MetricCardComponent,
    PriceChartComponent,
    DataTableComponent,
    LoadingSpinnerComponent,
    EmptyStateComponent,
    BigNumberPipe,
    StockPercentPipe,
  ],
  templateUrl: './company-dashboard.component.html',
  styleUrls: ['./company-dashboard.component.scss'],
})
export class CompanyDashboardComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dashboardService = inject(DashboardService);
  readonly store = inject(DashboardStore);

  private readonly destroy$ = new Subject<void>();

  // Financials tab state
  period: 'ANNUAL' | 'QUARTERLY' = 'ANNUAL';
  selectedYears = 5;
  financialStatements: FinancialStatementRow[] = [];
  financialStatementsLoading = false;
  financialColumns: ColumnDef[] = [];

  // Peers tab
  peers: PeerComparisonRow[] = [];
  peersLoading = false;
  peerColumns: ColumnDef[] = [];

  // Tab index
  selectedTabIndex = 0;

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const ticker = params.get('ticker') ?? '';
      if (ticker) {
        this.store.loadDashboard(ticker);
        this.loadFinancialStatements(ticker);
        this.loadPeers(ticker);
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onPriceRangeChange(range: string): void {
    this.store.setPriceRange(range);
  }

  onPeriodChange(period: 'ANNUAL' | 'QUARTERLY'): void {
    this.period = period;
    const ticker = this.store.ticker();
    if (ticker) {
      this.loadFinancialStatements(ticker);
    }
  }

  onYearsChange(years: number): void {
    this.selectedYears = years;
    const ticker = this.store.ticker();
    if (ticker) {
      this.loadFinancialStatements(ticker);
    }
  }

  onTabChange(index: number): void {
    this.selectedTabIndex = index;
  }

  onAddToWatchlist(): void {
    this.snackBar.open('Added to watchlist', 'Close', { duration: 3000 });
  }

  retry(): void {
    const ticker = this.store.ticker();
    if (ticker) {
      this.store.loadDashboard(ticker);
      this.loadFinancialStatements(ticker);
      this.loadPeers(ticker);
    }
  }

  private loadFinancialStatements(ticker: string): void {
    this.financialStatementsLoading = true;
    this.dashboardService
      .getIncomeStatements(ticker, this.period, this.selectedYears)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.financialStatements = data;
          this.financialStatementsLoading = false;
          if (data.length > 0) {
            const firstRow = data[0];
            this.financialColumns = Object.keys(firstRow)
              .filter((k) => k !== 'fiscalYear')
              .map((key) => ({
                field: key,
                header: this.formatColumnHeader(key),
                format: this.getColumnFormat(key),
                align: 'right' as const,
              }));
            // Prepend fiscalYear column
            this.financialColumns.unshift({
              field: 'fiscalYear',
              header: 'Year',
              format: undefined,
              align: 'left' as const,
            });
          }
        },
        error: () => {
          this.financialStatements = [];
          this.financialStatementsLoading = false;
        },
      });
  }

  private loadPeers(ticker: string): void {
    this.peersLoading = true;
    this.dashboardService
      .getPeers(ticker)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.peers = data;
          this.peersLoading = false;
          if (data.length > 0) {
            const metricKeys = Object.keys(data[0].metrics);
            this.peerColumns = [
              { field: 'name', header: 'Company', align: 'left' as const },
              ...metricKeys.map((key) => ({
                field: `metrics.${key}`,
                header: this.formatColumnHeader(key),
                format: this.getColumnFormat(key),
                align: 'right' as const,
              })),
            ];
          }
        },
        error: () => {
          this.peers = [];
          this.peersLoading = false;
        },
      });
  }

  private formatColumnHeader(key: string): string {
    return key
      .replace(/([A-Z])/g, ' $1')
      .replace(/^./, (s) => s.toUpperCase())
      .trim();
  }

  private getColumnFormat(key: string): string | undefined {
    const lowerKey = key.toLowerCase();
    if (
      lowerKey.includes('margin') ||
      lowerKey.includes('growth') ||
      lowerKey.includes('roe') ||
      lowerKey.includes('roa') ||
      lowerKey.includes('yield') ||
      lowerKey.includes('percent')
    ) {
      return 'percent';
    }
    if (
      lowerKey.includes('marketcap') ||
      lowerKey.includes('revenue') ||
      lowerKey.includes('ebitda') ||
      lowerKey.includes('income')
    ) {
      return 'bigNumber';
    }
    return 'decimal';
  }
}
