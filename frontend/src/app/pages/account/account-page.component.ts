import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { UserProfile } from '../../core/models';

@Component({
  selector: 'app-account-page',
  templateUrl: './account-page.component.html',
  styleUrls: ['../login/login-page.component.scss', './account-page.component.scss']
})
export class AccountPageComponent implements OnInit {
  user: UserProfile | null = null;
  firstName = '';
  loading = false;
  message: string | null = null;
  error: string | null = null;

  constructor(
    private readonly auth: AuthService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.auth.me().subscribe({
      next: (user) => {
        this.user = user;
        this.firstName = user.firstName || '';
      },
      error: () => {
        this.auth.clearSession();
        void this.router.navigateByUrl('/connexion');
      }
    });
  }

  save(): void {
    this.loading = true;
    this.message = null;
    this.error = null;
    this.auth.updateMe(this.firstName.trim()).subscribe({
      next: (user) => {
        this.loading = false;
        this.user = user;
        this.message = 'Profil mis à jour.';
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message || 'Mise à jour impossible.';
      }
    });
  }

  logout(): void {
    this.auth.logout().subscribe({
      next: () => void this.router.navigateByUrl('/'),
      error: () => void this.router.navigateByUrl('/')
    });
  }
}
