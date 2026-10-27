import { computed, signal } from '@angular/core';
import { en } from './messages.en';
import { fr, Messages } from './messages.fr';

export type Lang = 'fr' | 'en';
export type { Messages };

const STORAGE_KEY = 'mabecoju.lang';

function initialLang(): Lang {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored === 'fr' || stored === 'en') {
      return stored;
    }
  } catch {
    // storage unavailable
  }
  // the office works in French: English only when chosen
  return 'fr';
}

/** The language of the interface; French by default (the office's language), English on demand. */
export const lang = signal<Lang>(initialLang());

/**
 * Texts of the current language. Read it in templates, computed values and the rules that build texts: they follow
 * the language as soon as it changes.
 */
export const texts = computed<Messages>(() => (lang() === 'en' ? en : fr));

/** Locale for Intl formatting (long dates); amounts and short dates keep the Swiss notation in both languages. */
export const locale = computed(() => (lang() === 'en' ? 'en-GB' : 'fr-CH'));

export function setLang(value: Lang): void {
  lang.set(value);
  try {
    localStorage.setItem(STORAGE_KEY, value);
  } catch {
    // the choice lasts until the next reload
  }
  if (typeof document !== 'undefined') {
    document.documentElement.lang = value;
  }
}

/** "lundi 26 octobre 2026" / "Monday 26 October 2026" */
export function longDate(date: Date): string {
  return new Intl.DateTimeFormat(locale(), { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).format(date);
}

/** Month names of the current language, January first. */
export function monthNames(): string[] {
  return texts().common.months;
}
