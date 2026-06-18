import {
  signalStore,
  withState,
  withComputed,
  withMethods,
  patchState,
} from '@ngrx/signals';
import { computed, inject } from '@angular/core';
import { tap, catchError, of, switchMap, finalize } from 'rxjs';
import { WatchlistService } from '../services/watchlist.service';
import {
  WatchlistSummary,
  WatchlistDetail,
} from '../models/watchlist.models';

interface WatchlistState {
  watchlists: WatchlistSummary[];
  selectedWatchlist: WatchlistDetail | null;
  loading: boolean;
  error: string | null;
}

const initialState: WatchlistState = {
  watchlists: [],
  selectedWatchlist: null,
  loading: false,
  error: null,
};

export const WatchlistStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),

  withComputed(({ watchlists, selectedWatchlist, loading }) => ({
    hasWatchlists: computed(() => watchlists().length > 0),
    defaultWatchlist: computed(() =>
      watchlists().find((w) => w.isDefault) ?? null
    ),
    stockCount: computed(() => selectedWatchlist()?.stocks?.length ?? 0),
  })),

  withMethods((state, watchlistService = inject(WatchlistService)) => ({
    loadWatchlists(): void {
      patchState(state, { loading: true, error: null });
      watchlistService.getWatchlists().pipe(
        tap((watchlists) =>
          patchState(state, { watchlists, loading: false })
        ),
        catchError((err) => {
          patchState(state, {
            loading: false,
            error: err?.error?.message ?? 'Failed to load watchlists.',
          });
          return of(null);
        })
      ).subscribe();
    },

    selectWatchlist(id: number): void {
      patchState(state, { loading: true, error: null, selectedWatchlist: null });
      watchlistService.getWatchlistDetail(id).pipe(
        tap((detail) =>
          patchState(state, { selectedWatchlist: detail, loading: false })
        ),
        catchError((err) => {
          patchState(state, {
            loading: false,
            error: err?.error?.message ?? 'Failed to load watchlist.',
          });
          return of(null);
        })
      ).subscribe();
    },

    createWatchlist(name: string, description: string): void {
      patchState(state, { loading: true, error: null });
      watchlistService.createWatchlist({ name, description }).pipe(
        tap(() => {
          patchState(state, { loading: false });
          this.loadWatchlists();
        }),
        catchError((err) => {
          patchState(state, {
            loading: false,
            error: err?.error?.message ?? 'Failed to create watchlist.',
          });
          return of(null);
        })
      ).subscribe();
    },

    deleteWatchlist(id: number): void {
      patchState(state, { loading: true, error: null });
      watchlistService.deleteWatchlist(id).pipe(
        tap(() => {
          patchState(state, {
            watchlists: state.watchlists().filter((w) => w.id !== id),
            selectedWatchlist:
              state.selectedWatchlist()?.id === id
                ? null
                : state.selectedWatchlist(),
            loading: false,
          });
        }),
        catchError((err) => {
          patchState(state, {
            loading: false,
            error: err?.error?.message ?? 'Failed to delete watchlist.',
          });
          return of(null);
        })
      ).subscribe();
    },

    addStock(watchlistId: number, ticker: string): void {
      patchState(state, { loading: true, error: null });
      watchlistService.addStock(watchlistId, ticker).pipe(
        tap(() => {
          patchState(state, { loading: false });
          this.selectWatchlist(watchlistId);
        }),
        catchError((err) => {
          patchState(state, {
            loading: false,
            error: err?.error?.message ?? 'Failed to add stock.',
          });
          return of(null);
        })
      ).subscribe();
    },

    removeStock(watchlistId: number, ticker: string): void {
      patchState(state, { loading: true, error: null });
      watchlistService.removeStock(watchlistId, ticker).pipe(
        tap(() => {
          patchState(state, { loading: false });
          this.selectWatchlist(watchlistId);
        }),
        catchError((err) => {
          patchState(state, {
            loading: false,
            error: err?.error?.message ?? 'Failed to remove stock.',
          });
          return of(null);
        })
      ).subscribe();
    },

    reorderStocks(watchlistId: number, tickers: string[]): void {
      patchState(state, { loading: true, error: null });
      watchlistService.reorderStocks(watchlistId, tickers).pipe(
        tap(() => {
          patchState(state, { loading: false });
          this.selectWatchlist(watchlistId);
        }),
        catchError((err) => {
          patchState(state, {
            loading: false,
            error: err?.error?.message ?? 'Failed to reorder stocks.',
          });
          return of(null);
        })
      ).subscribe();
    },
  }))
);
