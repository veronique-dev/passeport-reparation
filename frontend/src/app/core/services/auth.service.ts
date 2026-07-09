import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { AuthTokens, MessageResponse, UserProfile } from '../models';

const ACCESS_KEY = 'pr_access_token';
const REFRESH_KEY = 'pr_refresh_token';
const USER_KEY = 'pr_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly base = environment.apiBaseUrl;
  private readonly userSubject = new BehaviorSubject<UserProfile | null>(this.readUser());

  readonly user$ = this.userSubject.asObservable();

  constructor(private readonly http: HttpClient) {}

  get accessToken(): string | null {
    return localStorage.getItem(ACCESS_KEY);
  }

  get refreshToken(): string | null {
    return localStorage.getItem(REFRESH_KEY);
  }

  get currentUser(): UserProfile | null {
    return this.userSubject.value;
  }

  get isLoggedIn(): boolean {
    return !!this.accessToken && !!this.currentUser;
  }

  register(email: string, password: string, firstName?: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.base}/api/auth/register`, {
      email,
      password,
      firstName: firstName || null
    });
  }

  login(email: string, password: string): Observable<AuthTokens> {
    return this.http.post<AuthTokens>(`${this.base}/api/auth/login`, { email, password }).pipe(
      tap(tokens => this.persistSession(tokens))
    );
  }

  refresh(): Observable<AuthTokens> {
    return this.http.post<AuthTokens>(`${this.base}/api/auth/refresh`, {
      refreshToken: this.refreshToken
    }).pipe(tap(tokens => this.persistSession(tokens)));
  }

  logout(): Observable<MessageResponse> {
    const refreshToken = this.refreshToken;
    this.clearSession();
    if (!refreshToken) {
      return new Observable(observer => {
        observer.next({ message: 'Déconnecté.' });
        observer.complete();
      });
    }
    return this.http.post<MessageResponse>(`${this.base}/api/auth/logout`, { refreshToken });
  }

  me(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.base}/api/auth/me`).pipe(
      tap(user => {
        localStorage.setItem(USER_KEY, JSON.stringify(user));
        this.userSubject.next(user);
      })
    );
  }

  updateMe(firstName: string): Observable<UserProfile> {
    return this.http.patch<UserProfile>(`${this.base}/api/auth/me`, { firstName }).pipe(
      tap(user => {
        localStorage.setItem(USER_KEY, JSON.stringify(user));
        this.userSubject.next(user);
      })
    );
  }

  confirmEmail(token: string): Observable<MessageResponse> {
    return this.http.get<MessageResponse>(`${this.base}/api/auth/confirm`, {
      params: { token }
    });
  }

  forgotPassword(email: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.base}/api/auth/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.base}/api/auth/reset-password`, {
      token,
      newPassword
    });
  }

  private persistSession(tokens: AuthTokens): void {
    localStorage.setItem(ACCESS_KEY, tokens.accessToken);
    localStorage.setItem(REFRESH_KEY, tokens.refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(tokens.user));
    this.userSubject.next(tokens.user);
  }

  clearSession(): void {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(USER_KEY);
    this.userSubject.next(null);
  }

  private readUser(): UserProfile | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as UserProfile;
    } catch {
      return null;
    }
  }
}
