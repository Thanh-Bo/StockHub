import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { Subject, takeUntil } from 'rxjs';
import { FinancialsService } from '../services/financials.service';
import { FinancialStatementRow } from '../../dashboard/models/dashboard.models';
import { DataTableComponent } from '@app/shared/components/data-table/data-table.component';
import { LoadingSpinnerComponent } from '@app/shared/components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state.component';
import { ColumnDef, PagedResponse } from '@app/shared/models/shared.models';

type StatementType = 'income' | 'balance' | 'cashflow';
type PeriodType = 'ANNUAL' | 'QUARTERLY';

@Component({
  selector: 'app-financial-statements',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonToggleModule,
    MatSelectModule,
    MatFormFieldModule,
    DataTableComponent,
    LoadingSpinnerComponent,
    EmptyStateComponent,
  ],
  templateUrl: './financial-statements.component.html',
  styleUrls: ['./financial-statements.component.scss'],
})
export class FinancialStatementsComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly financialsService = inject(FinancialsService);

  private readonly destroy$ = new Subject<void>();

  ticker = '';

  statementType: StatementType = 'income';
  period: PeriodType = 'ANNUAL';
  selectedYears = 5;

  data: FinancialStatementRow[] = [];
  columns: ColumnDef[] = [];
  loading = false;
  error: string | null = null;

  private currentPage = 0;
  private readonly pageSize = 10;

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const ticker = params.get('ticker') ?? '';
      if (ticker) {
        this.ticker = ticker;
        this.loadData();
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onStatementTypeChange(type: StatementType): void {
    this.statementType = type;
    this.currentPage = 0;
    this.loadData();
  }

  onPeriodChange(period: PeriodType): void {
    this.period = period;
    this.currentPage = 0;
    this.loadData();
  }

  onYearsChange(years: number): void {
    this.selectedYears = years;
    this.currentPage = 0;
    this.loadData();
  }

  retry(): void {
    this.loadData();
  }

  private loadData(): void {
    this.loading = true;
    this.error = null;
    this.data = [];

    const request$ = this.getStatementRequest();

    request$.pipe(takeUntil(this.destroy$)).subscribe({
      next: (response) => {
        const content =
          Array.isArray(response) ? response : (response as PagedResponse<FinancialStatementRow>).content;
        this.data = content;
        this.loading = false;

        if (content.length > 0) {
          this.buildColumns(content[0]);
        }
      },
      error: (err) => {
        this.error =
          err?.error?.message ?? err?.message ?? 'Failed to load financial statements.';
        this.loading = false;
      },
    });
  }

  private getStatementRequest() {
    const ticker = this.ticker;
    switch (this.statementType) {
      case 'income':
        return this.financialsService.getIncomeStatements(
          ticker,
          this.period,
          this.selectedYears,
          this.currentPage,
          this.pageSize
        );
      case 'balance':
        return this.financialsService.getBalanceSheets(
          ticker,
          this.period,
          this.selectedYears,
          this.currentPage,
          this.pageSize
        );
      case 'cashflow':
        return this.financialsService.getCashFlowStatements(
          ticker,
          this.period,
          this.selectedYears,
          this.currentPage,
          this.pageSize
        );
    }
  }

  private buildColumns(firstRow: FinancialStatementRow): void {
    const metricKeys = Object.keys(firstRow).filter((k) => k !== 'fiscalYear');

    this.columns = metricKeys.map((key) => ({
      field: key,
      header: this.formatHeader(key),
      format: this.inferFormat(key),
      align: 'right' as const,
    }));

    this.columns.unshift({
      field: 'fiscalYear',
      header: 'Fiscal Year',
      format: undefined,
      align: 'left' as const,
    });
  }

  private formatHeader(key: string): string {
    return key
      .replace(/([A-Z])/g, ' $1')
      .replace(/^./, (s) => s.toUpperCase())
      .replace(/\bYoy\b/i, '(YoY)')
      .trim();
  }

  private inferFormat(key: string): string | undefined {
    const lower = key.toLowerCase();
    if (
      lower.includes('margin') ||
      lower.includes('growth') ||
      lower.includes('yoy') ||
      lower.includes('rate') ||
      lower.includes('taxpct') ||
      lower.includes('percent')
    ) {
      return 'percent';
    }
    if (
      lower.includes('revenue') ||
      lower.includes('income') ||
      lower.includes('ebitda') ||
      lower.includes('ebit') ||
      lower.includes('profit') ||
      lower.includes('expense') ||
      lower.includes('asset') ||
      lower.includes('liability') ||
      lower.includes('equity') ||
      lower.includes('debt') ||
      lower.includes('cash') ||
      lower.includes('flow')
    ) {
      return 'bigNumber';
    }
    if (
      lower.includes('eps') ||
      lower.includes('ratio') ||
      lower.includes('share')
    ) {
      return 'decimal';
    }
    return 'decimal';
  }
}
