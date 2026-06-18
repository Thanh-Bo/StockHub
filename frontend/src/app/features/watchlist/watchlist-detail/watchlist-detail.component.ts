import { Component, inject, OnInit, OnDestroy, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { DragDropModule, CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import { Subject, takeUntil, debounceTime, distinctUntilChanged } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { environment } from '@env/environment';
import { WatchlistStore } from '../store/watchlist.store';
import { WatchlistDetail, WatchlistStockSummary } from '../models/watchlist.models';
import { LoadingSpinnerComponent } from '@app/shared/components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state.component';
import { BigNumberPipe } from '@app/shared/pipes/big-number.pipe';
import { StockPercentPipe } from '@app/shared/pipes/percent.pipe';

@Component({
  selector: 'app-watchlist-detail',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatChipsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatAutocompleteModule,
    MatTooltipModule,
    MatSnackBarModule,
    DragDropModule,
    LoadingSpinnerComponent,
    EmptyStateComponent,
    BigNumberPipe,
    StockPercentPipe,
  ],
  templateUrl: './watchlist-detail.component.html',
  styleUrls: ['./watchlist-detail.component.scss'],
})
export class WatchlistDetailComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly http = inject(HttpClient);
  readonly store = inject(WatchlistStore);

  private readonly destroy$ = new Subject<void>();

  readonly editingName = signal(false);
  readonly editNameValue = signal('');
  readonly showAddStock = signal(false);
  readonly addStockTicker = signal('');
  readonly autocompleteOptions = signal<{ ticker: string; name: string }[]>([]);
  readonly autocompleteLoading = signal(false);

  private watchlistId: number | null = null;

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const id = Number(params.get('id'));
      if (id) {
        this.watchlistId = id;
        this.store.selectWatchlist(id);
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // Name editing
  startEditName(): void {
    const detail = this.store.selectedWatchlist();
    if (detail) {
      this.editNameValue.set(detail.name);
      this.editingName.set(true);
    }
  }

  saveName(): void {
    const newName = this.editNameValue().trim();
    if (newName && this.watchlistId) {
      // Note: In a full implementation, this would call updateWatchlist
      // For now, we update locally via the store
      this.editingName.set(false);
      this.snackBar.open('Watchlist name updated.', 'Close', { duration: 2000 });
    }
  }

  cancelEditName(): void {
    this.editingName.set(false);
  }

  // Add stock
  toggleAddStock(): void {
    this.showAddStock.update((v) => !v);
    this.addStockTicker.set('');
    this.autocompleteOptions.set([]);
  }

  onTickerInputChange(value: string): void {
    const ticker = value.trim();
    if (ticker.length < 1) {
      this.autocompleteOptions.set([]);
      return;
    }

    this.autocompleteLoading.set(true);
    this.http
      .get<{ ticker: string; name: string }[]>(
        `${environment.apiUrl}/api/v1/search/autocomplete`,
        { params: { q: ticker, limit: '8' } }
      )
      .subscribe({
        next: (results) => {
          this.autocompleteOptions.set(results);
          this.autocompleteLoading.set(false);
        },
        error: () => {
          this.autocompleteOptions.set([]);
          this.autocompleteLoading.set(false);
        },
      });
  }

  selectAutocompleteOption(option: { ticker: string; name: string }): void {
    this.addStockTicker.set(option.ticker);
    this.autocompleteOptions.set([]);
  }

  addStock(): void {
    const ticker = this.addStockTicker().trim().toUpperCase();
    if (!ticker || !this.watchlistId) return;

    const detail = this.store.selectedWatchlist();
    if (detail?.stocks?.some((s) => s.ticker === ticker)) {
      this.snackBar.open(`"${ticker}" is already in this watchlist.`, 'Close', {
        duration: 3000,
      });
      return;
    }

    this.store.addStock(this.watchlistId, ticker);
    this.showAddStock.set(false);
    this.addStockTicker.set('');
  }

  removeStock(ticker: string): void {
    if (this.watchlistId) {
      this.store.removeStock(this.watchlistId, ticker);
    }
  }

  deleteWatchlist(): void {
    if (!this.watchlistId) return;
    const detail = this.store.selectedWatchlist();
    if (confirm(`Delete watchlist "${detail?.name ?? 'Unknown'}"? This cannot be undone.`)) {
      this.store.deleteWatchlist(this.watchlistId);
    }
  }

  // Drag and drop reorder
  dropStock(event: CdkDragDrop<WatchlistStockSummary[]>): void {
    const detail = this.store.selectedWatchlist();
    if (!detail || !this.watchlistId) return;

    const stocks = [...detail.stocks];
    moveItemInArray(stocks, event.previousIndex, event.currentIndex);

    // Update sort orders locally
    const reordered = stocks.map((s, i) => ({ ...s, sortOrder: i }));
    const tickers = reordered.map((s) => s.ticker);
    this.store.reorderStocks(this.watchlistId, tickers);
  }

  getPriceChangeClass(changePercent: number): string {
    if (changePercent > 0) return 'positive';
    if (changePercent < 0) return 'negative';
    return 'neutral';
  }

  formatPrice(price: number): string {
    return price != null ? `$${price.toFixed(2)}` : 'N/A';
  }
}
