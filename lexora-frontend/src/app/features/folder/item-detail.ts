import { Component, computed, inject, input, output, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { firstValueFrom, Observable, of, switchMap } from 'rxjs';
import {
  AdvanceRequest,
  AdvanceRequestsService,
  AdvancesService,
  DocumentModel,
  DocumentsService,
  DocumentType,
  HonorariesService,
  Invoice,
  InvoicesService,
  PrintInput,
  TemplatesService,
} from '../../api/generated';
import { AuthService } from '../../core/auth.service';
import { ACCEPT_PDF, openPdf } from '../../shared/download';
import { formatChf, formatDate } from '../../shared/format';
import { blobProblemMessage, problemMessage } from '../../shared/problem';
import { latestRecall } from '../../shared/recalls';
import { texts } from '../../i18n/i18n';
import { loadable, Loadable } from '../home/home.model';
import { customerName, FolderData, invoiceRef, meansText, requestRef, TimelineItem } from './timeline.model';

interface Fact {
  label: string;
  value: string;
}

interface Action {
  id: string;
  label: string;
  /** Asks for a second click before running. */
  confirm?: string;
  danger?: boolean;
  run: () => Observable<unknown>;
  /** Item to select once the folder is reloaded (e.g. the new reminder). */
  next?: (result: unknown) => string | null;
}

interface Printable {
  type: DocumentType;
  fileName: string;
  print: (input: PrintInput) => Observable<Blob>;
}

/** Facts and actions of the event selected in the timeline. */
@Component({
  selector: 'lx-item-detail',
  imports: [FormsModule],
  template: `
    <section class="card detail" aria-labelledby="detail-title">
      <div class="composer-head">
        <span class="muted kind">{{ kindLabel() }} · {{ item().date ? swiss(item().date) : '' }}</span>
        <button type="button" class="btn btn-small" (click)="closed.emit()" [attr.aria-label]="tx().folder.detail.closeDetail">{{ tx().common.close }}</button>
      </div>
      <h2 id="detail-title">{{ item().title }}</h2>
      <dl class="facts">
        @for (f of facts(); track f.label) {
          <dt>{{ f.label }}</dt>
          <dd>{{ f.value }}</dd>
        }
      </dl>

      @if (printable(); as p) {
        <div class="print">
          <span class="label">{{ tx().folder.detail.print }}</span>
          @switch (models().state) {
            @case ('loading') {
              <p class="note muted">{{ tx().folder.detail.readingModels }}</p>
            }
            @case ('error') {
              <p class="problem">{{ tx().folder.detail.modelsFailed }}</p>
            }
            @case ('ready') {
              @if (modelList().length === 0) {
                <p class="note muted">{{ tx().folder.detail.noModel }}</p>
              } @else {
                <div class="field">
                  <label for="print-model">{{ tx().folder.detail.model }}</label>
                  <select id="print-model" class="input" [ngModel]="modelId()" (ngModelChange)="chosenModel.set(+$event)">
                    @for (m of modelList(); track m.id) {
                      <option [value]="m.id">{{ m.name }}</option>
                    }
                  </select>
                </div>
                <label class="check">
                  <input type="checkbox" [(ngModel)]="mergeAddresses" />
                  {{ tx().folder.detail.merge }}
                </label>
                <button type="button" class="btn btn-primary" [disabled]="printing()" (click)="print(p)">
                  {{ printing() ? tx().folder.detail.preparingPdf : tx().folder.detail.openPdf }}
                </button>
              }
            }
          }
        </div>
      }

      @if (actions().length) {
        <div class="actions">
          @for (a of actions(); track a.id) {
            <button type="button" class="btn" [class.btn-warn]="a.danger" [disabled]="busy()" (click)="runAction(a)">
              {{ confirming() === a.id ? a.confirm : a.label }}
            </button>
          }
        </div>
      }
      @if (error(); as message) {
        <p class="problem" role="alert">{{ message }}</p>
      }
    </section>
  `,
  styleUrl: './composer.css',
  styles: `
    .detail { display: flex; flex-direction: column; gap: 14px; }
    .kind { font-size: 12px; }
    .print { display: flex; flex-direction: column; gap: 8px; padding-top: 12px; border-top: 1px solid var(--border); }
    .check { display: flex; align-items: center; gap: 8px; font-size: 13px; }
  `,
})
export class ItemDetail {
  private readonly auth = inject(AuthService);
  private readonly documents = inject(DocumentsService);
  private readonly templates = inject(TemplatesService);
  private readonly honoraries = inject(HonorariesService);
  private readonly invoices = inject(InvoicesService);
  private readonly requests = inject(AdvanceRequestsService);
  private readonly advances = inject(AdvancesService);

  readonly data = input.required<FolderData>();
  readonly item = input.required<TimelineItem>();
  /** Something changed: reload the folder, then select this key (null: clear the selection). */
  readonly changed = output<string | null>();
  readonly closed = output<void>();

  protected readonly busy = signal(false);
  protected readonly printing = signal(false);
  protected readonly confirming = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly tx = texts;
  protected mergeAddresses = false;
  protected readonly swiss = (iso: string) => formatDate(iso);

  private readonly names = computed(() => new Map(this.data().customers.map((c) => [c.id, customerName(c)])));
  private readonly invoice = computed(() => {
    const ref = this.item().ref;
    const id = ref.kind === 'invoice' ? ref.id : ref.kind === 'payment' || ref.kind === 'invoice-recall' ? ref.invoiceId : null;
    return id === null ? null : this.data().invoices.find((i) => i.id === id) ?? null;
  });
  private readonly request = computed(() => {
    const ref = this.item().ref;
    const id = ref.kind === 'request' ? ref.id : ref.kind === 'request-recall' ? ref.requestId : null;
    return id === null ? null : this.data().requests.find((r) => r.id === id) ?? null;
  });

  protected readonly kindLabel = computed(() => {
    return texts().folder.detail.kinds[this.item().ref.kind];
  });

  protected readonly facts = computed<Fact[]>(() => {
    const data = this.data();
    const ref = this.item().ref;
    const t = texts().folder.detail;
    const name = (id?: number) => this.names().get(id ?? -1) ?? texts().folder.timeline.unknownClient;
    switch (ref.kind) {
      case 'opening': {
        const f = data.folder;
        return [
          { label: t.number, value: String(f.number ?? f.id) },
          { label: t.type, value: f.type === 'TUTELAGE' ? texts().common.tutelage : texts().common.invoicing },
          { label: t.openedOn, value: formatDate(f.openingDate) },
          ...(f.firstDate ? [{ label: t.firstSession, value: formatDate(f.firstDate) }] : []),
          ...(f.handledBy ? [{ label: t.handledBy, value: f.handledBy }] : []),
          ...(f.status ? [{ label: t.status, value: f.status }] : []),
          ...(f.wayToOurOffice ? [{ label: t.cameThrough, value: f.wayToOurOffice }] : []),
          ...(f.comment ? [{ label: t.comment, value: f.comment }] : []),
        ];
      }
      case 'honorary': {
        const h = data.honoraries.find((x) => x.id === ref.id);
        if (!h) return [];
        return [
          { label: t.wording, value: h.description ?? '' },
          { label: t.duration, value: (h.minutes ?? 0) + ' min' },
          { label: t.hourlyRate, value: formatChf(h.hourlyRate) },
          ...(h.expenses ?? []).map((e, i) => ({ label: e.description || t.expense(i + 1), value: formatChf(e.amount) })),
          { label: t.amount, value: formatChf(h.amount) },
          { label: t.invoice, value: h.invoiceId != null ? invoiceRef(h.invoiceId) : t.toInvoice },
        ];
      }
      case 'invoice': {
        const inv = this.invoice();
        if (!inv) return [];
        return [
          { label: t.due, value: t.dueValue(formatDate(inv.dueDate), inv.delayDays ?? 0) },
          { label: t.honoraries, value: formatChf(inv.honorariesSum) },
          ...((inv.discount ?? 0) > 0 ? [{ label: t.discount, value: '− ' + formatChf(inv.discount) }] : []),
          { label: t.vat(inv.taxRatePercent ?? 0), value: formatChf(inv.tax) },
          ...((inv.advancedSum ?? 0) > 0 ? [{ label: t.advances, value: '− ' + formatChf(inv.advancedSum) }] : []),
          { label: t.amount, value: formatChf(inv.amount) },
          ...(inv.shares ?? []).map((s) => ({ label: t.paidBy(name(s.customerId)), value: formatChf(s.paid) + ' / ' + formatChf(s.amount) })),
          { label: inv.paid ? t.paidOn : t.remaining, value: inv.paid ? formatDate(inv.paidOn) : formatChf(inv.remainingSum) },
          { label: t.recalls, value: inv.recalls?.length ? String(inv.recalls.length) : t.none },
        ];
      }
      case 'request': {
        const req = this.request();
        if (!req) return [];
        return [
          { label: t.due, value: t.dueValue(formatDate(req.dueDate), req.delayDays ?? 0) },
          { label: t.requested, value: formatChf(req.amount) },
          ...(req.shares ?? []).map((s) => ({ label: t.paidBy(name(s.customerId)), value: formatChf(s.paid) + ' / ' + formatChf(s.amount) })),
          { label: req.paid ? t.paidOn : t.remaining, value: req.paid ? formatDate(req.paidOn) : formatChf(req.remainingSum) },
          { label: t.recalls, value: req.recalls?.length ? String(req.recalls.length) : t.none },
        ];
      }
      case 'advance': {
        const a = data.advances.find((x) => x.id === ref.id);
        if (!a) return [];
        return [
          { label: t.client, value: name(a.customerId) },
          { label: t.means, value: meansText(a) },
          ...(a.iban ? [{ label: 'IBAN', value: a.iban }] : []),
          { label: t.answers, value: a.requestId ? requestRef(a.requestId) : t.spontaneous },
          { label: t.deductedOn, value: a.invoiceId ? invoiceRef(a.invoiceId) : t.notYet },
          { label: t.amount, value: formatChf(a.amount) },
        ];
      }
      case 'payment': {
        const p = this.invoice()?.payments?.find((x) => x.id === ref.id);
        if (!p) return [];
        return [
          { label: t.client, value: name(p.customerId) },
          { label: t.means, value: meansText(p) },
          ...(p.iban ? [{ label: 'IBAN', value: p.iban }] : []),
          { label: t.invoice, value: invoiceRef(ref.invoiceId) },
          { label: t.amount, value: formatChf(p.amount) },
        ];
      }
      case 'invoice-recall':
      case 'request-recall': {
        const doc = ref.kind === 'invoice-recall' ? this.invoice() : this.request();
        const r = doc?.recalls?.find((x) => x.id === ref.id);
        if (!r) return [];
        return [
          { label: t.document, value: ref.kind === 'invoice-recall' ? invoiceRef(ref.invoiceId) : requestRef(ref.requestId) },
          { label: t.degree, value: String(r.degree ?? '') },
          { label: t.claimed, value: formatChf(r.amount) },
        ];
      }
    }
  });

  protected readonly printable = computed<Printable | null>(() => {
    const ref = this.item().ref;
    const folder = (this.data().folder.name ?? 'dossier').replace(/[^\p{L}\p{N}]+/gu, '_');
    switch (ref.kind) {
      case 'invoice':
        return this.auth.hasGrant('invoicing:invoice_display')
          ? { type: 'INVOICE', fileName: `${folder}_${invoiceRef(ref.id)}.pdf`, print: (printInput) => this.documents.printInvoice({ id: ref.id, printInput }, 'body', false, ACCEPT_PDF) }
          : null;
      case 'request':
        return this.auth.hasGrant('invoicing:request_display')
          ? { type: 'ADVANCE_REQUEST', fileName: `${folder}_${requestRef(ref.id)}.pdf`, print: (printInput) => this.documents.printAdvanceRequest({ id: ref.id, printInput }, 'body', false, ACCEPT_PDF) }
          : null;
      case 'invoice-recall':
        return this.auth.hasGrant('invoicing:invoice_display')
          ? {
              type: 'RECALL',
              fileName: `${folder}_${invoiceRef(ref.invoiceId)}_${texts().folder.detail.recallSuffix}.pdf`,
              print: (printInput) => this.documents.printInvoiceRecall({ id: ref.invoiceId, recallId: ref.id, printInput }, 'body', false, ACCEPT_PDF),
            }
          : null;
      case 'request-recall':
        return this.auth.hasGrant('invoicing:request_display')
          ? {
              type: 'RECALL',
              fileName: `${folder}_${requestRef(ref.requestId)}_${texts().folder.detail.recallSuffix}.pdf`,
              print: (printInput) => this.documents.printAdvanceRequestRecall({ id: ref.requestId, recallId: ref.id, printInput }, 'body', false, ACCEPT_PDF),
            }
          : null;
      default:
        return null;
    }
  });

  protected readonly models = toSignal(
    toObservable(computed(() => this.printable()?.type ?? null)).pipe(
      switchMap((type) =>
        type ? loadable(this.templates.listDocumentModels({ type })) : of<Loadable<DocumentModel[]>>({ state: 'ready', value: [] }),
      ),
    ),
    { initialValue: { state: 'loading' } as Loadable<DocumentModel[]> },
  );
  protected readonly modelList = computed(() => {
    const m = this.models();
    return m.state === 'ready' ? m.value : [];
  });
  protected readonly chosenModel = signal<number | null>(null);
  protected readonly modelId = computed(() => this.chosenModel() ?? this.modelList()[0]?.id ?? null);

  protected readonly actions = computed<Action[]>(() => {
    const ref = this.item().ref;
    const can = (grant: string) => this.auth.hasGrant('invoicing:' + grant);
    const t = texts().folder.detail;
    const list: Action[] = [];
    if (ref.kind === 'honorary') {
      const h = this.data().honoraries.find((x) => x.id === ref.id);
      if (h && h.invoiceId == null && can('honorary_modify')) {
        list.push({ id: 'delete', label: t.deleteSession, confirm: t.confirmDelete, danger: true, run: () => this.honoraries.deleteHonorary({ id: ref.id }), next: () => null });
      }
    }
    if (ref.kind === 'invoice') {
      const inv = this.invoice();
      if (inv && !inv.paid && can('invoice_modify')) {
        const degree = (inv.recalls?.length ?? 0) + 1;
        list.push({
          id: 'recall',
          label: t.createRecall(degree),
          run: () => this.invoices.recallInvoice({ id: ref.id, dateInput: {} }),
          next: (result) => {
            const last = latestRecall((result as Invoice).recalls);
            return last?.id ? 'fr' + last.id : 'f' + ref.id;
          },
        });
      }
    }
    if (ref.kind === 'request') {
      const req = this.request();
      if (req && !req.paid && can('request_modify')) {
        const degree = (req.recalls?.length ?? 0) + 1;
        list.push({
          id: 'recall',
          label: t.createRecall(degree),
          run: () => this.requests.recallAdvanceRequest({ id: ref.id, dateInput: {} }),
          next: (result) => {
            const last = latestRecall((result as AdvanceRequest).recalls);
            return last?.id ? 'pr' + last.id : 'p' + ref.id;
          },
        });
      }
    }
    if (ref.kind === 'advance') {
      const a = this.data().advances.find((x) => x.id === ref.id);
      if (a && a.invoiceId == null && can('advance_receipt')) {
        list.push({ id: 'cancel', label: t.cancelAdvance, confirm: t.confirmCancel, danger: true, run: () => this.advances.cancelAdvance({ id: ref.id }), next: () => null });
      }
    }
    if (ref.kind === 'payment' && can('invoice_modify')) {
      list.push({
        id: 'cancel',
        label: t.cancelPayment,
        confirm: t.confirmCancel,
        danger: true,
        run: () => this.invoices.cancelInvoicePayment({ id: ref.invoiceId, paymentId: ref.id }),
        next: () => 'f' + ref.invoiceId,
      });
    }
    return list;
  });

  constructor() {
    // another item: forget the half-done confirmation and the last error
    toObservable(this.item).subscribe(() => {
      this.confirming.set(null);
      this.error.set(null);
      this.chosenModel.set(null);
    });
  }

  protected async runAction(action: Action): Promise<void> {
    if (action.confirm && this.confirming() !== action.id) {
      this.confirming.set(action.id);
      return;
    }
    this.confirming.set(null);
    this.busy.set(true);
    this.error.set(null);
    try {
      const result = await firstValueFrom(action.run(), { defaultValue: null });
      this.changed.emit(action.next ? action.next(result) : this.item().key);
    } catch (e: unknown) {
      this.error.set(problemMessage(e));
    } finally {
      this.busy.set(false);
    }
  }

  protected async print(printable: Printable): Promise<void> {
    const modelId = this.modelId();
    if (modelId === null) {
      return;
    }
    this.printing.set(true);
    this.error.set(null);
    try {
      const pdf = await firstValueFrom(printable.print({ modelId, mergeAddresses: this.mergeAddresses }));
      openPdf(pdf, printable.fileName);
    } catch (e: unknown) {
      this.error.set(await blobProblemMessage(e, texts().common.pdfFailed));
    } finally {
      this.printing.set(false);
    }
  }
}
