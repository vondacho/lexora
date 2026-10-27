import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { catchError, firstValueFrom, of } from 'rxjs';
import { BankReceiptResult, BankStatementAnalysis, BankStatementsService, DebtorsOverview, DebtorsService } from '../../api/generated';
import { AuthService } from '../../core/auth.service';
import { ChfPipe, SwissDatePipe } from '../../shared/format';
import { problemMessage } from '../../shared/problem';
import { Decision, docRef, formatReference, initialDecisions, ManualChoice, PROBLEMS, receipts, Row, rows, summary } from './bank.model';

interface Outcome {
  recorded: number;
  skipped: number;
  settled: number;
  advances: number;
}

const MAX_SIZE = 10 * 1024 * 1024;

/** "Encaissements": import of the bank notification (camt.054), matched to the invoices and requests it pays. */
@Component({
  selector: 'mbj-bank-page',
  imports: [FormsModule, RouterLink, ChfPipe, SwissDatePipe],
  templateUrl: './bank-page.html',
  styleUrl: './bank-page.css',
})
export class BankPage {
  private readonly bank = inject(BankStatementsService);
  private readonly debtors = inject(DebtorsService);
  protected readonly canImport = inject(AuthService).hasGrant('invoicing:debtor_receipt');

  protected readonly fileName = signal<string | null>(null);
  private content = '';
  protected readonly analysis = signal<BankStatementAnalysis | null>(null);
  protected readonly decisions = signal<Decision[]>([]);
  protected readonly openDocuments = signal<ManualChoice[]>([]);
  protected readonly reading = signal(false);
  protected readonly saving = signal(false);
  protected readonly dragging = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly outcome = signal<Outcome | null>(null);

  protected readonly rows = computed(() => {
    const a = this.analysis();
    return a ? rows(a, this.decisions()) : [];
  });
  protected readonly summary = computed(() => summary(this.rows()));
  protected readonly problems = PROBLEMS;
  protected readonly ref = docRef;
  protected readonly reference = formatReference;

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(false);
    const file = event.dataTransfer?.files?.[0];
    if (file) {
      void this.load(file);
    }
  }

  protected onPick(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) {
      void this.load(file);
    }
    input.value = '';
  }

  protected reset(): void {
    this.analysis.set(null);
    this.fileName.set(null);
    this.outcome.set(null);
    this.error.set(null);
    this.content = '';
  }

  protected toggle(row: Row): void {
    this.decide(row.index, { ...row.decision, include: !row.decision.include });
  }

  /** A document picked for a credit without match ('' clears the choice). */
  protected choose(row: Row, key: string): void {
    const manual = this.openDocuments().find((d) => d.kind + ':' + d.documentId === key) ?? null;
    this.decide(row.index, { include: !!manual, manual });
  }

  protected choiceKey(row: Row): string {
    const m = row.decision.manual;
    return m ? m.kind + ':' + m.documentId : '';
  }

  protected async validate(): Promise<void> {
    const list = receipts(this.rows());
    if (!list.length || this.saving()) {
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    try {
      const result = await firstValueFrom(this.bank.recordBankReceipts({ bankReceiptsInput: { receipts: list } }));
      const results: BankReceiptResult[] = result.results ?? [];
      this.outcome.set({
        recorded: results.filter((r) => r.recorded).length,
        skipped: results.filter((r) => !r.recorded).length,
        settled: results.filter((r) => r.settled).length,
        advances: results.reduce((s, r) => s + (r.advance ?? 0), 0),
      });
      // read the file again: what was recorded now shows as already imported
      await this.analyse();
    } catch (e: unknown) {
      this.error.set(problemMessage(e, 'Les paiements n’ont pas pu être enregistrés.'));
    } finally {
      this.saving.set(false);
    }
  }

  private decide(index: number, decision: Decision): void {
    this.decisions.update((list) => list.map((d, i) => (i === index ? decision : d)));
  }

  private async load(file: File): Promise<void> {
    this.error.set(null);
    this.outcome.set(null);
    if (file.size > MAX_SIZE) {
      this.error.set('Fichier trop grand pour un relevé (plus de 10 Mo).');
      return;
    }
    this.fileName.set(file.name);
    this.content = await file.text();
    await this.analyse();
  }

  private async analyse(): Promise<void> {
    this.reading.set(true);
    try {
      const [analysis, overview] = await Promise.all([
        firstValueFrom(this.bank.analyseBankStatement({ bankStatementInput: { fileName: this.fileName() ?? undefined, content: this.content } })),
        firstValueFrom(this.debtors.getDebtorsOverview().pipe(catchError(() => of({} as DebtorsOverview)))),
      ]);
      this.analysis.set(analysis);
      this.decisions.set(initialDecisions(analysis));
      this.openDocuments.set([
        ...(overview.openInvoices ?? []).map((d) => ({ kind: 'INVOICE' as const, documentId: d.documentId ?? 0, folderId: d.folderId ?? 0, folderName: d.folderName ?? '', remaining: d.remaining ?? 0 })),
        ...(overview.openRequests ?? []).map((d) => ({ kind: 'ADVANCE_REQUEST' as const, documentId: d.documentId ?? 0, folderId: d.folderId ?? 0, folderName: d.folderName ?? '', remaining: d.remaining ?? 0 })),
      ].filter((d) => d.remaining > 0).sort((a, b) => a.folderName.localeCompare(b.folderName)));
    } catch (e: unknown) {
      this.analysis.set(null);
      this.error.set(problemMessage(e, 'Le fichier n’a pas pu être lu.'));
    } finally {
      this.reading.set(false);
    }
  }
}
