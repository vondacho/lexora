import { effect, inject, Injectable } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';
import { texts } from './i18n';
import { NavKey } from '../shell/navigation';

/** Page titles from the route's key (data.titleKey), in the current language, updated when it changes. */
@Injectable({ providedIn: 'root' })
export class TranslatedTitleStrategy extends TitleStrategy {
  private readonly title = inject(Title);
  private key: NavKey | null = null;

  constructor() {
    super();
    effect(() => this.apply(texts()));
  }

  override updateTitle(snapshot: RouterStateSnapshot): void {
    let route = snapshot.root;
    let key: NavKey | null = null;
    while (route) {
      key = (route.data?.['titleKey'] as NavKey | undefined) ?? key;
      route = route.firstChild!;
    }
    this.key = key;
    this.apply(texts());
  }

  private apply(t: ReturnType<typeof texts>): void {
    this.title.setTitle(this.key ? `${t.shell.nav[this.key]} — mabecoju` : 'mabecoju');
  }
}
