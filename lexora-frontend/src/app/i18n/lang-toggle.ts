import { Component } from '@angular/core';
import { lang, Lang, setLang, texts } from './i18n';

/** FR | EN switch of the interface language. */
@Component({
  selector: 'lx-lang-toggle',
  template: `
    <div class="toggle" role="group" [attr.aria-label]="t().common.language">
      @for (l of langs; track l) {
        <button type="button" [class.on]="lang() === l" [attr.aria-pressed]="lang() === l" [attr.lang]="l" [title]="names[l]" (click)="choose(l)">
          {{ l.toUpperCase() }}
        </button>
      }
    </div>
  `,
  styles: `
    .toggle { display: inline-flex; border: 1px solid var(--border); border-radius: var(--radius); overflow: hidden; background: var(--surface); }
    button { min-width: 38px; height: 34px; padding: 0 8px; border: 0; background: none; color: var(--muted); font-size: 12px; font-weight: 600; cursor: pointer; }
    button + button { border-left: 1px solid var(--border); }
    button.on { background: var(--tint); color: var(--accent-dark); }
  `,
})
export class LangToggle {
  protected readonly lang = lang;
  protected readonly t = texts;
  protected readonly langs: Lang[] = ['fr', 'en'];
  protected readonly names: Record<Lang, string> = { fr: 'Français', en: 'English' };

  protected choose(value: Lang): void {
    setLang(value);
  }
}
