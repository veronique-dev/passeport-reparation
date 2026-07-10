import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { AuthTokens, UserProfile } from '../models';
import { environment } from '../../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  const user: UserProfile = {
    id: 'u1',
    email: 'ada@example.com',
    firstName: 'Ada',
    emailVerified: true
  };

  const tokens: AuthTokens = {
    accessToken: 'access-1',
    refreshToken: 'refresh-1',
    tokenType: 'Bearer',
    expiresInSeconds: 900,
    user
  };

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('persists session on login', () => {
    let logged: AuthTokens | undefined;
    service.login('ada@example.com', 'secret123').subscribe(res => (logged = res));

    const req = http.expectOne(`${environment.apiBaseUrl}/api/auth/login`);
    expect(req.request.method).toBe('POST');
    req.flush(tokens);

    expect(logged).toEqual(tokens);
    expect(service.accessToken).toBe('access-1');
    expect(service.refreshToken).toBe('refresh-1');
    expect(service.isLoggedIn).toBeTrue();
    expect(service.currentUser?.email).toBe('ada@example.com');
  });

  it('clears session on logout and posts refresh token', () => {
    localStorage.setItem('pr_access_token', 'access-1');
    localStorage.setItem('pr_refresh_token', 'refresh-1');
    localStorage.setItem('pr_user', JSON.stringify(user));

    service.logout().subscribe();
    expect(service.accessToken).toBeNull();

    const req = http.expectOne(`${environment.apiBaseUrl}/api/auth/logout`);
    expect(req.request.body).toEqual({ refreshToken: 'refresh-1' });
    req.flush({ message: 'Déconnecté.' });
  });

  it('registers without persisting session', () => {
    service.register('ada@example.com', 'secret123', 'Ada').subscribe();
    const req = http.expectOne(`${environment.apiBaseUrl}/api/auth/register`);
    expect(req.request.body).toEqual({
      email: 'ada@example.com',
      password: 'secret123',
      firstName: 'Ada'
    });
    req.flush({ message: 'ok' });
    expect(service.isLoggedIn).toBeFalse();
  });

  it('refreshes tokens', () => {
    localStorage.setItem('pr_refresh_token', 'refresh-old');
    service.refresh().subscribe();
    const req = http.expectOne(`${environment.apiBaseUrl}/api/auth/refresh`);
    expect(req.request.body).toEqual({ refreshToken: 'refresh-old' });
    req.flush(tokens);
    expect(service.accessToken).toBe('access-1');
  });
});
