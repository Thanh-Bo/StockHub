import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of, forkJoin } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { environment } from '@env/environment';
import { CompanyCardComponent } from '@app/shared/components/company-card/company-card.component';
import { LoadingSpinnerComponent } from '@app/shared/components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state.component';
import { AutocompleteResult } from '@app/shared/models/shared.models';

interface SearchResult {
  ticker: string;
  name: string;
  sector: string;
  marketCap: number;
  price: number;
  priceChangePercent: number;
  exchange?: string;
}

@Component({
  selector: 'app-search-results',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    CompanyCardComponent,
    LoadingSpinnerComponent,
    EmptyStateComponent,
  ],
  templateUrl: './search-results.component.html',
  styleUrls: ['./search-results.component.scss'],
})
export class SearchResultsComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api/v1`;

  results: SearchResult[] = [];
  trendingResults: SearchResult[] = [];
  loading = false;
  error: string | null = null;
  query = '';

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      const q = params['q']?.trim();
      this.query = q || '';

      if (this.query) {
        this.search(this.query);
      } else {
        this.loadTrending();
      }
    });
  }

  private search(q: string): void {
    this.loading = true;
    this.error = null;
    this.results = [];

    const params = new HttpParams().set('q', q).set('limit', '20');

    this.http
      .get<SearchResult[]>(`${this.apiUrl}/search`, { params })
      .pipe(
        catchError((err) => {
          this.error =
            err?.error?.message ?? err?.message ?? 'Search failed. Please try again.';
          return of([]);
        })
      )
      .subscribe((data) => {
        this.results = data;
        this.loading = false;
      });
  }

  loadTrending(): void {
    this.loading = true;
    this.error = null;

    this.http
      .get<SearchResult[]>(`${this.apiUrl}/search/trending`)
      .pipe(
        catchError((err) => {
          this.error =
            err?.error?.message ?? err?.message ?? 'Failed to load trending searches.';
          return of([]);
        })
      )
      .subscribe((data) => {
        this.trendingResults = data;
        this.loading = false;
      });
  }

  retry(): void {
    if (this.query) {
      this.search(this.query);
    } else {
      this.loadTrending();
    }
  }
}
