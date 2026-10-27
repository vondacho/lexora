import { computed, inject, Injectable, signal } from '@angular/core';
import { catchError, map, Observable, of, tap } from 'rxjs';
import { User, UsersService } from '../api/generated';

const STORAGE_KEY = 'lexora.auth';

/**
 * Session of the user, authenticated with HTTP Basic as the API requires.
 * The credentials live in sessionStorage: they survive a reload but not the closing of the tab.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly users = inject(UsersService);
  private readonly token = signal<string | null>(readStoredToken());

  readonly user = signal<User | null>(null);
  readonly loggedIn = computed(() => this.user() !== null);
  /** Value of the Authorization header, null when nobody is logged in. */
  readonly authorization = computed(() => {
    const token = this.token();
    return token ? 'Basic ' + token : null;
  });

  /** Checks the credentials against /api/me and keeps them when they are accepted. */
  login(username: string, password: string): Observable<User> {
    this.token.set(basicToken(username, password));
    return this.loadUser().pipe(
      tap({
        next: () => storeToken(this.token()),
        error: () => this.logout(),
      }),
    );
  }

  /** True when a session exists or could be restored from the stored credentials. */
  restore(): Observable<boolean> {
    if (this.user()) {
      return of(true);
    }
    if (!this.token()) {
      return of(false);
    }
    return this.loadUser().pipe(
      map(() => true),
      catchError(() => {
        this.logout();
        return of(false);
      }),
    );
  }

  logout(): void {
    this.token.set(null);
    this.user.set(null);
    storeToken(null);
  }

  /** Grants are written module:page, e.g. invoicing:debtors_open. */
  hasGrant(grant: string): boolean {
    return this.user()?.grants?.includes(grant) ?? false;
  }

  private loadUser(): Observable<User> {
    return this.users.getCurrentUser().pipe(tap((user) => this.user.set(user)));
  }
}

/** base64 of "user:password" in UTF-8, as RFC 7617 recommends. */
export function basicToken(username: string, password: string): string {
  const bytes = new TextEncoder().encode(username + ':' + password);
  return btoa(String.fromCharCode(...bytes));
}

function readStoredToken(): string | null {
  try {
    return sessionStorage.getItem(STORAGE_KEY);
  } catch {
    return null;
  }
}

function storeToken(token: string | null): void {
  try {
    if (token) {
      sessionStorage.setItem(STORAGE_KEY, token);
    } else {
      sessionStorage.removeItem(STORAGE_KEY);
    }
  } catch {
    // storage unavailable (private mode): the session lasts until the next reload
  }
}
