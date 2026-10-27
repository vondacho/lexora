import { Component, computed, inject, signal } from '@angular/core';
import { BrandStore } from './brand';

/**
 * Logo, name and tagline of the installation. The name is left out when the logo already shows it; the logo is
 * left out when it cannot be loaded. A parent hides the tagline with `--brand-tag-display: none`; the class
 * `stacked` puts the tagline under the name.
 */
@Component({
  selector: 'lx-brand',
  template: `
    @if (!logoFailed()) {
      <img class="logo" [class.wide]="includesName()" [src]="store.brand().logoUrl" [attr.alt]="includesName() ? store.name() : ''" (error)="logoFailed.set(true)" />
    }
    <span class="words">
      @if (!includesName() || logoFailed()) {
        <span class="name">{{ store.name() }}</span>
      }
      @if (store.tagline(); as tagline) {
        <span class="tag">{{ tagline }}</span>
      }
    </span>
  `,
  styles: `
    :host {
      display: inline-flex;
      align-items: center;
      gap: 10px;
      min-width: 0;
    }
    .logo {
      display: block;
      width: 28px;
      height: 28px;
      object-fit: contain;
      flex: none;
    }
    .logo.wide {
      width: auto;
      max-width: 180px;
    }
    .words {
      display: flex;
      align-items: baseline;
      gap: 10px;
      min-width: 0;
    }
    :host(.stacked) .words {
      flex-direction: column;
      gap: 0;
    }
    :host(.stacked) .tag {
      white-space: normal;
    }
    .name {
      font-weight: 600;
      font-size: 18px;
      letter-spacing: -0.01em;
      white-space: nowrap;
    }
    .tag {
      display: var(--brand-tag-display, inline);
      font-size: 12px;
      color: var(--muted);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  `,
})
export class BrandMark {
  protected readonly store = inject(BrandStore);
  protected readonly logoFailed = signal(false);
  protected readonly includesName = computed(() => this.store.brand().logoIncludesName);
}
