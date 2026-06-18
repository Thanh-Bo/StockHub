import {
  signalStore,
  withState,
  withComputed,
  withMethods,
  patchState,
} from '@ngrx/signals';
import { computed, inject } from '@angular/core';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe, switchMap, tap, catchError, of } from 'rxjs';
import { DashboardService } from '../services/dashboard.service';
import { DashboardData } from '../models/dashboard.models';
import { MetricDefinition } from '@app/shared/models/shared.models';

interface DashboardState {
  ticker: string;
  data: DashboardData | null;
  loading: boolean;
  error: string | null;
  priceRange: string;
  priceHistoryLoading: boolean;
}

const initialState: DashboardState = {
  ticker: '',
  data: null,
  loading: false,
  error: null,
  priceRange: '1M',
  priceHistoryLoading: false,
};

export const DashboardStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),

  withComputed(({ data, ticker }) => ({
    priceChangeColor: computed(() => {
      const d = data();
      if (!d) return '#6b7280';
      return d.priceChangePercent >= 0 ? '#22c55e' : '#ef4444';
    }),

    keyMetrics: computed((): MetricDefinition[] => {
      const d = data();
      if (!d) return [];
      return [
        {
          label: 'Market Cap',
          value: d.marketCap,
          format: 'bigNumber',
          tooltip: 'Total market capitalization',
          trend: d.marketCap > 0 ? 'up' : 'neutral',
        },
        {
          label: 'P/E Ratio',
          value: d.peRatio,
          format: 'decimal',
          tooltip: 'Price-to-Earnings ratio',
          trend: d.peRatio < d.industryContext.avgPE ? 'up' : 'down',
        },
        {
          label: 'Revenue Growth (YoY)',
          value: d.revenueGrowthYoY,
          format: 'percent',
          tooltip: 'Year-over-year revenue growth',
          trend: d.revenueGrowthYoY > 0 ? 'up' : 'down',
        },
        {
          label: 'ROE',
          value: d.roe,
          format: 'percent',
          tooltip: 'Return on Equity',
          trend: d.roe > d.industryContext.avgROE ? 'up' : 'down',
        },
        {
          label: 'Net Margin',
          value: d.netMargin,
          format: 'percent',
          tooltip: 'Net profit margin',
          trend: d.netMargin > d.industryContext.avgNetMargin ? 'up' : 'down',
        },
        {
          label: 'Debt to Equity',
          value: d.debtToEquity,
          format: 'decimal',
          tooltip: 'Debt-to-Equity ratio',
          trend: d.debtToEquity < 1 ? 'up' : 'down',
        },
        {
          label: 'Dividend Yield',
          value: d.dividendYield,
          format: 'percent',
          tooltip: 'Annual dividend yield',
          trend: d.dividendYield > 0 ? 'up' : 'neutral',
        },
        {
          label: 'Gross Margin',
          value: d.grossMargin,
          format: 'percent',
          tooltip: 'Gross profit margin',
          trend: d.grossMargin > 50 ? 'up' : 'down',
        },
      ];
    }),

    name: computed(() => data()?.name ?? ''),
    description: computed(() => data()?.description ?? ''),
    sector: computed(() => data()?.sector ?? ''),
    industry: computed(() => data()?.industry ?? ''),
    currentPrice: computed(() => data()?.currentPrice ?? 0),
    priceChange: computed(() => data()?.priceChange ?? 0),
    priceChangePercent: computed(() => data()?.priceChangePercent ?? 0),
    priceHistory: computed(() => data()?.priceHistory ?? []),
    headquarters: computed(() => data()?.headquarters ?? ''),
    employees: computed(() => data()?.employees ?? 0),
    dayHigh: computed(() => data()?.dayHigh ?? 0),
    dayLow: computed(() => data()?.dayLow ?? 0),
    previousClose: computed(() => data()?.previousClose ?? 0),
    volume: computed(() => data()?.volume ?? 0),
    lastUpdated: computed(() => data()?.lastUpdated ?? ''),
    dataSource: computed(() => data()?.dataSource ?? ''),
    industryContext: computed(() => data()?.industryContext ?? null),
  })),

  withMethods((store, dashboardService = inject(DashboardService)) => ({
    loadDashboard: rxMethod<string>(
      pipe(
        tap((ticker) =>
          patchState(store, {
            ticker,
            loading: true,
            error: null,
            data: null,
          })
        ),
        switchMap((ticker) =>
          dashboardService.getDashboard(ticker).pipe(
            tap((data) =>
              patchState(store, {
                data,
                loading: false,
                error: null,
              })
            ),
            catchError((err) => {
              const message =
                err?.error?.message ??
                err?.message ??
                'Failed to load dashboard';
              patchState(store, {
                loading: false,
                error: message,
              });
              return of(null);
            })
          )
        )
      )
    ),

    refresh(): void {
      const currentTicker = store.ticker();
      if (currentTicker) {
        this.loadDashboard(currentTicker);
      }
    },

    setPriceRange(range: string): void {
      patchState(store, { priceRange: range });
    },
  }))
);
