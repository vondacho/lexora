import { Component, computed, ElementRef, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged, of, switchMap } from 'rxjs';
import { Folder, FoldersService, SearchFoldersRequestParams } from '../../api/generated';
import { loadable, Loadable } from './home.model';
import { texts } from '../../i18n/i18n';

type Criterion = 'name' | 'number' | 'customer';

const CRITERIA: Criterion[] = ['name', 'number', 'customer'];
const SHOWN = 8;

@Component({
  selector: 'mbj-search-widget',
  imports: [RouterLink],
  template: `
    <section class="card widget" aria-labelledby="search-title">
      <div class="widget-head">
        <div>
          <h2 id="search-title">{{ tx().home.search.title }}</h2>
          <p class="muted">{{ tx().home.search.subtitle }}</p>
        </div>
      </div>
      <div class="criteria" role="group" [attr.aria-label]="tx().home.search.by">
        @for (c of criteria; track c) {
          <button type="button" class="chip" [class.on]="criterion() === c" [attr.aria-pressed]="criterion() === c" (click)="criterion.set(c)">
            {{ tx().home.search.criteria[c] }}
          </button>
        }
      </div>
      <div class="field">
        <label for="folder-query">{{ tx().home.search.labels[criterion()] }}</label>
        <input #queryInput id="folder-query" class="input" type="search" autocomplete="off" [attr.inputmode]="criterion() === 'number' ? 'numeric' : null" [placeholder]="tx().home.search.placeholders[criterion()]" [value]="query()" (input)="query.set($any($event.target).value)" />
      </div>
      @switch (results().state) {
        @case ('loading') {
          <p class="empty">{{ tx().home.search.searching }}</p>
        }
        @case ('error') {
          <p class="problem">{{ tx().home.search.failed }}</p>
        }
        @case ('ready') {
          @if (found(); as folders) {
            @if (folders.length === 0) {
              <p class="empty">{{ tx().home.search.none }}</p>
            } @else {
              <ul class="rows">
                @for (f of folders.slice(0, shown); track f.id) {
                  <li>
                    <span class="number mono">#{{ f.number ?? f.id }}</span>
                    <div class="row-main">
                      <a [routerLink]="['/dossiers', f.id]">{{ f.name }}</a>
                      <span class="row-sub">{{ subtitle(f) }}</span>
                    </div>
                    @if (f.status) {
                      <span class="pill">{{ f.status }}</span>
                    }
                  </li>
                }
              </ul>
              <p class="widget-foot muted">
                {{ tx().home.search.count(folders.length, shown) }}
              </p>
            }
          } @else {
            <p class="empty">{{ tx().home.search.hint }}</p>
          }
        }
      }
    </section>
  `,
  styles: `
    .criteria { display: flex; flex-wrap: wrap; gap: 8px; }
    .chip { min-height: 36px; padding: 0 14px; border: 1px solid var(--border); border-radius: 999px; background: var(--surface); cursor: pointer; }
    .chip.on { border-color: var(--accent); background: var(--accent); color: #fff; font-weight: 600; }
    .number { width: 52px; color: var(--muted); font-size: 13px; }
  `,
})
export class SearchWidget {
  private readonly folders = inject(FoldersService);
  private readonly input = viewChild.required<ElementRef<HTMLInputElement>>('queryInput');

  protected readonly tx = texts;
  protected readonly criteria = CRITERIA;
  protected readonly shown = SHOWN;
  protected readonly criterion = signal<Criterion>('name');
  protected readonly query = signal('');

  private readonly search = computed(() => ({ criterion: this.criterion(), query: this.query().trim() }));

  protected readonly results = toSignal(
    toObservable(this.search).pipe(
      debounceTime(250),
      distinctUntilChanged((a, b) => a.criterion === b.criterion && a.query === b.query),
      switchMap(({ criterion, query }) => {
        if (!query) {
          // without a criterion the API returns every folder: wait for the first letters
          return of<Loadable<Folder[] | null>>({ state: 'ready', value: null });
        }
        const params: SearchFoldersRequestParams = { [criterion]: query };
        return loadable<Folder[] | null>(this.folders.searchFolders(params));
      }),
    ),
    { initialValue: { state: 'ready', value: null } as Loadable<Folder[] | null> },
  );

  protected readonly found = computed(() => {
    const results = this.results();
    return results.state === 'ready' ? results.value : null;
  });

  constructor() {
    // the header's search link opens the home page at #recherche
    inject(ActivatedRoute)
      .fragment.pipe(takeUntilDestroyed())
      .subscribe((fragment) => {
        if (fragment === 'recherche') {
          setTimeout(() => {
            const input = this.input().nativeElement;
            input.scrollIntoView({ block: 'center' });
            input.focus();
          });
        }
      });
  }

  protected subtitle(folder: Folder): string {
    const parts = [];
    const t = texts().common;
    if (folder.type === 'TUTELAGE') {
      parts.push(t.tutelage);
    } else if (folder.type === 'INVOICING') {
      parts.push(t.invoicing);
    }
    if (folder.handledBy) {
      parts.push(t.handledBy(folder.handledBy));
    }
    return parts.join(' · ');
  }
}
