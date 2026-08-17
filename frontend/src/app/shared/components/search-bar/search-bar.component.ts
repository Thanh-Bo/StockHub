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
import { AppIconComponent } from '../app-icon/app-icon.component';

@Component({
  selector: 'app-search-bar',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, AppIconComponent],
  templateUrl: './search-bar.component.html',
  styleUrls: ['./search-bar.component.scss'],
})
export class SearchBarComponent implements OnInit, OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  searchControl = new FormControl<string>('');
  suggestions: AutocompleteResult[] = [];
  showDropdown = false;
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
        this.showDropdown = true;
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onFocus(): void {
    this.showDropdown = true;
  }

  onBlur(): void {
    setTimeout(() => {
      this.showDropdown = false;
    }, 150);
  }

  selectSuggestion(item: AutocompleteResult): void {
    if (item.ticker) {
      this.router.navigate(['/stocks', item.ticker]);
      this.searchControl.setValue('');
      this.showDropdown = false;
    }
  }

  onEnter(): void {
    const query = this.searchControl.value?.trim();
    if (query) {
      this.search.emit(query);
      this.router.navigate(['/search'], { queryParams: { q: query } });
      this.searchControl.setValue('');
      this.showDropdown = false;
    }
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
