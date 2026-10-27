import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/**
 * Adds the Basic credentials to the API calls.
 * X-Requested-With makes Spring Security answer 401 without WWW-Authenticate, so the browser shows no login popup.
 * A 401 during a session (password changed, user deleted) ends the session and goes back to the login page.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith('/api/')) {
    return next(req);
  }
  const auth = inject(AuthService);
  const router = inject(Router);
  const headers: Record<string, string> = { 'X-Requested-With': 'XMLHttpRequest' };
  const authorization = auth.authorization();
  if (authorization) {
    headers['Authorization'] = authorization;
  }
  return next(req.clone({ setHeaders: headers })).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && auth.loggedIn()) {
        const back = router.url;
        auth.logout();
        void router.navigate(['/connexion'], { queryParams: { retour: back } });
      }
      return throwError(() => error);
    }),
  );
};
