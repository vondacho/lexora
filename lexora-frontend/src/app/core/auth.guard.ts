import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from './auth.service';

/** Lets the logged-in user through; the others go to the login page, which brings them back afterwards. */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth
    .restore()
    .pipe(
      map((ok) =>
        ok
          ? true
          : router.createUrlTree(['/connexion'], {
              queryParams: state.url === '/' ? {} : { retour: state.url },
            }),
      ),
    );
};
