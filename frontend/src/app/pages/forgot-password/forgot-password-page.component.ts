import { Component } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password-page',
  templateUrl: './forgot-password-page.component.html',
  styleUrls: ['../login/login-page.component.scss']
})
export class ForgotPasswordPageComponent {
  email = '';
  loading = false;
  error: string | null = null;
  success: string | null = null;

  constructor(private readonly auth: AuthService) {}

  submit(): void {
    this.loading = true;
    this.error = null;
    this.auth.forgotPassword(this.email.trim()).subscribe({
      next: (res) => {
        this.loading = false;
        this.success = res.message;
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message || 'Envoi impossible.';
      }
    });
  }
}
