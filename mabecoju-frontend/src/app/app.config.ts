import { ApplicationConfig, LOCALE_ID, provideBrowserGlobalErrorListeners } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeFrCh from '@angular/common/locales/fr-CH';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { routes } from './app.routes';
import { provideApi } from './api/generated';
import { authInterceptor } from './core/auth.interceptor';

registerLocaleData(localeFrCh);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor])),
    // the contract's paths start with /api: same origin, proxied to the backend by `ng serve`
    provideApi({ basePath: '' }),
    { provide: LOCALE_ID, useValue: 'fr-CH' },
  ],
};
