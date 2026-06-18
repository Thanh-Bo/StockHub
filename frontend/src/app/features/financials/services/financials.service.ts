import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { PagedResponse } from '@app/shared/models/shared.models';
import { FinancialStatementRow } from '../../dashboard/models/dashboard.models';

@Injectable({ providedIn: 'root' })
export class FinancialsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api/v1`;

  getIncomeStatements(
    ticker: string,
    period: string,
    years: number,
    page: number = 0,
    size: number = 10
  ): Observable<PagedResponse<FinancialStatementRow>> {
    const params = new HttpParams()
      .set('period', period)
      .set('years', years.toString())
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PagedResponse<FinancialStatementRow>>(
      `${this.apiUrl}/companies/${encodeURIComponent(ticker)}/income-statements`,
      { params }
    );
  }

  getBalanceSheets(
    ticker: string,
    period: string,
    years: number,
    page: number = 0,
    size: number = 10
  ): Observable<PagedResponse<FinancialStatementRow>> {
    const params = new HttpParams()
      .set('period', period)
      .set('years', years.toString())
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PagedResponse<FinancialStatementRow>>(
      `${this.apiUrl}/companies/${encodeURIComponent(ticker)}/balance-sheets`,
      { params }
    );
  }

  getCashFlowStatements(
    ticker: string,
    period: string,
    years: number,
    page: number = 0,
    size: number = 10
  ): Observable<PagedResponse<FinancialStatementRow>> {
    const params = new HttpParams()
      .set('period', period)
      .set('years', years.toString())
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PagedResponse<FinancialStatementRow>>(
      `${this.apiUrl}/companies/${encodeURIComponent(ticker)}/cash-flows`,
      { params }
    );
  }
}
