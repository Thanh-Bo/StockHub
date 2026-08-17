import { Component, inject, OnInit } from '@angular/core';
import { RouterOutlet, RouterModule, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { SearchBarComponent } from './shared/components/search-bar/search-bar.component';
import { AppIconComponent } from './shared/components/app-icon/app-icon.component';
import { SnackbarContainerComponent } from './shared/components/snackbar-container/snackbar-container.component';
import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterModule,
    SearchBarComponent,
    AppIconComponent,
    SnackbarContainerComponent,
  ],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  isLoggedIn = false;
  userDisplayName = '';
  menuOpen = false;
  currentYear = new Date().getFullYear();

  ngOnInit(): void {
    this.authService.currentUser$.subscribe((user) => {
      this.isLoggedIn = !!user;
      if (user) {
        this.userDisplayName = `${user.firstName} ${user.lastName}`;
      }
    });

    // Try to load current user if we have a token
    if (this.authService.isAuthenticated()) {
      this.authService.getCurrentUser().subscribe();
    }
  }

  login(): void {
    this.router.navigate(['/auth/login']);
  }

  register(): void {
    this.router.navigate(['/auth/register']);
  }

  logout(): void {
    this.authService.logout();
  }

  goToWatchlists(): void {
    this.router.navigate(['/watchlists']);
  }

  onSearch(query: string): void {
    this.router.navigate(['/search'], { queryParams: { q: query } });
  }
}
