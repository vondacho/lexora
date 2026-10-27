import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService } from '../core/auth.service';
import { NAV_ITEMS } from './navigation';
import { texts } from '../i18n/i18n';
import { LangToggle } from '../i18n/lang-toggle';

const NAV_KEY = 'mabecoju.nav';
/** Below this width the sidebar becomes a drawer over the page. */
const NARROW_QUERY = '(max-width: 900px)';

@Component({
  selector: 'mbj-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, LangToggle],
  templateUrl: './shell.html',
  styleUrl: './shell.css',
  host: { '(keydown.escape)': 'closeDrawer()' },
})
export class Shell {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly items = NAV_ITEMS;
  protected readonly t = texts;
  protected readonly user = this.auth.user;
  protected readonly initials = computed(() => {
    const u = this.user();
    return u?.shortcut || (u?.firstName?.[0] ?? '') + (u?.lastName?.[0] ?? '') || u?.username || '';
  });
  protected readonly fullName = computed(() => {
    const u = this.user();
    return [u?.firstName, u?.lastName].filter(Boolean).join(' ') || u?.username || '';
  });

  protected readonly narrow = signal(false);
  /** Sidebar shown on a wide screen; remembered between visits. */
  private readonly sidebarOpen = signal(readSidebarPreference());
  /** Drawer shown on a narrow screen; always starts closed. */
  private readonly drawerOpen = signal(false);
  protected readonly navOpen = computed(() => (this.narrow() ? this.drawerOpen() : this.sidebarOpen()));
  protected readonly navLabel = computed(() => (this.navOpen() ? texts().shell.hideMenu : texts().shell.showMenu));

  constructor() {
    const media = typeof matchMedia === 'function' ? matchMedia(NARROW_QUERY) : null;
    if (media) {
      this.narrow.set(media.matches);
      const onChange = (event: MediaQueryListEvent) => {
        this.narrow.set(event.matches);
        this.drawerOpen.set(false);
      };
      media.addEventListener('change', onChange);
      inject(DestroyRef).onDestroy(() => media.removeEventListener('change', onChange));
    }
    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.drawerOpen.set(false));
  }

  protected toggleNav(): void {
    if (this.narrow()) {
      this.drawerOpen.update((open) => !open);
    } else {
      const open = !this.sidebarOpen();
      this.sidebarOpen.set(open);
      try {
        localStorage.setItem(NAV_KEY, open ? 'open' : 'closed');
      } catch {
        // storage unavailable: the choice lasts until the next reload
      }
    }
  }

  protected closeDrawer(): void {
    this.drawerOpen.set(false);
  }

  protected logout(): void {
    this.auth.logout();
    void this.router.navigate(['/connexion']);
  }
}

function readSidebarPreference(): boolean {
  try {
    return localStorage.getItem(NAV_KEY) !== 'closed';
  } catch {
    return true;
  }
}
