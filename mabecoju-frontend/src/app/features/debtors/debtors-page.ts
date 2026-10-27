import { Component, computed, ElementRef, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { combineLatest, firstValueFrom, map, Observable, of, switchMap } from 'rxjs';
import {
  AdvanceRequest,
  AdvanceRequestsService,
  Customer,
  CustomersService,
  DebtorsOverview,
  DebtorsService,
  DocumentModel,
  DocumentsService,
  Invoice,
  InvoicesService,
  TemplatesService,
} from '../../api/generated';
import { AuthService } from '../../core/auth.service';
import { ACCEPT_PDF, openPdf } from '../../shared/download';
import { ChfPipe, formatDate } from '../../shared/format';
import { todayIso } from '../../shared/money';
import { blobProblemMessage, problemMessage } from '../../shared/problem';
import { latestRecall } from '../../shared/recalls';
import { debtsToRecall, DebtToRecall, loadable, Loadable } from '../home/home.model';
import {
  fillTemplate,
  insertAt,
  isHtml,
  letterCopies,
  modelIndexFor,
  placeholderValues,
  recipientsAt,
  RecalledDocument,
} from './recall.model';

interface LetterData {
  recalled: RecalledDocument;
  customers: Customer[];
}

interface Done {
  reference: string;
  degree: number;
  /** The reminder is saved but its PDF failed: offer to print it again. */
  printFailed: string | null;
}

const CHIPS = [
  { label: 'civilité', token: '$title_1' },
  { label: 'prénom', token: '$firstName_1' },
  { label: 'nom', token: '$lastName_1' },
  { label: 'montant dû', token: '$amount_1' },
  { label: 'n° du document', token: '$refNo' },
  { label: 'date de l’envoi', token: '$invoiceDate' },
  { label: 'dernier rappel', token: '$lastRecallDate' },
];

/** "Débiteurs & rappels": pick an overdue document, adjust the letter, create the reminder and print it. */
@Component({
  selector: 'mbj-debtors-page',
  imports: [FormsModule, RouterLink, ChfPipe],
  templateUrl: './debtors-page.html',
  styleUrl: './debtors-page.css',
})
export class DebtorsPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly debtors = inject(DebtorsService);
  private readonly invoices = inject(InvoicesService);
  private readonly requests = inject(AdvanceRequestsService);
  private readonly customers = inject(CustomersService);
  private readonly documents = inject(DocumentsService);

  private readonly textArea = viewChild<ElementRef<HTMLTextAreaElement>>('letterText');
  private readonly letterSection = viewChild<ElementRef<HTMLElement>>('letterSection');

  protected readonly chips = CHIPS;
  protected readonly today = todayIso();
  protected readonly issuer = this.auth.user;

  // ----- 1 · the overdue documents ------------------------------------------------------------------------------
  private readonly listVersion = signal(0);
  protected readonly overview = toSignal(
    toObservable(this.listVersion).pipe(switchMap(() => loadable(this.debtors.getDebtorsOverview()))),
    { initialValue: { state: 'loading' } as Loadable<DebtorsOverview> },
  );
  /** Kept while the list reloads, so that the screen does not flicker. */
  private lastDebts: DebtToRecall[] = [];
  protected readonly debts = computed(() => {
    const o = this.overview();
    if (o.state === 'ready') {
      this.lastDebts = debtsToRecall(o.value, Number.MAX_SAFE_INTEGER).items;
    }
    return this.lastDebts;
  });
  /** References recalled during this visit. */
  protected readonly sent = signal<Set<string>>(new Set());

  private readonly requested = toSignal(inject(ActivatedRoute).queryParamMap.pipe(map((q) => q.get('document'))));
  protected readonly current = computed(
    () => {
      const list = this.debts();
      return list.find((d) => d.reference === this.requested()) ?? list[0] ?? null;
    },
    // a reloaded list gives new objects: only a change of document or of its reminders counts
    { equal: (a, b) => a?.reference === b?.reference && a?.recallCount === b?.recallCount },
  );

  // ----- 2 · the letter ------------------------------------------------------------------------------------------
  protected readonly models = toSignal(loadable(inject(TemplatesService).listDocumentModels({ type: 'RECALL' })), {
    initialValue: { state: 'loading' } as Loadable<DocumentModel[]>,
  });
  /** In creation order (the API sorts them by name): the n-th reminder template serves the n-th reminder. */
  protected readonly modelList = computed(() => {
    const m = this.models();
    return m.state === 'ready' ? [...m.value].sort((a, b) => a.id - b.id) : [];
  });

  /** Reloaded after a reminder, to show it among the previous ones. */
  private readonly letterVersion = signal(0);
  protected readonly letterData = toSignal(
    combineLatest([toObservable(this.current), toObservable(this.letterVersion)]).pipe(
      switchMap(([debt]) => (debt ? loadable(this.loadLetter(debt)) : of<Loadable<LetterData>>({ state: 'loading' }))),
    ),
    { initialValue: { state: 'loading' } as Loadable<LetterData> },
  );
  protected readonly letter = computed(() => {
    const d = this.letterData();
    return d.state === 'ready' ? d.value : null;
  });
  protected readonly nextDegree = computed(() => (this.letter()?.recalled.doc.recalls?.length ?? 0) + 1);

  private readonly chosenModelId = signal<number | null>(null);
  protected readonly model = computed(() => {
    const list = this.modelList();
    const chosen = list.find((m) => m.id === this.chosenModelId());
    return chosen ?? list[modelIndexFor(this.nextDegree(), list.length)] ?? null;
  });
  /** Text typed for this reminder only; null: the template's text. */
  private readonly editedText = signal<string | null>(null);
  protected readonly text = computed(() => this.editedText() ?? this.model()?.content ?? '');
  protected readonly merge = signal(false);
  protected readonly attachments = signal(0);

  // ----- 3 · the preview -----------------------------------------------------------------------------------------
  protected readonly recipients = computed(() => {
    const l = this.letter();
    return l ? recipientsAt(l.customers, l.recalled.doc.date) : [];
  });
  protected readonly copies = computed(() => {
    const l = this.letter();
    return l ? letterCopies(this.recipients(), l.recalled, this.merge() && this.recipients().length > 1) : [];
  });
  protected readonly preview = computed(() => {
    const l = this.letter();
    const model = this.model();
    if (!l || !model) {
      return null;
    }
    const values = placeholderValues(l.recalled, this.recipients(), this.issuer(), model.title ?? '', this.today);
    const raw = this.text();
    const html = isHtml(raw);
    const safe = html ? Object.fromEntries(Object.entries(values).map(([k, v]) => [k, escapeHtml(v)])) : values;
    return { html, body: fillTemplate(raw, safe), title: model.title ?? 'Rappel' };
  });

  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly done = signal<Done | null>(null);
  protected readonly nextDebt = computed(() => {
    const list = this.debts();
    const sent = this.sent();
    const current = this.current();
    const start = Math.max(0, list.findIndex((d) => d.reference === current?.reference));
    return [...list.slice(start + 1), ...list.slice(0, start)].find((d) => !sent.has(d.reference)) ?? null;
  });

  constructor() {
    // another document: start again from its template
    toObservable(this.current)
      .pipe(takeUntilDestroyed())
      .subscribe(() => {
        this.chosenModelId.set(null);
        this.editedText.set(null);
        this.merge.set(false);
        this.attachments.set(0);
        this.error.set(null);
      });
  }

  protected canRecall(debt: DebtToRecall | null): boolean {
    return !!debt && this.auth.hasGrant(debt.kind === 'invoice' ? 'invoicing:invoice_modify' : 'invoicing:request_modify');
  }

  protected canPrint(debt: DebtToRecall | null): boolean {
    return !!debt && this.auth.hasGrant(debt.kind === 'invoice' ? 'invoicing:invoice_display' : 'invoicing:request_display');
  }

  protected pick(debt: DebtToRecall): void {
    this.done.set(null);
    void this.router.navigate([], { queryParams: { document: debt.reference }, replaceUrl: true });
    setTimeout(() => {
      const section = this.letterSection()?.nativeElement;
      if (section && section.getBoundingClientRect().top > window.innerHeight * 0.6) {
        section.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    });
  }

  protected pickModel(id: number): void {
    this.chosenModelId.set(id);
    this.editedText.set(null);
  }

  protected editText(text: string): void {
    this.editedText.set(text);
  }

  protected resetText(): void {
    this.editedText.set(null);
  }

  protected insert(token: string): void {
    const area = this.textArea()?.nativeElement;
    const text = this.text();
    const start = area?.selectionStart ?? text.length;
    const end = area?.selectionEnd ?? start;
    const result = insertAt(text, token, start, end);
    this.editedText.set(result.text);
    setTimeout(() => {
      area?.focus();
      area?.setSelectionRange(result.cursor, result.cursor);
    });
  }

  protected recallText(debt: DebtToRecall): string {
    const n = debt.recallCount;
    return n === 0 ? 'aucun rappel' : n === 1 ? '1 rappel' : `${n} rappels`;
  }

  protected previousRecalls(): string {
    const recalls = this.letter()?.recalled.doc.recalls ?? [];
    return recalls.length
      ? [...recalls].sort((a, b) => (a.degree ?? 0) - (b.degree ?? 0)).map((r) => `n° ${r.degree} le ${formatDate(r.date)}`).join(', ')
      : 'aucun';
  }

  /** Creates the reminder for what is still due, then opens its PDF. */
  protected async createAndPrint(): Promise<void> {
    const debt = this.current();
    const model = this.model();
    if (!debt || !model || this.busy()) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    let recallId: number | undefined;
    try {
      const updated = await firstValueFrom<Invoice | AdvanceRequest>(
        debt.kind === 'invoice'
          ? this.invoices.recallInvoice({ id: debt.documentId, dateInput: { date: this.today } })
          : this.requests.recallAdvanceRequest({ id: debt.documentId, dateInput: { date: this.today } }),
      );
      recallId = latestRecall(updated.recalls)?.id;
    } catch (e: unknown) {
      this.busy.set(false);
      this.error.set(problemMessage(e, 'Le rappel n’a pas pu être créé.'));
      return;
    }
    const degree = this.nextDegree();
    this.sent.update((s) => new Set(s).add(debt.reference));
    const printFailed = recallId === undefined ? 'Le rappel est enregistré mais introuvable pour l’impression.' : await this.print(debt, recallId);
    this.done.set({ reference: debt.reference, degree, printFailed });
    this.busy.set(false);
    this.letterVersion.update((v) => v + 1);
    this.listVersion.update((v) => v + 1);
  }

  /** Prints the last reminder of the current document again (after a failure, or another copy). */
  protected async reprint(): Promise<void> {
    const debt = this.current();
    const last = latestRecall(this.letter()?.recalled.doc.recalls);
    if (!debt || !last?.id || this.busy()) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    const failed = await this.print(debt, last.id);
    this.busy.set(false);
    if (failed) {
      this.error.set(failed);
    } else if (this.done()) {
      this.done.update((d) => (d ? { ...d, printFailed: null } : d));
    }
  }

  protected goNext(): void {
    const next = this.nextDebt();
    if (next) {
      this.pick(next);
    }
  }

  /** Null when the PDF opened, else the reason. */
  private async print(debt: DebtToRecall, recallId: number): Promise<string | null> {
    const model = this.model();
    if (!model || !this.canPrint(debt)) {
      return 'Vos droits ne permettent pas d’imprimer ce rappel.';
    }
    const edited = this.editedText();
    const printInput = {
      modelId: model.id,
      content: edited !== null && edited !== model.content ? edited : undefined,
      attachments: this.attachments(),
      mergeAddresses: this.merge() && this.recipients().length > 1,
    };
    try {
      const pdf = await firstValueFrom(
        debt.kind === 'invoice'
          ? this.documents.printInvoiceRecall({ id: debt.documentId, recallId, printInput }, 'body', false, ACCEPT_PDF)
          : this.documents.printAdvanceRequestRecall({ id: debt.documentId, recallId, printInput }, 'body', false, ACCEPT_PDF),
      );
      const folder = debt.folderName.replace(/[^\p{L}\p{N}]+/gu, '_');
      openPdf(pdf, `${folder}_${debt.reference}_rappel.pdf`);
      return null;
    } catch (e: unknown) {
      return blobProblemMessage(e, 'Le PDF n’a pas pu être produit.');
    }
  }

  private loadLetter(debt: DebtToRecall): Observable<LetterData> {
    const doc$: Observable<Invoice | AdvanceRequest> =
      debt.kind === 'invoice' ? this.invoices.getInvoice({ id: debt.documentId }) : this.requests.getAdvanceRequest({ id: debt.documentId });
    return combineLatest([doc$, this.customers.listCustomers({ folderId: debt.folderId })]).pipe(
      map(([doc, customers]) => ({ recalled: { kind: debt.kind, doc }, customers })),
    );
  }
}

function escapeHtml(text: string): string {
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
