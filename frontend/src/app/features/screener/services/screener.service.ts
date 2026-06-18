import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { ScreenerRequest, ScreenerResponse, FilterMetadata } from '../models/screener.models';

@Injectable({ providedIn: 'root' })
export class ScreenerService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api/v1/screener`;

  search(request: ScreenerRequest): Observable<ScreenerResponse> {
    return this.http.post<ScreenerResponse>(`${this.apiUrl}/search`, request);
  }

  getFilters(): Observable<FilterMetadata[]> {
    return this.http.get<FilterMetadata[]>(`${this.apiUrl}/filters`);
  }
}
