import { computed, DOCUMENT, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom, timeout } from 'rxjs';
import { Branding, BrandingService } from '../api/generated';
import { lang } from '../i18n/i18n';

/** Brand used when the API cannot be reached: the application still starts. */
export const DEFAULT_BRAND: Branding = {
  productName: 'Lexora',
  accentColor: '#1d5fb4',
  accentDarkColor: '#164a8c',
  logoUrl: '/api/branding/logo',
  logoIncludesName: false,
  faviconUrl: '/api/branding/favicon',
};

/**
 * Brand of the installation (name, tagline, colours, logo, favicon), set by the API's deployment settings and
 * loaded once before the first page is shown.
 */
@Injectable({ providedIn: 'root' })
export class BrandStore {
  private readonly api = inject(BrandingService);
  private readonly document = inject(DOCUMENT);

  readonly brand = signal<Branding>(DEFAULT_BRAND);
  /** The office name, or the product name. */
  readonly name = computed(() => this.brand().officeName || this.brand().productName);
  /** Tagline in the current language; the French one when no English one is set. */
  readonly tagline = computed(() => {
    const b = this.brand();
    return (lang() === 'en' ? b.taglineEn || b.taglineFr : b.taglineFr) ?? '';
  });

  async load(): Promise<void> {
    try {
      this.apply(await firstValueFrom(this.api.getBranding().pipe(timeout(5000))));
    } catch {
      this.apply(DEFAULT_BRAND);
    }
  }

  apply(brand: Branding): void {
    this.brand.set(brand);
    const root = this.document.documentElement.style;
    root.setProperty('--accent', brand.accentColor);
    root.setProperty('--accent-dark', brand.accentDarkColor);
    const icon = this.document.querySelector<HTMLLinkElement>('link[rel="icon"]');
    if (icon) {
      icon.href = brand.faviconUrl;
    }
  }
}
