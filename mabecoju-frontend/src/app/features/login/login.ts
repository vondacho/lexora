import { Component, inject, input, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'mbj-login',
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** Page to return to after logging in (query parameter "retour"). */
  readonly retour = input<string>();

  protected username = '';
  protected password = '';
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  protected submit(): void {
    if (!this.username || !this.password || this.busy()) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    this.auth.login(this.username.trim(), this.password).subscribe({
      next: () => {
        const target = this.retour();
        // only paths of this application, never another site
        void this.router.navigateByUrl(target?.startsWith('/') && !target.startsWith('//') ? target : '/a-traiter');
      },
      error: (e: unknown) => {
        this.busy.set(false);
        this.password = '';
        this.error.set(
          e instanceof HttpErrorResponse && e.status === 401
            ? 'Nom d’utilisateur ou mot de passe incorrect.'
            : 'Le serveur ne répond pas. Réessayez dans un instant.',
        );
      },
    });
  }
}
