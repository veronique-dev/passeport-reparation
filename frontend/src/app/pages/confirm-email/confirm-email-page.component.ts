import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-confirm-email-page',
  templateUrl: './confirm-email-page.component.html',
  styleUrls: ['../login/login-page.component.scss']
})
export class ConfirmEmailPageComponent implements OnInit {
  loading = true;
  error: string | null = null;
  success: string | null = null;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly auth: AuthService
  ) {}

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.loading = false;
      this.error = 'Lien de confirmation manquant.';
      return;
    }
    this.auth.confirmEmail(token).subscribe({
      next: (res) => {
        this.loading = false;
        this.success = res.message;
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message || 'Confirmation impossible.';
      }
    });
  }
}
