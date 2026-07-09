import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { switchMap } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { DiagnosisClaimService } from '../../core/services/diagnosis-claim.service';

@Component({
  selector: 'app-login-page',
  templateUrl: './login-page.component.html',
  styleUrls: ['./login-page.component.scss']
})
export class LoginPageComponent {
  email = '';
  password = '';
  loading = false;
  error: string | null = null;

  constructor(
    private readonly auth: AuthService,
    private readonly claim: DiagnosisClaimService,
    private readonly router: Router
  ) {}

  submit(): void {
    this.loading = true;
    this.error = null;
    this.auth.login(this.email.trim(), this.password).pipe(
      switchMap(() => this.claim.claimPendingAfterLogin())
    ).subscribe({
      next: () => {
        this.loading = false;
        void this.router.navigateByUrl('/mes-passeports');
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message || 'Connexion impossible.';
      }
    });
  }
}
