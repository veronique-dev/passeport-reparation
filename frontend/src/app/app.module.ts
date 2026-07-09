import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { HomePageComponent } from './pages/home/home-page.component';
import { ResultPageComponent } from './pages/result/result-page.component';
import { LoginPageComponent } from './pages/login/login-page.component';
import { RegisterPageComponent } from './pages/register/register-page.component';
import { ForgotPasswordPageComponent } from './pages/forgot-password/forgot-password-page.component';
import { ResetPasswordPageComponent } from './pages/reset-password/reset-password-page.component';
import { ConfirmEmailPageComponent } from './pages/confirm-email/confirm-email-page.component';
import { AccountPageComponent } from './pages/account/account-page.component';
import { PassportsPageComponent } from './pages/passports/passports-page.component';
import { AuthInterceptor } from './core/interceptors/auth.interceptor';

@NgModule({
  declarations: [
    AppComponent,
    HomePageComponent,
    ResultPageComponent,
    LoginPageComponent,
    RegisterPageComponent,
    ForgotPasswordPageComponent,
    ResetPasswordPageComponent,
    ConfirmEmailPageComponent,
    AccountPageComponent,
    PassportsPageComponent
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    FormsModule,
    AppRoutingModule
  ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }
  ],
  bootstrap: [AppComponent]
})
export class AppModule {}
