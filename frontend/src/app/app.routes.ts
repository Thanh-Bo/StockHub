import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'search',
    pathMatch: 'full',
  },
  {
    path: 'stocks/:ticker',
    loadComponent: () =>
      import('./features/dashboard/company-dashboard/company-dashboard.component').then(
        (m) => m.CompanyDashboardComponent
      ),
  },
  {
    path: 'stocks/:ticker/financials',
    loadComponent: () =>
      import('./features/financials/financial-statements/financial-statements.component').then(
        (m) => m.FinancialStatementsComponent
      ),
  },
  {
    path: 'screener',
    loadComponent: () =>
      import('./features/screener/stock-screener/stock-screener.component').then(
        (m) => m.StockScreenerComponent
      ),
  },
  {
    path: 'compare',
    loadComponent: () =>
      import('./features/comparison/company-comparison/company-comparison.component').then(
        (m) => m.CompanyComparisonComponent
      ),
  },
  {
    path: 'search',
    loadComponent: () =>
      import('./features/search/search-results.component').then(
        (m) => m.SearchResultsComponent
      ),
  },
  {
    path: 'auth/login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then(
        (m) => m.LoginComponent
      ),
  },
  {
    path: 'auth/register',
    loadComponent: () =>
      import('./features/auth/register/register.component').then(
        (m) => m.RegisterComponent
      ),
  },
  {
    path: 'watchlists',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/watchlist/watchlist-list/watchlist-list.component').then(
        (m) => m.WatchlistListComponent
      ),
  },
  {
    path: 'watchlists/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/watchlist/watchlist-detail/watchlist-detail.component').then(
        (m) => m.WatchlistDetailComponent
      ),
  },
  {
    path: '**',
    redirectTo: 'search',
  },
];
