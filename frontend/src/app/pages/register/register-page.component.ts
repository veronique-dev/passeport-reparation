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
        this.error = err?.error?.message || 'Inscription impossible.';
      }
    });
  }

  goLogin(): void {
    void this.router.navigateByUrl('/connexion');
  }
}
