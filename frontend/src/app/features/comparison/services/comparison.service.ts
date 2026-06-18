import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { ComparisonResponse } from '../models/comparison.models';

@Injectable({ providedIn: 'root' })
export class ComparisonService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api/v1/companies`;

  compare(tickers: string[]): Observable<ComparisonResponse> {
    const params = new HttpParams().set('tickers', tickers.join(','));
    return this.http.get<ComparisonResponse>(`${this.apiUrl}/compare`, { params });
  }
}
