import {
  Component,
  Output,
  EventEmitter,
  OnDestroy,
  inject,
  OnInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormControl } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatOptionModule } from '@angular/material/core';
import { HttpClient } from '@angular/common/http';
import {
  debounceTime,
  distinctUntilChanged,
  filter,
  switchMap,
  takeUntil,
  Observable,
  of,
} from 'rxjs';
import { Subject } from 'rxjs';
import { environment } from '@env/environment';
import { AutocompleteResult } from '../../models/shared.models';

@Component({
  selector: 'app-search-bar',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
    MatAutocompleteModule,
    MatInputModule,
    MatIconModule,
    MatChipsModule,
    MatOptionModule,
  ],
  templateUrl: './search-bar.component.html',
  styleUrls: ['./search-bar.component.scss'],
})
export class SearchBarComponent implements OnInit, OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  searchControl = new FormControl<string>('');
  suggestions: AutocompleteResult[] = [];
  @Output() search = new EventEmitter<string>();

  private readonly destroy$ = new Subject<void>();
  private readonly apiUrl = environment.apiUrl;

  ngOnInit(): void {
    this.searchControl.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        filter((v): v is string => typeof v === 'string' && v.length >= 2),
        switchMap((query) => this.autocomplete(query)),
        takeUntil(this.destroy$)
      )
      .subscribe((results) => {
        this.suggestions = results;
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onOptionSelected(event: any): void {
    const ticker = event.option.value;
    if (ticker) {
      this.router.navigate(['/stocks', ticker]);
      this.searchControl.setValue('');
    }
  }

  onEnter(): void {
    const query = this.searchControl.value?.trim();
    if (query) {
      this.search.emit(query);
      this.router.navigate(['/search'], { queryParams: { q: query } });
      this.searchControl.setValue('');
    }
  }

  displayFn(result: AutocompleteResult | string): string {
    if (!result) return '';
    if (typeof result === 'string') return result;
    return result.ticker;
  }

  private autocomplete(query: string): Observable<AutocompleteResult[]> {
    return this.http
      .get<AutocompleteResult[]>(`${this.apiUrl}/api/v1/search/autocomplete`, {
        params: { q: query, limit: 8 },
      })
      .pipe(
        switchMap((results) => {
          if (!results || results.length === 0) {
            return of([]);
          }
          return of(results);
        })
      );
  }
}
