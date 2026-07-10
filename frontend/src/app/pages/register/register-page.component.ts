import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-register-page',
  templateUrl: './register-page.component.html',
  styleUrls: ['../login/login-page.component.scss']
})
export class RegisterPageComponent {
  email = '';
  password = '';
  firstName = '';
  loading = false;
  error: string | null = null;
  success: string | null = null;

  constructor(
    private readonly auth: AuthService,
    private readonly router: Router
  ) {}

  submit(): void {
    this.loading = true;
    this.error = null;
    this.success = null;
    this.auth.register(this.email.trim(), this.password, this.firstName.trim() || undefined).subscribe({
      next: (res) => {
        this.loading = false;
        this.success = res.message;
      },
      error: (err) => {
        this.loading = false;
        this.error = this.describeError(err);
      }
    });
  }

  goLogin(): void {
    void this.router.navigateByUrl('/connexion');
  }

  private describeError(err: any): string {
    const apiMessage = err?.error?.message;
    if (apiMessage) {
      return apiMessage;
    }
    if (err?.status === 0) {
      return 'API indisponible. Vérifie que la gateway tourne sur http://localhost:8090.';
    }
    if (err?.status === 404) {
      return 'Service compte indisponible (auth-service). Lance docker compose up -d auth-service mailhog, puis redémarre la gateway.';
    }
    if (err?.status === 403 || err?.status >= 500) {
      return 'Inscription temporairement indisponible. Vérifie que auth-service et Mailpit tournent (docker compose up -d auth-service mailhog).';
    }
    return 'Inscription impossible.';
  }
}
