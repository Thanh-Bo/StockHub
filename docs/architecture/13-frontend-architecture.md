# Section 13: Frontend Architecture

## 13.1 Technology Stack

| Technology | Version | Purpose |
|-----------|---------|---------|
| Angular | 19 | SPA Framework |
| Angular Material | 19 | UI Component Library |
| NgRx Signal Store | 19 | State Management |
| Chart.js + ng2-charts | 4.x / 5.x | Interactive Charts |
| TypeScript | 5.5+ | Type-Safe JavaScript |
| RxJS | 7.x | Reactive Programming |
| Angular CLI | 19 | Build Tooling |

---

## 13.2 Project Folder Structure

```
src/
├── app/
│   ├── core/                        # Singleton services, guards, interceptors
│   │   ├── guards/
│   │   │   ├── auth.guard.ts
│   │   │   └── premium.guard.ts
│   │   ├── interceptors/
│   │   │   ├── auth.interceptor.ts       # Attach JWT to requests
│   │   │   ├── error.interceptor.ts      # Global error handling
│   │   │   └── cache.interceptor.ts      # HTTP caching headers
│   │   ├── services/
│   │   │   ├── auth.service.ts
│   │   │   ├── http-client.service.ts    # Wrapper around HttpClient
│   │   │   └── analytics.service.ts
│   │   └── models/
│   │       ├── user.model.ts
│   │       └── pagination.model.ts
│   │
│   ├── shared/                      # Reusable components, pipes, directives
│   │   ├── components/
│   │   │   ├── metric-card/
│   │   │   │   ├── metric-card.component.ts
│   │   │   │   ├── metric-card.component.html
│   │   │   │   └── metric-card.component.scss
│   │   │   ├── data-table/
│   │   │   │   ├── data-table.component.ts
│   │   │   │   ├── data-table.component.html
│   │   │   │   └── data-table.component.scss
│   │   │   ├── price-chart/
│   │   │   │   ├── price-chart.component.ts
│   │   │   │   ├── price-chart.component.html
│   │   │   │   └── price-chart.component.scss
│   │   │   ├── company-card/
│   │   │   ├── filter-bar/
│   │   │   ├── search-bar/
│   │   │   ├── range-slider/
│   │   │   ├── growth-chart/
│   │   │   ├── comparison-table/
│   │   │   └── loading-spinner/
│   │   ├── pipes/
│   │   │   ├── big-number.pipe.ts        # $2.8T, $150B, $50M
│   │   │   ├── percent.pipe.ts           # 15.2%
│   │   │   └── ticker-format.pipe.ts     # AAPL → uppercase
│   │   └── directives/
│   │       └── highlight-threshold.directive.ts
│   │
│   ├── features/                    # Feature modules (lazy-loaded)
│   │   ├── auth/
│   │   │   ├── pages/
│   │   │   │   ├── login/
│   │   │   │   └── register/
│   │   │   ├── store/
│   │   │   │   └── auth.store.ts         # NgRx Signal Store
│   │   │   ├── auth.routes.ts
│   │   │   └── auth.module.ts
│   │   │
│   │   ├── dashboard/
│   │   │   ├── pages/
│   │   │   │   └── company-dashboard/
│   │   │   │       ├── company-dashboard.component.ts
│   │   │   │       ├── company-dashboard.component.html
│   │   │   │       └── company-dashboard.component.scss
│   │   │   ├── components/
│   │   │   │   ├── company-header/
│   │   │   │   ├── metrics-grid/
│   │   │   │   ├── revenue-chart/
│   │   │   │   ├── financial-tabs/
│   │   │   │   └── peer-comparison-widget/
│   │   │   ├── services/
│   │   │   │   └── dashboard.service.ts
│   │   │   ├── store/
│   │   │   │   └── dashboard.store.ts
│   │   │   ├── models/
│   │   │   │   └── dashboard.models.ts
│   │   │   ├── dashboard.routes.ts
│   │   │   └── dashboard.module.ts
│   │   │
│   │   ├── financials/
│   │   │   ├── pages/
│   │   │   │   └── financial-statements/
│   │   │   ├── components/
│   │   │   │   ├── income-statement-table/
│   │   │   │   ├── balance-sheet-table/
│   │   │   │   └── cash-flow-table/
│   │   │   ├── services/
│   │   │   │   └── financials.service.ts
│   │   │   ├── store/
│   │   │   │   └── financials.store.ts
│   │   │   └── financials.routes.ts
│   │   │
│   │   ├── screener/
│   │   │   ├── pages/
│   │   │   │   └── stock-screener/
│   │   │   ├── components/
│   │   │   │   ├── filter-panel/
│   │   │   │   ├── results-table/
│   │   │   │   └── save-screener-dialog/
│   │   │   ├── services/
│   │   │   │   └── screener.service.ts
│   │   │   ├── store/
│   │   │   │   └── screener.store.ts
│   │   │   └── screener.routes.ts
│   │   │
│   │   ├── comparison/
│   │   │   ├── pages/
│   │   │   │   └── company-comparison/
│   │   │   ├── components/
│   │   │   │   ├── comparison-chart/
│   │   │   │   └── metric-row/
│   │   │   ├── services/
│   │   │   │   └── comparison.service.ts
│   │   │   └── comparison.routes.ts
│   │   │
│   │   ├── watchlist/
│   │   │   ├── pages/
│   │   │   │   ├── watchlist-list/
│   │   │   │   └── watchlist-detail/
│   │   │   ├── components/
│   │   │   │   ├── watchlist-card/
│   │   │   │   ├── add-stock-dialog/
│   │   │   │   └── watchlist-summary/
│   │   │   ├── services/
│   │   │   │   └── watchlist.service.ts
│   │   │   ├── store/
│   │   │   │   └── watchlist.store.ts
│   │   │   └── watchlist.routes.ts
│   │   │
│   │   └── search/
│   │       ├── pages/
│   │       │   └── search-results/
│   │       ├── components/
│   │       │   └── global-search-bar/
│   │       ├── services/
│   │       │   └── search.service.ts
│   │       └── search.routes.ts
│   │
│   ├── app.component.ts
│   ├── app.component.html
│   ├── app.component.scss
│   ├── app.config.ts              # Standalone bootstrap config
│   └── app.routes.ts              # Top-level route configuration
│
├── assets/
│   ├── images/
│   └── icons/
│
├── environments/
│   ├── environment.ts              # Development
│   └── environment.prod.ts         # Production
│
└── styles/
    ├── _variables.scss             # Theme colors, spacing
    ├── _typography.scss
    └── styles.scss                 # Global styles
```

---

## 13.3 Routing Configuration

```typescript
// app.routes.ts
export const routes: Routes = [
  // Public routes
  {
    path: '',
    loadComponent: () => import('./features/search/pages/search-results/search-results.component')
  },
  {
    path: 'stocks/:ticker',
    loadChildren: () => import('./features/dashboard/dashboard.routes')
  },
  {
    path: 'stocks/:ticker/financials',
    loadChildren: () => import('./features/financials/financials.routes')
  },
  {
    path: 'screener',
    loadChildren: () => import('./features/screener/screener.routes')
  },
  {
    path: 'compare',
    loadChildren: () => import('./features/comparison/comparison.routes')
  },
  {
    path: 'search',
    loadChildren: () => import('./features/search/search.routes')
  },

  // Auth routes (lazy)
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes')
  },

  // Protected routes
  {
    path: 'watchlists',
    loadChildren: () => import('./features/watchlist/watchlist.routes'),
    canActivate: [authGuard]
  },

  // Fallback
  { path: '**', redirectTo: '' }
];
```

---

## 13.4 State Management (NgRx Signal Store)

### Architecture Decision: NgRx Signal Store over traditional NgRx

| Aspect | NgRx Signal Store | NgRx (Store + Effects) |
|--------|-------------------|------------------------|
| Boilerplate | Minimal | Heavy (actions, reducers, effects, selectors) |
| Angular integration | Native signals, `computed()`, `effect()` | RxJS-based |
| Learning curve | Low | High |
| Type safety | Excellent | Good |
| DevTools | Limited | Full Redux DevTools |
| Suitable for | Medium-scale apps | Enterprise-scale apps |

**Decision**: NgRx Signal Store for this project. Less boilerplate, modern Angular patterns, sufficient for portfolio-scale.

### Store Examples

```typescript
// dashboard.store.ts
import { signalStore, withState, withMethods, withComputed } from '@ngrx/signals';
import { computed, inject } from '@angular/core';
import { DashboardService } from '../services/dashboard.service';

interface DashboardState {
  ticker: string;
  data: DashboardResponse | null;
  loading: boolean;
  error: string | null;
}

const initialState: DashboardState = {
  ticker: '',
  data: null,
  loading: false,
  error: null,
};

export const DashboardStore = signalStore(
  withState(initialState),

  withComputed(({ data }) => ({
    priceChangeColor: computed(() => {
      const d = data();
      if (!d) return 'inherit';
      return d.priceChangePercent >= 0 ? '#22c55e' : '#ef4444';
    }),
    keyMetrics: computed(() => {
      const d = data();
      if (!d) return [];
      return [
        { label: 'Market Cap', value: d.marketCap, format: 'bigNumber' },
        { label: 'P/E Ratio', value: d.peRatio, format: 'decimal' },
        { label: 'Revenue Growth', value: d.revenueGrowthYoY, format: 'percent' },
        { label: 'ROE', value: d.roe, format: 'percent' },
        { label: 'Net Margin', value: d.netMargin, format: 'percent' },
        { label: 'D/E Ratio', value: d.debtToEquity, format: 'decimal' },
      ];
    }),
  })),

  withMethods((store, dashboardService = inject(DashboardService)) => ({
    async loadDashboard(ticker: string) {
      patchState(store, { ticker, loading: true, error: null });
      try {
        const data = await firstValueFrom(dashboardService.getDashboard(ticker));
        patchState(store, { data, loading: false });
      } catch (err) {
        patchState(store, { error: 'Failed to load dashboard', loading: false });
      }
    },
    async refresh() {
      return this.loadDashboard(store.ticker());
    },
  }))
);
```

```typescript
// screener.store.ts
export const ScreenerStore = signalStore(
  withState({
    filters: [] as FilterCriteria[],
    sort: { field: 'marketCap', direction: 'DESC' } as SortCriteria,
    results: null as ScreenerResponse | null,
    loading: false,
    page: 0,
    pageSize: 25,
  }),

  withMethods((store, screenerService = inject(ScreenerService)) => ({
    addFilter(filter: FilterCriteria) {
      patchState(store, {
        filters: [...store.filters(), filter],
        page: 0, // Reset page on filter change
      });
    },
    removeFilter(index: number) {
      const updated = [...store.filters()];
      updated.splice(index, 1);
      patchState(store, { filters: updated, page: 0 });
    },
    setSort(sort: SortCriteria) {
      patchState(store, { sort, page: 0 });
    },
    async search() {
      patchState(store, { loading: true });
      try {
        const results = await firstValueFrom(screenerService.search({
          filters: store.filters(),
          sort: store.sort(),
          pagination: { page: store.page(), size: store.pageSize() },
        }));
        patchState(store, { results, loading: false });
      } catch {
        patchState(store, { loading: false });
      }
    },
    nextPage() {
      patchState(store, { page: store.page() + 1 });
      this.search();
    },
    previousPage() {
      patchState(store, { page: Math.max(0, store.page() - 1) });
      this.search();
    },
  }))
);
```

---

## 13.5 Services Pattern

```typescript
// dashboard.service.ts
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private http = inject(HttpClient);
  private cache = inject(HttpCacheService);

  getDashboard(ticker: string): Observable<DashboardResponse> {
    return this.http.get<DashboardResponse>(
      `${environment.apiUrl}/api/v1/companies/${ticker}/dashboard`,
      { context: this.cache.withTTL(ticker, 300_000) } // 5-min client cache
    );
  }

  getFinancialStatements(ticker: string, period: string, years: number):
      Observable<FinancialStatementResponse> {
    return this.http.get<FinancialStatementResponse>(
      `${environment.apiUrl}/api/v1/companies/${ticker}/income-statements`,
      { params: { period, years } }
    );
  }

  getPriceHistory(ticker: string, range: string = '1Y'):
      Observable<PriceHistoryResponse> {
    return this.http.get<PriceHistoryResponse>(
      `${environment.apiUrl}/api/v1/companies/${ticker}/prices`,
      { params: { range } }
    );
  }
}
```

---

## 13.6 HTTP Interceptors

```typescript
// auth.interceptor.ts
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private authService = inject(AuthService);

  intercept(req: HttpRequest<unknown>, next: HttpHandlerFn): Observable<HttpEvent<unknown>> {
    const token = this.authService.getAccessToken();
    if (token && req.url.includes(environment.apiUrl)) {
      req = req.clone({
        setHeaders: { Authorization: `Bearer ${token}` },
      });
    }
    return next(req);
  }
}

// error.interceptor.ts
@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  private snackBar = inject(MatSnackBar);
  private router = inject(Router);

  intercept(req: HttpRequest<unknown>, next: HttpHandlerFn): Observable<HttpEvent<unknown>> {
    return next(req).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          this.router.navigate(['/auth/login']);
        } else if (error.status === 429) {
          this.snackBar.open('Too many requests. Please slow down.', 'Dismiss', { duration: 5000 });
        } else if (error.status >= 500) {
          this.snackBar.open('Server error. Please try again later.', 'Dismiss', { duration: 5000 });
        }
        return throwError(() => error);
      })
    );
  }
}
```

---

## 13.7 Route Guards

```typescript
// auth.guard.ts
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree(['/auth/login'], {
    queryParams: { returnUrl: state.url },
  });
};

// premium.guard.ts
export const premiumGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.hasRole('PREMIUM') || authService.hasRole('ADMIN')) {
    return true;
  }

  return router.createUrlTree(['/pricing']);
};
```

---

## 13.8 Build Configuration

```json
// angular.json (excerpt)
{
  "projects": {
    "stockhub": {
      "architect": {
        "build": {
          "configurations": {
            "production": {
              "budgets": [
                { "type": "initial", "maximumWarning": "500kb", "maximumError": "1mb" },
                { "type": "anyComponentStyle", "maximumWarning": "2kb", "maximumError": "4kb" }
              ],
              "optimization": true,
              "outputHashing": "all",
              "sourceMap": false,
              "namedChunks": false,
              "aot": true,
              "extractLicenses": true,
              "vendorChunk": false,
              "buildOptimizer": true
            }
          }
        }
      }
    }
  }
}
```
