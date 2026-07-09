import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-reset-password-page',
  templateUrl: './reset-password-page.component.html',
  styleUrls: ['../login/login-page.component.scss']
})
export class ResetPasswordPageComponent implements OnInit {
  token = '';
  password = '';
  loading = false;
  error: string | null = null;
  success: string | null = null;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly auth: AuthService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') || '';
    if (!this.token) {
      this.error = 'Lien invalide.';
    }
  }

  submit(): void {
    if (!this.token) {
      return;
    }
    this.loading = true;
    this.error = null;
    this.auth.resetPassword(this.token, this.password).subscribe({
      next: (res) => {
        this.loading = false;
        this.success = res.message;
        setTimeout(() => void this.router.navigateByUrl('/connexion'), 1200);
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message || 'Réinitialisation impossible.';
      }
    });
  }
}
