import { afterEach, describe, expect, it } from 'vitest';
import { lang, setLang, texts } from './i18n';

describe('interface language', () => {
  afterEach(() => setLang('fr'));

  it('switches every text, remembers the choice and sets the page language', () => {
    expect(texts().shell.nav.todo).toBe('À traiter');
    setLang('en');
    expect(lang()).toBe('en');
    expect(texts().shell.nav.todo).toBe('To do');
    expect(localStorage.getItem('lexora.lang')).toBe('en');
    expect(document.documentElement.lang).toBe('en');
  });

  it('follows the plural rule of each language (zero is singular in French, plural in English)', () => {
    expect(texts().folder.page.openSessions(0)).toBe('0 séance ouverte');
    expect(texts().folder.page.openSessions(2)).toBe('2 séances ouvertes');
    setLang('en');
    expect(texts().folder.page.openSessions(0)).toBe('0 open sessions');
    expect(texts().folder.page.openSessions(1)).toBe('1 open session');
  });

  it('elides "de" before a vowel in French only', () => {
    expect(texts().common.ofMonth('octobre', 2026)).toBe('d’octobre 2026');
    expect(texts().common.ofMonth('mars', 2026)).toBe('de mars 2026');
    setLang('en');
    expect(texts().common.ofMonth('October', 2026)).toBe('of October 2026');
  });
});
