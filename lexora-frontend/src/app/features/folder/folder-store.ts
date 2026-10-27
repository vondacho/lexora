import { inject, Injectable, signal } from '@angular/core';
import { catchError, firstValueFrom, forkJoin, map, Observable, of, throwError } from 'rxjs';
import {
  AdvanceRequestsService,
  AdvancesService,
  CustomersService,
  FoldersService,
  HonorariesService,
  InvoicesService,
  InvoicingSettings,
  SettingsService,
  TemplatesService,
  Wording,
} from '../../api/generated';
import { HttpErrorResponse } from '@angular/common/http';
import { FolderData } from './timeline.model';

export type FolderState =
  | { state: 'loading' }
  | { state: 'ready'; data: FolderData }
  | { state: 'missing' }
  | { state: 'error' };

/** Tariffs used when the invoicing settings cannot be read. */
const DEFAULT_SETTINGS: InvoicingSettings = { meetingHourlyRate: 200, standardHourlyRate: 150, vatRatePercent: 8.1 };

/** Data of the folder screen, reloaded after each change. Provided by the folder page. */
@Injectable()
export class FolderStore {
  private readonly folders = inject(FoldersService);
  private readonly customers = inject(CustomersService);
  private readonly honoraries = inject(HonorariesService);
  private readonly invoices = inject(InvoicesService);
  private readonly requests = inject(AdvanceRequestsService);
  private readonly advances = inject(AdvancesService);

  readonly state = signal<FolderState>({ state: 'loading' });
  /** True while a reload runs; the previous data stays on screen meanwhile. */
  readonly refreshing = signal(false);
  readonly settings = signal<InvoicingSettings>(DEFAULT_SETTINGS);
  readonly wordings = signal<Wording[]>([]);

  private folderId = 0;

  constructor() {
    inject(SettingsService)
      .getInvoicingSettings()
      .pipe(catchError(() => of(DEFAULT_SETTINGS)))
      .subscribe((settings) => this.settings.set(settings));
    inject(TemplatesService)
      .listWordings()
      .pipe(catchError(() => of([] as Wording[])))
      .subscribe((wordings) => this.wordings.set(wordings));
  }

  load(folderId: number): void {
    this.folderId = folderId;
    this.state.set({ state: 'loading' });
    void this.reload();
  }

  /** Reloads everything; resolves once the screen shows the new data. */
  async reload(): Promise<void> {
    const id = this.folderId;
    this.refreshing.set(true);
    const next = await firstValueFrom(
      forkJoin({
        folder: this.folders.getFolder({ id }),
        customers: orEmpty(this.customers.listCustomers({ folderId: id })),
        honoraries: orEmpty(this.honoraries.listHonoraries({ folderId: id })),
        invoices: orEmpty(this.invoices.listFolderInvoices({ folderId: id })),
        requests: orEmpty(this.requests.listFolderAdvanceRequests({ folderId: id })),
        advances: orEmpty(this.advances.listAdvances({ folderId: id })),
      }).pipe(
        map((data): FolderState => ({ state: 'ready', data })),
        catchError((e: unknown) =>
          of<FolderState>(e instanceof HttpErrorResponse && (e.status === 404 || e.status === 400) ? { state: 'missing' } : { state: 'error' }),
        ),
      ),
    );
    if (id === this.folderId) {
      this.state.set(next);
      this.refreshing.set(false);
    }
  }
}

/** A list the user may not read (403) is shown empty rather than failing the whole folder. */
function orEmpty<T>(list: Observable<T[]>): Observable<T[]> {
  return list.pipe(
    catchError((e: unknown) => (e instanceof HttpErrorResponse && e.status === 403 ? of([] as T[]) : throwError(() => e))),
  );
}
