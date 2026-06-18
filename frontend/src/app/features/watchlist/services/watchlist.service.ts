import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import {
  WatchlistSummary,
  WatchlistDetail,
  WatchlistResponse,
  CreateWatchlistRequest,
  UpdateWatchlistRequest,
} from '../models/watchlist.models';

@Injectable({ providedIn: 'root' })
export class WatchlistService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api/v1/watchlists`;

  getWatchlists(): Observable<WatchlistSummary[]> {
    return this.http.get<WatchlistSummary[]>(this.apiUrl);
  }

  getWatchlistDetail(id: number): Observable<WatchlistDetail> {
    return this.http.get<WatchlistDetail>(`${this.apiUrl}/${id}`);
  }

  createWatchlist(request: CreateWatchlistRequest): Observable<WatchlistResponse> {
    return this.http.post<WatchlistResponse>(this.apiUrl, request);
  }

  updateWatchlist(id: number, request: UpdateWatchlistRequest): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}`, request);
  }

  deleteWatchlist(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  addStock(id: number, ticker: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${id}/stocks`, { ticker });
  }

  removeStock(id: number, ticker: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}/stocks/${ticker}`);
  }

  reorderStocks(id: number, tickers: string[]): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}/stocks/reorder`, { tickers });
  }
}
