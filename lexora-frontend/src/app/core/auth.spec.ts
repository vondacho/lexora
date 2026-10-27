import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { provideApi } from '../api/generated';
import { authInterceptor } from './auth.interceptor';
import { AuthService, basicToken } from './auth.service';

describe('AuthService and authInterceptor', () => {
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideApi({ basePath: '' }),
      ],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('encodes the credentials in UTF-8', () => {
    expect(basicToken('demo', 'demo')).toBe('ZGVtbzpkZW1v');
    expect(basicToken('zoé', 'é')).toBe(btoa('zoÃ©:Ã©'));
  });

  it('sends the credentials to /api/me and keeps them when accepted', () => {
    let name = '';
    auth.login('demo', 'demo').subscribe((user) => (name = user.username ?? ''));

    const req = http.expectOne('/api/me');
    expect(req.request.headers.get('Authorization')).toBe('Basic ZGVtbzpkZW1v');
    expect(req.request.headers.get('X-Requested-With')).toBe('XMLHttpRequest');
    req.flush({ username: 'demo', grants: ['invoicing:debtors_open'] });

    expect(name).toBe('demo');
    expect(auth.loggedIn()).toBe(true);
    expect(auth.hasGrant('invoicing:debtors_open')).toBe(true);
    expect(auth.hasGrant('admin:users')).toBe(false);
    expect(sessionStorage.getItem('lexora.auth')).toBe('ZGVtbzpkZW1v');
  });

  it('forgets rejected credentials', () => {
    let failed = false;
    auth.login('demo', 'wrong').subscribe({ error: () => (failed = true) });
    http.expectOne('/api/me').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(failed).toBe(true);
    expect(auth.loggedIn()).toBe(false);
    expect(auth.authorization()).toBeNull();
    expect(sessionStorage.getItem('lexora.auth')).toBeNull();
  });

  it('ends the session and goes to the login page on a 401 during the session', () => {
    auth.login('demo', 'demo').subscribe();
    http.expectOne('/api/me').flush({ username: 'demo' });
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    TestBed.inject(HttpClient).get('/api/debtors/overview').subscribe({ error: () => undefined });
    http.expectOne('/api/debtors/overview').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(auth.loggedIn()).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/connexion'], { queryParams: { retour: '/' } });
  });

  it('restores the session from the stored credentials after a reload', () => {
    sessionStorage.setItem('lexora.auth', 'ZGVtbzpkZW1v');
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting(), provideApi({ basePath: '' })],
    });
    const restored = TestBed.inject(AuthService);
    const controller = TestBed.inject(HttpTestingController);
    let ok = false;
    restored.restore().subscribe((value) => (ok = value));
    const req = controller.expectOne('/api/me');
    expect(req.request.headers.get('Authorization')).toBe('Basic ZGVtbzpkZW1v');
    req.flush({ username: 'demo' });
    expect(ok).toBe(true);
    controller.verify();
    http = controller;
  });

  it('leaves the other requests alone', () => {
    TestBed.inject(HttpClient).get('/assets/x.json').subscribe();
    const req = http.expectOne('/assets/x.json');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });
});
