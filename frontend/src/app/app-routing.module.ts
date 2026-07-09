import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { HomePageComponent } from './pages/home/home-page.component';
import { ResultPageComponent } from './pages/result/result-page.component';
import { LoginPageComponent } from './pages/login/login-page.component';
import { RegisterPageComponent } from './pages/register/register-page.component';
import { ForgotPasswordPageComponent } from './pages/forgot-password/forgot-password-page.component';
import { ResetPasswordPageComponent } from './pages/reset-password/reset-password-page.component';
import { ConfirmEmailPageComponent } from './pages/confirm-email/confirm-email-page.component';
import { AccountPageComponent } from './pages/account/account-page.component';
import { PassportsPageComponent } from './pages/passports/passports-page.component';
import { AuthGuard } from './core/guards/auth.guard';

const routes: Routes = [
  { path: '', component: HomePageComponent },
  { path: 'resultat', component: ResultPageComponent },
  { path: 'connexion', component: LoginPageComponent },
  { path: 'inscription', component: RegisterPageComponent },
  { path: 'mot-de-passe-oublie', component: ForgotPasswordPageComponent },
  { path: 'reinitialiser-mot-de-passe', component: ResetPasswordPageComponent },
  { path: 'confirmer-email', component: ConfirmEmailPageComponent },
  { path: 'compte', component: AccountPageComponent, canActivate: [AuthGuard] },
  { path: 'mes-passeports', component: PassportsPageComponent, canActivate: [AuthGuard] },
  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
