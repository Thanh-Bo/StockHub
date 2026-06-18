import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { PricePoint } from '@app/shared/models/shared.models';
import { DashboardData, PeerComparisonRow } from '../models/dashboard.models';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api/v1`;

  getDashboard(ticker: string): Observable<DashboardData> {
    return this.http.get<DashboardData>(
      `${this.apiUrl}/companies/${encodeURIComponent(ticker)}/dashboard`
    );
  }

  getPriceHistory(ticker: string, range: string): Observable<PricePoint[]> {
    const params = new HttpParams().set('range', range);
    return this.http.get<PricePoint[]>(
      `${this.apiUrl}/companies/${encodeURIComponent(ticker)}/prices`,
      { params }
    );
  }

  getIncomeStatements(
    ticker: string,
    period: string,
    years: number
  ): Observable<any[]> {
    const params = new HttpParams()
      .set('period', period)
      .set('years', years.toString());
    return this.http.get<any[]>(
      `${this.apiUrl}/companies/${encodeURIComponent(ticker)}/income-statements`,
      { params }
    );
  }

  getMetrics(ticker: string): Observable<any> {
    return this.http.get<any>(
      `${this.apiUrl}/companies/${encodeURIComponent(ticker)}/metrics`
    );
  }

  getPeers(ticker: string): Observable<PeerComparisonRow[]> {
    return this.http.get<PeerComparisonRow[]>(
      `${this.apiUrl}/companies/${encodeURIComponent(ticker)}/peers`
    );
  }
}
