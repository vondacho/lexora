import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideApi } from '../api/generated';
import { setLang } from '../i18n/i18n';
import { BrandStore, DEFAULT_BRAND } from './brand';

describe('BrandStore', () => {
  let store: BrandStore;
  let http: HttpTestingController;

  beforeEach(() => {
    document.head.querySelector('link[rel="icon"]')?.remove();
    const icon = document.createElement('link');
    icon.rel = 'icon';
    icon.href = '/api/branding/favicon';
    document.head.appendChild(icon);
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideApi({ basePath: '' })],
    });
    store = TestBed.inject(BrandStore);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    document.documentElement.removeAttribute('style');
    setLang('fr');
  });

  it("applies the installation's name, colours and favicon", async () => {
    const loading = store.load();
    http.expectOne('/api/branding').flush({
      ...DEFAULT_BRAND,
      officeName: 'Étude Exemple',
      taglineFr: 'juriste & médiatrice',
      accentColor: '#7a1f5c',
      accentDarkColor: '#5f1848',
      faviconUrl: '/api/branding/favicon?v=12ab',
    });
    await loading;

    expect(store.name()).toBe('Étude Exemple');
    expect(document.documentElement.style.getPropertyValue('--accent')).toBe('#7a1f5c');
    expect(document.documentElement.style.getPropertyValue('--accent-dark')).toBe('#5f1848');
    expect(document.querySelector<HTMLLinkElement>('link[rel="icon"]')!.getAttribute('href')).toBe('/api/branding/favicon?v=12ab');
  });

  it('shows the French tagline in English when there is no English one', async () => {
    const loading = store.load();
    http.expectOne('/api/branding').flush({ ...DEFAULT_BRAND, taglineFr: 'juriste & médiatrice' });
    await loading;
    setLang('en');
    expect(store.tagline()).toBe('juriste & médiatrice');
    store.apply({ ...DEFAULT_BRAND, taglineFr: 'juriste & médiatrice', taglineEn: 'lawyer & mediator' });
    expect(store.tagline()).toBe('lawyer & mediator');
  });

  it('falls back to the Lexora brand when the API cannot be reached', async () => {
    const loading = store.load();
    http.expectOne('/api/branding').flush(null, { status: 502, statusText: 'Bad Gateway' });
    await loading;
    expect(store.name()).toBe('Lexora');
    expect(store.tagline()).toBe('');
    expect(document.documentElement.style.getPropertyValue('--accent')).toBe('#1d5fb4');
  });
});
