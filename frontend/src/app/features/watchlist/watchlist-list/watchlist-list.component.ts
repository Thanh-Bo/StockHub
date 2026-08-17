import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { WatchlistStore } from '../store/watchlist.store';
import {
  WatchlistSummary,
} from '../models/watchlist.models';
import { LoadingSpinnerComponent } from '@app/shared/components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state.component';
import { AppIconComponent } from '@app/shared/components/app-icon/app-icon.component';
import { SnackbarService } from '@app/shared/services/snackbar.service';

@Component({
  selector: 'app-watchlist-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    LoadingSpinnerComponent,
    EmptyStateComponent,
    AppIconComponent,
  ],
  templateUrl: './watchlist-list.component.html',
  styleUrls: ['./watchlist-list.component.scss'],
})
export class WatchlistListComponent implements OnInit {
  private readonly snackBar = inject(SnackbarService);
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
