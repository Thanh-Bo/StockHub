import { Component, inject, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { WatchlistStore } from '../store/watchlist.store';
import {
  WatchlistSummary,
  CreateWatchlistRequest,
} from '../models/watchlist.models';
import { LoadingSpinnerComponent } from '@app/shared/components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state.component';
import { StockPercentPipe } from '@app/shared/pipes/percent.pipe';
import { BigNumberPipe } from '@app/shared/pipes/big-number.pipe';

@Component({
  selector: 'app-watchlist-list',
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
    MatTooltipModule,
    MatSnackBarModule,
    LoadingSpinnerComponent,
    EmptyStateComponent,
    StockPercentPipe,
    BigNumberPipe,
  ],
  templateUrl: './watchlist-list.component.html',
  styleUrls: ['./watchlist-list.component.scss'],
})
export class WatchlistListComponent implements OnInit {
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  readonly store = inject(WatchlistStore);

  readonly showCreateForm = signal(false);
  readonly newWatchlistName = signal('');
  readonly newWatchlistDesc = signal('');

  ngOnInit(): void {
    this.store.loadWatchlists();
  }

  openCreateForm(): void {
    this.showCreateForm.set(true);
    this.newWatchlistName.set('');
    this.newWatchlistDesc.set('');
  }

  cancelCreate(): void {
    this.showCreateForm.set(false);
    this.newWatchlistName.set('');
    this.newWatchlistDesc.set('');
  }

  createWatchlist(): void {
    const name = this.newWatchlistName().trim();
    if (!name) {
      this.snackBar.open('Watchlist name is required.', 'Close', {
        duration: 3000,
      });
      return;
    }

    this.store.createWatchlist(name, this.newWatchlistDesc().trim());
    this.showCreateForm.set(false);
    this.newWatchlistName.set('');
    this.newWatchlistDesc.set('');
  }

  deleteWatchlist(event: Event, watchlist: WatchlistSummary): void {
    event.stopPropagation();
    event.preventDefault();

    if (confirm(`Delete watchlist "${watchlist.name}"? This action cannot be undone.`)) {
      this.store.deleteWatchlist(watchlist.id);
    }
  }
}
