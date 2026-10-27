import { Component, computed, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { DocumentModel, DocumentModelInput, DocumentType, TemplatesService } from '../../api/generated';
import { AuthService } from '../../core/auth.service';
import { problemMessage } from '../../shared/problem';
import { texts } from '../../i18n/i18n';
import { fillTemplate, insertAt } from '../debtors/recall.model';
import { RichEditor } from './rich-editor';
import {
  fields,
  grouped,
  isHtml,
  layoutsFor,
  mentionsAmount,
  PageKind,
  pagesOf,
  sampleValues,
  textToHtml,
  typeLabel,
  unknownFields,
} from './templates.model';

interface Draft {
  name: string;
  title: string;
  type: DocumentType;
  addressCount: number;
  pages: PageKind[];
  content: string;
  format: 'html' | 'text';
}

const STARTERS: Record<DocumentType, string> = {
  INVOICE: '<p>$title_1,</p><p>Vous trouverez ci-joint le décompte de mes honoraires. Votre part s’élève à frs. $amount_1, payable dans les $delay jours.</p><p>Je vous prie d’agréer mes meilleures salutations.</p>',
  ADVANCE_REQUEST: '<p>$title_1,</p><p>Je me permets de vous demander de bien vouloir me verser à titre de provision un montant de frs. $amount_1 d’ici au $dueDate.</p><p>Avec mes meilleures salutations.</p>',
  RECALL: '<p>$title_1,</p><p>Sauf erreur de notre part, notre envoi n° $refNo du $invoiceDate reste impayé. Le solde s’élève à frs. $amount_1.</p><p>Avec nos meilleures salutations.</p>',
  UNDEFINED: '<p>$title_1,</p><p></p>',
};

const escapeHtml = (v: string) => v.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');

/** "Modèles de lettres": the letter texts of invoices, advance requests and reminders, with their placeholders. */
@Component({
  selector: 'mbj-templates-page',
  imports: [FormsModule, RichEditor],
  templateUrl: './templates-page.html',
  styleUrl: './templates-page.css',
})
export class TemplatesPage {
  private readonly templates = inject(TemplatesService);
  protected readonly canEdit = inject(AuthService).hasGrant('invoicing:models');

  private readonly rich = viewChild(RichEditor);
  private readonly plain = viewChild<ElementRef<HTMLTextAreaElement>>('plainText');
  private readonly editorSection = viewChild<ElementRef<HTMLElement>>('editorSection');

  protected readonly tx = texts;
  protected readonly fields = computed(fields);
  protected readonly typeLabel = typeLabel;
  protected readonly types: DocumentType[] = ['INVOICE', 'ADVANCE_REQUEST', 'RECALL', 'UNDEFINED'];

  protected readonly state = signal<'loading' | 'ready' | 'error'>('loading');
  protected readonly models = signal<DocumentModel[]>([]);
  protected readonly groups = computed(() => grouped(this.models()));

  /** A template id, 'new', or null. */
  protected readonly selected = signal<number | 'new' | null>(null);
  protected readonly original = computed(() => {
    const id = this.selected();
    return typeof id === 'number' ? this.models().find((m) => m.id === id) ?? null : null;
  });
  protected readonly draft = signal<Draft | null>(null);
  private readonly savedDraft = signal<string>('');
  protected readonly dirty = computed(() => this.draft() !== null && JSON.stringify(this.draft()) !== this.savedDraft());
  /** Another template asked for while the current one has unsaved changes. */
  protected readonly pending = signal<number | 'new' | null>(null);

  protected readonly unknown = computed(() => unknownFields(this.draft()?.content ?? ''));
  protected readonly noAmount = computed(() => {
    const d = this.draft();
    return !!d && d.type !== 'UNDEFINED' && !mentionsAmount(d.content);
  });

  protected readonly copy = signal<0 | 1>(0);
  protected readonly preview = computed(() => {
    const d = this.draft();
    if (!d) {
      return null;
    }
    const values = sampleValues(d.type, this.copy(), d.title);
    // filled fields are highlighted, so that one sees what the API puts in
    const marked = Object.fromEntries(Object.entries(values).map(([k, v]) => [k, `<span class="filled">${escapeHtml(v)}</span>`]));
    const source = d.format === 'html' ? d.content : escapeHtml(d.content);
    return { html: d.format === 'html', body: fillTemplate(source, marked), values };
  });

  protected readonly busy = signal(false);
  protected readonly confirmDelete = signal(false);
  protected readonly confirmConvert = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly flash = signal<string | null>(null);

  constructor() {
    void this.reload();
  }

  protected select(id: number | 'new'): void {
    if (this.dirty() && id !== this.selected()) {
      this.pending.set(id);
      return;
    }
    this.open(id);
  }

  protected discardAndOpen(): void {
    const id = this.pending();
    this.pending.set(null);
    if (id !== null) {
      this.open(id);
    }
  }

  protected update<K extends keyof Draft>(key: K, value: Draft[K]): void {
    this.draft.update((d) => (d ? { ...d, [key]: value } : d));
  }

  protected setPage(index: number, kind: PageKind | 'none'): void {
    this.draft.update((d) => {
      if (!d) return d;
      const pages = [...d.pages];
      if (kind === 'none') {
        pages.splice(index, 1);
      } else {
        pages[index] = kind;
      }
      return { ...d, pages: pages.length ? pages : ['letter'] };
    });
  }

  protected addPage(): void {
    this.draft.update((d) => (d && d.pages.length < 4 ? { ...d, pages: [...d.pages, d.type === 'INVOICE' ? 'detail' : 'letter'] } : d));
  }

  /** Inserts a field at the cursor of the text. */
  protected insert(token: string): void {
    const d = this.draft();
    if (!d) return;
    if (d.format === 'html') {
      this.rich()?.insert(token);
      return;
    }
    const area = this.plain()?.nativeElement;
    const start = area?.selectionStart ?? d.content.length;
    const end = area?.selectionEnd ?? start;
    const result = insertAt(d.content, token, start, end);
    this.update('content', result.text);
    setTimeout(() => {
      area?.focus();
      area?.setSelectionRange(result.cursor, result.cursor);
    });
  }

  /** Plain text → formatted text, after a confirmation (the legacy application would print the tags). */
  protected convert(): void {
    if (!this.confirmConvert()) {
      this.confirmConvert.set(true);
      return;
    }
    this.confirmConvert.set(false);
    this.draft.update((d) => (d ? { ...d, content: textToHtml(d.content), format: 'html' } : d));
  }

  protected async save(): Promise<void> {
    const d = this.draft();
    if (!d || !d.name.trim() || this.busy()) {
      return;
    }
    const body: DocumentModelInput = {
      name: d.name.trim(),
      title: d.title.trim() || undefined,
      type: d.type,
      content: d.content,
      addressCount: d.addressCount,
      layouts: layoutsFor(d.pages, this.original()?.layouts, d.type),
    };
    await this.run(async () => {
      const existing = this.original();
      const saved = await firstValueFrom(
        existing
          ? this.templates.updateDocumentModel({ id: existing.id, documentModelInput: body })
          : this.templates.createDocumentModel({ documentModelInput: body }),
      );
      await this.reload();
      this.open(saved.id);
      this.flash.set(existing ? texts().templates.saved : texts().templates.created);
    });
  }

  protected async duplicate(): Promise<void> {
    const d = this.draft();
    if (!d) return;
    await this.run(async () => {
      const copy = await firstValueFrom(
        this.templates.createDocumentModel({
          documentModelInput: {
            name: (d.name.trim() + texts().templates.copySuffix).slice(0, 80),
            title: d.title || undefined,
            type: d.type,
            content: d.content,
            addressCount: d.addressCount,
            layouts: layoutsFor(d.pages, this.original()?.layouts, d.type),
          },
        }),
      );
      await this.reload();
      this.open(copy.id);
      this.flash.set(texts().templates.copied);
    });
  }

  protected async remove(): Promise<void> {
    const original = this.original();
    if (!original) return;
    if (!this.confirmDelete()) {
      this.confirmDelete.set(true);
      return;
    }
    await this.run(async () => {
      await firstValueFrom(this.templates.deleteDocumentModel({ id: original.id }), { defaultValue: null });
      await this.reload();
      this.selected.set(null);
      this.draft.set(null);
      this.flash.set(texts().templates.deleted);
    });
    this.confirmDelete.set(false);
  }

  protected newTemplate(): void {
    this.select('new');
  }

  private open(id: number | 'new'): void {
    const model = id === 'new' ? null : this.models().find((m) => m.id === id) ?? null;
    const content = model?.content ?? STARTERS.INVOICE;
    const draft: Draft = {
      name: model?.name ?? '',
      title: model?.title ?? '',
      type: model?.type ?? 'INVOICE',
      addressCount: model?.addressCount ?? 1,
      pages: pagesOf(model?.layouts),
      content,
      format: model && !isHtml(content) ? 'text' : 'html',
    };
    this.selected.set(id);
    this.draft.set(draft);
    this.savedDraft.set(JSON.stringify(draft));
    this.confirmDelete.set(false);
    this.confirmConvert.set(false);
    this.error.set(null);
    this.flash.set(null);
    this.copy.set(0);
    setTimeout(() => {
      const section = this.editorSection()?.nativeElement;
      if (section && section.getBoundingClientRect().top > window.innerHeight * 0.6) {
        section.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    });
  }

  /** A new template takes the starter text of its type while nothing was typed yet. */
  protected changeType(type: DocumentType): void {
    const d = this.draft();
    if (!d) return;
    const untouched = this.selected() === 'new' && Object.values(STARTERS).includes(d.content);
    this.draft.set({ ...d, type, content: untouched ? STARTERS[type] : d.content });
  }

  private async run(action: () => Promise<void>): Promise<void> {
    this.busy.set(true);
    this.error.set(null);
    try {
      await action();
    } catch (e: unknown) {
      this.error.set(problemMessage(e));
    } finally {
      this.busy.set(false);
    }
  }

  private async reload(): Promise<void> {
    try {
      this.models.set(await firstValueFrom(this.templates.listDocumentModels()));
      this.state.set('ready');
      if (this.selected() === null && this.models().length) {
        this.open(grouped(this.models())[0].items[0].id);
      }
    } catch {
      this.state.set('error');
    }
  }
}
