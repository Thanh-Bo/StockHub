import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, BehaviorSubject, of } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { environment } from '@env/environment';
import {
  LoginRequest,
  RegisterRequest,
  LoginResponse,
  RefreshRequest,
  UserResponse,
  JwtPayload,
} from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly apiUrl = `${environment.apiUrl}/api/v1/auth`;

  private accessToken: string | null = null;
  private refreshTokenValue: string | null = null;
  private currentUserSubject = new BehaviorSubject<UserResponse | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  login(email: string, password: string): Observable<LoginResponse> {
    const body: LoginRequest = { email, password };
    return this.http
      .post<LoginResponse>(`${this.apiUrl}/login`, body)
      .pipe(tap((res) => this.storeTokens(res)));
  }

  register(request: RegisterRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${this.apiUrl}/register`, request)
      .pipe(tap((res) => this.storeTokens(res)));
  }

  refreshToken(): Observable<LoginResponse> {
    const body: RefreshRequest = { refreshToken: this.refreshTokenValue ?? '' };
    return this.http
      .post<LoginResponse>(`${this.apiUrl}/refresh`, body)
      .pipe(tap((res) => this.storeTokens(res)));
  }

  logout(): void {
    const token = this.accessToken;
    if (token) {
      this.http
        .post(`${this.apiUrl}/logout`, {})
        .pipe(
          catchError(() => of(null))
        )
        .subscribe();
    }
    this.clearTokens();
    this.router.navigate(['/auth/login']);
  }

  getAccessToken(): string | null {
    if (this.accessToken && !this.isTokenExpired(this.accessToken)) {
      return this.accessToken;
    }
    return null;
  }

  isAuthenticated(): boolean {
    return !!this.accessToken && !this.isTokenExpired(this.accessToken);
  }

  hasRole(role: string): boolean {
    const payload = this.decodeToken(this.accessToken);
    if (payload && payload.roles) {
      return payload.roles.includes(role);
    }
    return false;
  }

  getCurrentUser(): Observable<UserResponse> {
    if (this.currentUserSubject.value) {
      return of(this.currentUserSubject.value);
    }
    return this.http
      .get<UserResponse>(`${this.apiUrl}/me`)
      .pipe(tap((user) => this.currentUserSubject.next(user)));
  }

  googleLogin(): void {
    window.location.href = `${this.apiUrl}/oauth2/google`;
  }

  private storeTokens(response: LoginResponse): void {
    this.accessToken = response.accessToken;
    this.refreshTokenValue = response.refreshToken;
    if (response.user) {
      this.currentUserSubject.next(response.user);
    }
  }

  private clearTokens(): void {
    this.accessToken = null;
    this.refreshTokenValue = null;
    this.currentUserSubject.next(null);
  }

  private isTokenExpired(token: string): boolean {
    const payload = this.decodeToken(token);
    if (!payload) {
      return true;
    }
    const now = Math.floor(Date.now() / 1000);
    return payload.exp < now;
  }

  private decodeToken(token: string | null): JwtPayload | null {
    if (!token) {
      return null;
    }
    try {
      const parts = token.split('.');
      if (parts.length !== 3) {
        return null;
      }
      const payload = JSON.parse(atob(parts[1]));
      return {
        sub: payload.sub ?? '',
        roles: payload.roles ?? [],
        exp: payload.exp ?? 0,
        iat: payload.iat ?? 0,
        email: payload.email ?? '',
      };
    } catch {
      return null;
    }
  }
}
