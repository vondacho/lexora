import { Component, computed, ElementRef, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';
import { catchError, debounceTime, distinctUntilChanged, filter, forkJoin, map, Observable, of, switchMap } from 'rxjs';
import {
  AdvanceRequest,
  AdvanceRequestsService,
  DebtorsService,
  Folder,
  FoldersService,
  Invoice,
  InvoicesService,
} from '../api/generated';
import { AuthService } from '../core/auth.service';
import { loadable, Loadable } from '../features/home/home.model';
import { texts } from '../i18n/i18n';
import { formatChf } from '../shared/format';
import { NAV_ITEMS } from './navigation';
import { matchScreens, mergeFolders, parseQuery, SearchQuery } from './global-search.model';

type Group = 'documents' | 'folders' | 'screens';

interface Hit {
  id: string;
  group: Group;
  label: string;
  sub: string;
  link: (string | number)[];
  /** Timeline item to select in the folder (f12, p3). */
  element?: string;
}

const GROUPS: Group[] = ['documents', 'folders', 'screens'];

/**
 * Search field of the header: folders by number, name or client, invoices and advance requests by reference
 * (F-12, P-3), payment slips by their reference, and screens by name. Keyboard: / or Ctrl+K to focus, arrows to
 * move, Enter to open, Escape to clear. On phones it opens from an icon over the header.
 */
@Component({
  selector: 'lx-global-search',
  host: {
    '(document:keydown)': 'shortcut($event)',
    '[class.open]': 'phoneOpen()',
  },
  template: `
    <button type="button" class="icon-btn trigger" (click)="openOnPhone()" [attr.aria-label]="tx().shell.globalSearch.open" [title]="tx().shell.globalSearch.open">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="11" cy="11" r="7"></circle><path d="m20 20-3.5-3.5"></path></svg>
    </button>
    <div class="box">
      <svg class="glass" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="11" cy="11" r="7"></circle><path d="m20 20-3.5-3.5"></path></svg>
      <input
        #input
        type="search"
        role="combobox"
        autocomplete="off"
        spellcheck="false"
        aria-autocomplete="list"
        aria-controls="global-search-list"
        [attr.aria-expanded]="listShown()"
        [attr.aria-activedescendant]="activeHit() ? 'gs-' + activeHit()!.id : null"
        [attr.aria-label]="tx().shell.globalSearch.label"
        [placeholder]="tx().shell.searchPlaceholder"
        [value]="query()"
        (input)="type($any($event.target).value)"
        (keydown)="key($event)"
        (focus)="focused.set(true)"
        (blur)="blurred()"
      />
      @if (!query()) {
        <kbd class="hint" [title]="tx().shell.globalSearch.shortcut">/</kbd>
      }
    </div>
    <div id="global-search-list" class="list" role="listbox" [attr.aria-label]="tx().shell.globalSearch.label" [hidden]="!listShown()">
      @for (group of groups(); track group.key) {
        <div role="group" [attr.aria-labelledby]="'gs-group-' + group.key">
          <div class="group" [id]="'gs-group-' + group.key" role="presentation">{{ tx().shell.globalSearch.groups[group.key] }}</div>
          @for (hit of group.hits; track hit.id) {
            <div
              role="option"
              class="option"
              [id]="'gs-' + hit.id"
              [class.active]="activeHit()?.id === hit.id"
              [attr.aria-selected]="activeHit()?.id === hit.id"
              (mousedown)="$event.preventDefault()"
              (mousemove)="activate(hit)"
              (click)="go(hit)"
            >
              <span class="label">{{ hit.label }}</span>
              <span class="sub">{{ hit.sub }}</span>
            </div>
          }
        </div>
      }
      @if (statusText(); as status) {
        <p class="status">{{ status }}</p>
      }
    </div>
    <span class="sr-only" aria-live="polite">{{ announcement() }}</span>
  `,
  styleUrl: './global-search.css',
})
export class GlobalSearch {
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly folders = inject(FoldersService);
  private readonly invoices = inject(InvoicesService);
  private readonly requests = inject(AdvanceRequestsService);
  private readonly debtors = inject(DebtorsService);
  private readonly input = viewChild.required<ElementRef<HTMLInputElement>>('input');

  protected readonly tx = texts;
  protected readonly query = signal('');
  protected readonly focused = signal(false);
  protected readonly phoneOpen = signal(false);
  private readonly activeIndex = signal(0);

  private readonly can = {
    folders: this.auth.hasGrant('invoicing:folder_search'),
    invoices: this.auth.hasGrant('invoicing:invoice_display'),
    requests: this.auth.hasGrant('invoicing:request_display'),
    slips: this.auth.hasGrant('invoicing:debtor_search'),
  };

  private readonly parsed = computed(() => parseQuery(this.query()));

  private readonly screens = computed<Hit[]>(() => {
    const t = this.tx();
    const screens = NAV_ITEMS.map((item) => ({ path: item.path, label: t.shell.nav[item.key] }));
    return matchScreens(this.parsed().text, screens).map((s) => ({
      id: 'screen' + s.path.replace(/\W/g, '-'),
      group: 'screens' as const,
      label: s.label,
      sub: t.shell.globalSearch.screen,
      link: [s.path],
    }));
  });

  private readonly remote = toSignal(
    toObservable(this.parsed).pipe(
      debounceTime(220),
      distinctUntilChanged((a, b) => a.text === b.text),
      switchMap((q) => (this.searchable(q) ? loadable(this.searchRemote(q)) : of<Loadable<Hit[]>>({ state: 'ready', value: [] }))),
    ),
    { initialValue: { state: 'ready', value: [] } as Loadable<Hit[]> },
  );

  /** Results in display order: documents, folders, screens. */
  protected readonly hits = computed<Hit[]>(() => {
    const remote = this.remote();
    const found = remote.state === 'ready' ? remote.value : [];
    return GROUPS.flatMap((g) => [...found, ...this.screens()].filter((h) => h.group === g));
  });

  protected readonly groups = computed(() =>
    GROUPS.map((key) => ({ key, hits: this.hits().filter((h) => h.group === key) })).filter((g) => g.hits.length),
  );

  protected readonly activeHit = computed(() => this.hits()[Math.min(this.activeIndex(), this.hits().length - 1)] ?? null);

  protected readonly statusText = computed(() => {
    const t = this.tx().shell.globalSearch;
    const remote = this.remote();
    if (remote.state === 'loading') {
      return t.searching;
    }
    if (remote.state === 'error') {
      return t.failed;
    }
    return this.hits().length ? null : t.none;
  });

  protected readonly listShown = computed(() => this.focused() && this.query().trim().length > 0);

  protected readonly announcement = computed(() => {
    if (!this.listShown() || this.remote().state === 'loading') {
      return '';
    }
    return this.statusText() ?? this.tx().shell.globalSearch.results(this.hits().length);
  });

  constructor() {
    this.router.events
      .pipe(
        filter((e) => e instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.reset());
  }

  protected type(value: string): void {
    this.query.set(value);
    this.activeIndex.set(0);
  }

  protected key(event: KeyboardEvent): void {
    const count = this.hits().length;
    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        if (count) this.activeIndex.set((this.activeIndex() + 1) % count);
        break;
      case 'ArrowUp':
        event.preventDefault();
        if (count) this.activeIndex.set((this.activeIndex() - 1 + count) % count);
        break;
      case 'Enter': {
        event.preventDefault();
        const hit = this.activeHit();
        if (hit) this.go(hit);
        break;
      }
      case 'Escape':
        event.preventDefault();
        if (this.query()) {
          this.type('');
        } else {
          this.input().nativeElement.blur();
        }
        break;
    }
  }

  protected activate(hit: Hit): void {
    this.activeIndex.set(this.hits().indexOf(hit));
  }

  protected go(hit: Hit): void {
    void this.router.navigate(hit.link, { queryParams: hit.element ? { element: hit.element } : {} });
    this.input().nativeElement.blur();
  }

  protected blurred(): void {
    this.focused.set(false);
    if (!this.query()) {
      this.phoneOpen.set(false);
    }
  }

  protected openOnPhone(): void {
    this.phoneOpen.set(true);
    setTimeout(() => this.input().nativeElement.focus());
  }

  /** "/" or Ctrl+K (⌘K) anywhere outside a field focuses the search. */
  protected shortcut(event: KeyboardEvent): void {
    const target = event.target as HTMLElement | null;
    const typing = !!target && (/^(INPUT|TEXTAREA|SELECT)$/.test(target.tagName) || target.isContentEditable);
    const slash = event.key === '/' && !typing && !event.ctrlKey && !event.metaKey && !event.altKey;
    const k = event.key.toLowerCase() === 'k' && (event.ctrlKey || event.metaKey);
    if (slash || k) {
      event.preventDefault();
      this.openOnPhone();
    }
  }

  private reset(): void {
    this.query.set('');
    this.activeIndex.set(0);
    this.phoneOpen.set(false);
  }

  private searchable(q: SearchQuery): boolean {
    return !!q.document || !!q.slip || q.number !== undefined || q.words;
  }

  private searchRemote(q: SearchQuery): Observable<Hit[]> {
    const calls: Observable<Hit[]>[] = [];
    if (q.document) {
      calls.push(this.documentHits(q.document));
    }
    if (q.slip && this.can.slips) {
      calls.push(this.slipHits(q.slip));
    }
    if (this.can.folders && (q.number !== undefined || q.words)) {
      const none = of<Folder[]>([]);
      calls.push(
        forkJoin([
          q.number !== undefined ? this.folders.searchFolders({ number: String(q.number) }) : none,
          q.words ? this.folders.searchFolders({ name: q.text }) : none,
          q.words ? this.folders.searchFolders({ customer: q.text }) : none,
        ]).pipe(map(([byNumber, byName, byCustomer]) => this.folderHits(mergeFolders(byNumber, byName, byCustomer)))),
      );
    }
    return calls.length ? forkJoin(calls).pipe(map((lists) => lists.flat())) : of([]);
  }

  private folderHits(matches: ReturnType<typeof mergeFolders>): Hit[] {
    const t = this.tx().shell.globalSearch;
    return matches.map(({ folder, via }) => ({
      id: 'folder' + folder.id,
      group: 'folders',
      label: folder.name ?? '#' + folder.id,
      sub: ['#' + (folder.number ?? folder.id), folder.status, via === 'customer' ? t.viaCustomer : null]
        .filter(Boolean)
        .join(' · '),
      link: ['/dossiers', folder.id],
    }));
  }

  /** An unknown or forbidden reference is simply not found. */
  private documentHits(doc: NonNullable<SearchQuery['document']>): Observable<Hit[]> {
    const invoice = doc.kind === 'invoice';
    if (!(invoice ? this.can.invoices : this.can.requests)) {
      return of([]);
    }
    const source: Observable<Invoice | AdvanceRequest> = invoice
      ? this.invoices.getInvoice({ id: doc.id })
      : this.requests.getAdvanceRequest({ id: doc.id });
    return source.pipe(
      switchMap((d) =>
        this.folderName(d.folderId).pipe(
          map((name): Hit[] => {
            const t = this.tx().shell.globalSearch;
            const reference = (invoice ? 'F-' : 'P-') + d.id;
            const state = d.paid ? t.paid : t.remaining(formatChf(d.remainingSum));
            return [
              {
                id: 'doc' + reference,
                group: 'documents',
                label: name ? `${reference} · ${name}` : reference,
                sub: [invoice ? t.invoice : t.request, formatChf(d.amount), state].join(' · '),
                link: ['/dossiers', d.folderId!],
                element: (invoice ? 'f' : 'p') + d.id,
              },
            ];
          }),
        ),
      ),
      catchError(() => of([])),
    );
  }

  private slipHits(reference: string): Observable<Hit[]> {
    return this.debtors.lookupTicket({ reference }).pipe(
      map((slip): Hit[] => {
        if (!slip.folderId) {
          return [];
        }
        const element = slip.invoiceId ? 'f' + slip.invoiceId : slip.requestId ? 'p' + slip.requestId : undefined;
        return [
          {
            id: 'slip' + (slip.ticketId ?? reference),
            group: 'documents',
            label: [element ? element.replace(/^f/, 'F-').replace(/^p/, 'P-') : null, slip.folderName].filter(Boolean).join(' · '),
            sub: [this.tx().shell.globalSearch.slip(slip.number ?? reference), slip.customerName].filter(Boolean).join(' · '),
            link: ['/dossiers', slip.folderId],
            element,
          },
        ];
      }),
      catchError(() => of([])),
    );
  }

  private folderName(id: number | undefined): Observable<string | null> {
    if (!id) {
      return of(null);
    }
    return this.folders.getFolder({ id }).pipe(
      map((f) => f.name ?? null),
      catchError(() => of(null)),
    );
  }
}
