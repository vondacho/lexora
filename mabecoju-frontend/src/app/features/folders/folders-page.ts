import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { catchError, firstValueFrom, of } from 'rxjs';
import { CustomerInput, Folder, FolderInput, FoldersService, FolderStatus } from '../../api/generated';
import { AuthService } from '../../core/auth.service';
import { SwissDatePipe } from '../../shared/format';
import { todayIso } from '../../shared/money';
import { problemMessage } from '../../shared/problem';
import { cleaned, CustomerFields, FolderFields, hasName } from './folder-fields';
import { distinct, FolderFilter, filterFolders, page, Sort, sortFolders } from './folders.model';

const PAGE_SIZE = 50;

/** "Dossiers": the register of folders, and the opening of a new one with its first client. */
@Component({
  selector: 'mbj-folders-page',
  imports: [FormsModule, RouterLink, SwissDatePipe, FolderFields, CustomerFields],
  templateUrl: './folders-page.html',
  styleUrl: './folders-page.css',
})
export class FoldersPage {
  private readonly folders = inject(FoldersService);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  protected readonly canCreate = this.auth.hasGrant('invoicing:folder_create');

  protected readonly state = signal<'loading' | 'ready' | 'error'>('loading');
  protected readonly all = signal<Folder[]>([]);
  protected readonly statuses = signal<string[]>([]);

  protected readonly filter = signal<FolderFilter>({ text: '', type: '', status: '', handledBy: '' });
  protected readonly sort = signal<Sort>('name');
  protected readonly pageIndex = signal(0);
  /** Folders found by a client's name (the API searches the customers). */
  protected readonly byCustomer = signal<{ query: string; folders: Folder[] } | null>(null);

  protected readonly statusChoices = computed(() => [...new Set([...this.statuses(), ...distinct(this.all(), 'status')])].sort());
  protected readonly handlers = computed(() => distinct(this.all(), 'handledBy'));
  protected readonly found = computed(() => sortFolders(filterFolders(this.byCustomer()?.folders ?? this.all(), this.filter()), this.sort()));
  protected readonly current = computed(() => page(this.found(), this.pageIndex(), PAGE_SIZE));

  // new folder
  protected readonly creating = signal(false);
  protected folder: FolderInput = this.emptyFolder();
  protected customer: CustomerInput = {};
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  constructor() {
    void this.load();
  }

  protected setFilter(patch: Partial<FolderFilter>): void {
    this.filter.update((f) => ({ ...f, ...patch }));
    this.pageIndex.set(0);
  }

  protected async searchCustomer(query: string): Promise<void> {
    const q = query.trim();
    if (!q) {
      this.byCustomer.set(null);
      return;
    }
    const folders = await firstValueFrom(this.folders.searchFolders({ customer: q }).pipe(catchError(() => of([] as Folder[]))));
    this.byCustomer.set({ query: q, folders });
    this.pageIndex.set(0);
  }

  protected startCreate(): void {
    this.folder = this.emptyFolder();
    this.customer = {};
    this.error.set(null);
    this.creating.set(true);
  }

  protected async create(): Promise<void> {
    if (this.busy()) return;
    if (!this.folder.name?.trim()) {
      this.error.set('Indiquez le nom du dossier.');
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      const opened = await firstValueFrom(
        this.folders.openFolder({
          openFolderInput: { folder: cleaned(this.folder), firstCustomer: hasName(this.customer) ? cleaned(this.customer) : undefined },
        }),
      );
      void this.router.navigate(['/dossiers', opened.folder?.id]);
    } catch (e: unknown) {
      this.error.set(problemMessage(e, 'Le dossier n’a pas pu être ouvert.'));
    } finally {
      this.busy.set(false);
    }
  }

  private emptyFolder(): FolderInput {
    const status = this.statuses().includes('En cours') ? 'En cours' : undefined;
    return { type: 'INVOICING', name: '', status, handledBy: this.auth.user()?.shortcut ?? undefined, openingDate: todayIso() };
  }

  private async load(): Promise<void> {
    try {
      const [folders, statuses] = await Promise.all([
        firstValueFrom(this.folders.searchFolders({})),
        firstValueFrom(this.folders.listFolderStatuses().pipe(catchError(() => of([] as FolderStatus[])))),
      ]);
      this.all.set(folders);
      this.statuses.set(statuses.map((s) => s.name ?? '').filter(Boolean));
      this.folder = this.emptyFolder();
      this.state.set('ready');
    } catch {
      this.state.set('error');
    }
  }
}
