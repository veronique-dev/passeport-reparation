import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { AuthGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';

describe('AuthGuard', () => {
  let guard: AuthGuard;
  let auth: { isLoggedIn: boolean };
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    auth = { isLoggedIn: false };
    router = jasmine.createSpyObj('Router', ['parseUrl']);
    router.parseUrl.and.returnValue('/connexion' as never);

    TestBed.configureTestingModule({
      providers: [
        AuthGuard,
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: router }
      ]
    });
    guard = TestBed.inject(AuthGuard);
  });

  it('allows logged-in users', () => {
    auth.isLoggedIn = true;
    expect(guard.canActivate()).toBeTrue();
    expect(router.parseUrl).not.toHaveBeenCalled();
  });

  it('redirects guests to /connexion', () => {
    auth.isLoggedIn = false;
    expect(guard.canActivate()).toBe('/connexion' as never);
    expect(router.parseUrl).toHaveBeenCalledWith('/connexion');
  });
});
