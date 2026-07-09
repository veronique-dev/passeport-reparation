import { Injectable } from '@angular/core';
import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest
} from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '../services/auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private readonly auth: AuthService) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = this.auth.accessToken;
    if (!token || req.url.includes('/api/auth/login') || req.url.includes('/api/auth/register')) {
      return next.handle(req);
    }
    return next.handle(req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    }));
  }
}
