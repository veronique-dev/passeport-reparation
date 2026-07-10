import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { LoginPageComponent } from './login-page.component';
import { AuthService } from '../../core/services/auth.service';
import { DiagnosisClaimService } from '../../core/services/diagnosis-claim.service';
import { AuthTokens } from '../../core/models';

describe('LoginPageComponent', () => {
  let component: LoginPageComponent;
  let fixture: ComponentFixture<LoginPageComponent>;
  let auth: jasmine.SpyObj<AuthService>;
  let claim: jasmine.SpyObj<DiagnosisClaimService>;
  let router: jasmine.SpyObj<Router>;

  const tokens = {
    accessToken: 'a',
    refreshToken: 'r',
    tokenType: 'Bearer',
    expiresInSeconds: 900,
    user: { id: 'u1', email: 'ada@example.com', emailVerified: true }
  } as AuthTokens;

  beforeEach(async () => {
    auth = jasmine.createSpyObj('AuthService', ['login']);
    claim = jasmine.createSpyObj('DiagnosisClaimService', ['claimPendingAfterLogin']);
    router = jasmine.createSpyObj('Router', ['navigateByUrl']);

    await TestBed.configureTestingModule({
      declarations: [LoginPageComponent],
      imports: [FormsModule],
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: DiagnosisClaimService, useValue: claim },
        { provide: Router, useValue: router }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('logs in, claims pending passport, then navigates', fakeAsync(() => {
    component.email = ' ada@example.com ';
    component.password = 'secret123';
    auth.login.and.returnValue(of(tokens));
    claim.claimPendingAfterLogin.and.returnValue(of(null));
    router.navigateByUrl.and.returnValue(Promise.resolve(true));

    component.submit();
    tick();

    expect(auth.login).toHaveBeenCalledWith('ada@example.com', 'secret123');
    expect(claim.claimPendingAfterLogin).toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/mes-passeports');
    expect(component.loading).toBeFalse();
    expect(component.error).toBeNull();
  }));

  it('shows API error message on failure', fakeAsync(() => {
    component.email = 'ada@example.com';
    component.password = 'bad';
    auth.login.and.returnValue(throwError(() => ({ error: { message: 'Email ou mot de passe incorrect.' } })));

    component.submit();
    tick();

    expect(component.loading).toBeFalse();
    expect(component.error).toBe('Email ou mot de passe incorrect.');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  }));
});
