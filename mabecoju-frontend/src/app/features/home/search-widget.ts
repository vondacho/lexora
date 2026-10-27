import { Component, computed, ElementRef, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged, of, switchMap } from 'rxjs';
import { Folder, FoldersService, SearchFoldersRequestParams } from '../../api/generated';
import { loadable, Loadable } from './home.model';

type Criterion = 'name' | 'number' | 'customer';

const CRITERIA: { value: Criterion; label: string; placeholder: string }[] = [
  { value: 'name', label: 'Nom', placeholder: 'ex. Dupont' },
  { value: 'number', label: 'N°', placeholder: 'ex. 1651' },
  { value: 'customer', label: 'Client', placeholder: 'prénom ou nom d’un client' },
];
const SHOWN = 8;

@Component({
  selector: 'mbj-search-widget',
  imports: [RouterLink],
  template: `
    <section class="card widget" aria-labelledby="search-title">
      <div class="widget-head">
        <div>
          <h2 id="search-title">Rechercher un dossier</h2>
          <p class="muted">Par nom, numéro ou client · le début suffit</p>
        </div>
      </div>
      <div class="criteria" role="group" aria-label="Rechercher par">
        @for (c of criteria; track c.value) {
          <button type="button" class="chip" [class.on]="criterion() === c.value" [attr.aria-pressed]="criterion() === c.value" (click)="criterion.set(c.value)">
            {{ c.label }}
          </button>
        }
      </div>
      <div class="field">
        <label for="folder-query">{{ current().label === 'N°' ? 'Numéro du dossier' : current().label === 'Nom' ? 'Nom du dossier' : 'Nom du client' }}</label>
        <input #queryInput id="folder-query" class="input" type="search" autocomplete="off" [attr.inputmode]="criterion() === 'number' ? 'numeric' : null" [placeholder]="current().placeholder" [value]="query()" (input)="query.set($any($event.target).value)" />
      </div>
      @switch (results().state) {
        @case ('loading') {
          <p class="empty">Recherche…</p>
        }
        @case ('error') {
          <p class="problem">La recherche n’a pas abouti.</p>
        }
        @case ('ready') {
          @if (found(); as folders) {
            @if (folders.length === 0) {
              <p class="empty">Aucun dossier ne correspond.</p>
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
                {{ folders.length }} dossier{{ folders.length > 1 ? 's' : '' }}{{ folders.length > shown ? ' · ' + shown + ' affichés' : '' }}
              </p>
            }
          } @else {
            <p class="empty">Tapez le début d’un nom ou d’un numéro.</p>
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

  protected readonly criteria = CRITERIA;
  protected readonly shown = SHOWN;
  protected readonly criterion = signal<Criterion>('name');
  protected readonly query = signal('');
  protected readonly current = computed(() => CRITERIA.find((c) => c.value === this.criterion())!);

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
    if (folder.type === 'TUTELAGE') {
      parts.push('Tutelle');
    } else if (folder.type === 'INVOICING') {
      parts.push('Facturation');
    }
    if (folder.handledBy) {
      parts.push('géré par ' + folder.handledBy);
    }
    return parts.join(' · ');
  }
}
