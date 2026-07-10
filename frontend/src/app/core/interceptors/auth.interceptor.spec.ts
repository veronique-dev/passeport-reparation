import { TestBed } from '@angular/core/testing';
import { HttpHandler, HttpRequest, HttpResponse } from '@angular/common/http';
import { of } from 'rxjs';
import { AuthInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';

describe('AuthInterceptor', () => {
  let interceptor: AuthInterceptor;
  let auth: { accessToken: string | null };
  let next: jasmine.SpyObj<HttpHandler>;

  beforeEach(() => {
    auth = { accessToken: 'tok-123' };
    next = jasmine.createSpyObj('HttpHandler', ['handle']);
    next.handle.and.callFake((req: HttpRequest<unknown>) => of(new HttpResponse({ status: 200, body: {}, url: req.url })));

    TestBed.configureTestingModule({
      providers: [
        AuthInterceptor,
        { provide: AuthService, useValue: auth }
      ]
    });
    interceptor = TestBed.inject(AuthInterceptor);
  });

  it('adds Bearer token to API requests', () => {
    const req = new HttpRequest('GET', 'http://localhost:8090/api/diagnoses/mine');
    interceptor.intercept(req, next).subscribe();
    const forwarded = next.handle.calls.mostRecent().args[0] as HttpRequest<unknown>;
    expect(forwarded.headers.get('Authorization')).toBe('Bearer tok-123');
  });

  it('skips Authorization on login', () => {
    const req = new HttpRequest('POST', 'http://localhost:8090/api/auth/login', {});
    interceptor.intercept(req, next).subscribe();
    const forwarded = next.handle.calls.mostRecent().args[0] as HttpRequest<unknown>;
    expect(forwarded.headers.has('Authorization')).toBeFalse();
  });

  it('skips Authorization when no token', () => {
    auth.accessToken = null;
    const req = new HttpRequest('GET', 'http://localhost:8090/api/diagnoses/mine');
    interceptor.intercept(req, next).subscribe();
    const forwarded = next.handle.calls.mostRecent().args[0] as HttpRequest<unknown>;
    expect(forwarded.headers.has('Authorization')).toBeFalse();
  });
});
