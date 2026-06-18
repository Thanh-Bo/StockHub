import {
  signalStore,
  withState,
  withComputed,
  withMethods,
  patchState,
} from '@ngrx/signals';
import { computed, inject } from '@angular/core';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe, switchMap, tap, catchError, of } from 'rxjs';
import { ScreenerService } from '../services/screener.service';
import {
  FilterCriteria,
  SortConfig,
  ScreenerResultItem,
  ScreenerRequest,
} from '../models/screener.models';

interface ScreenerState {
  filters: FilterCriteria[];
  sort: SortConfig;
  results: ScreenerResultItem[] | null;
  loading: boolean;
  page: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  error: string | null;
}

const initialState: ScreenerState = {
  filters: [],
  sort: { field: 'marketCap', direction: 'DESC' },
  results: null,
  loading: false,
  page: 0,
  pageSize: 25,
  totalElements: 0,
  totalPages: 0,
  error: null,
};

export const ScreenerStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),

  withComputed(({ filters, results, page, pageSize, totalElements, loading }) => ({
    activeFilterCount: computed(() => filters().length),
    hasResults: computed(() => results() !== null && results()!.length > 0),
    hasMorePages: computed(() => page() < totalElements() / pageSize() - 1),
    hasPreviousPage: computed(() => page() > 0),
  })),

  withMethods((state, screenerService = inject(ScreenerService)) => ({
    addFilter(filter: FilterCriteria): void {
      const current = state.filters();
      const existing = current.findIndex(
        (f) => f.field === filter.field
      );
      if (existing >= 0) {
        const updated = [...current];
        updated[existing] = filter;
        patchState(state, { filters: updated });
      } else {
        patchState(state, { filters: [...current, filter] });
      }
    },

    removeFilter(field: string): void {
      patchState(state, {
        filters: state.filters().filter((f) => f.field !== field),
      });
    },

    updateFilter(field: string, filter: Partial<FilterCriteria>): void {
      const filters = state.filters().map((f) =>
        f.field === field ? { ...f, ...filter } : f
      );
      patchState(state, { filters });
    },

    clearFilters(): void {
      patchState(state, { filters: [], results: null, page: 0 });
    },

    setSort(sort: SortConfig): void {
      patchState(state, { sort });
    },

    setPage(page: number): void {
      patchState(state, { page });
    },

    search(): void {
      const request: ScreenerRequest = {
        filters: state.filters(),
        sort: state.sort(),
        pagination: {
          page: state.page(),
          size: state.pageSize(),
        },
      };

      patchState(state, { loading: true, error: null });

      screenerService.search(request).pipe(
        tap((response) =>
          patchState(state, {
            results: response.content,
            totalElements: response.totalElements,
            totalPages: response.totalPages,
            loading: false,
          })
        ),
        catchError((err) => {
          patchState(state, {
            loading: false,
            error: err?.error?.message ?? 'Search failed. Please try again.',
          });
          return of(null);
        })
      ).subscribe();
    },

    nextPage(): void {
      if (state.page() < state.totalPages() - 1) {
        const newPage = state.page() + 1;
        patchState(state, { page: newPage });
        this.search();
      }
    },

    previousPage(): void {
      if (state.page() > 0) {
        const newPage = state.page() - 1;
        patchState(state, { page: newPage });
        this.search();
      }
    },
  }))
);
