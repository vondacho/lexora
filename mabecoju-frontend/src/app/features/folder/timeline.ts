import { Component, computed, ElementRef, inject, input, output, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HonorariesService } from '../../api/generated';
import { ChfPipe, SwissDatePipe } from '../../shared/format';
import { fromCents, toCents, todayIso } from '../../shared/money';
import { problemMessage } from '../../shared/problem';
import { FolderStore } from './folder-store';
import { Category, groupByMonth, TimelineItem } from './timeline.model';

type Filter = 'all' | Category;

const FILTERS: { value: Filter; label: string }[] = [
  { value: 'all', label: 'Tout' },
  { value: 'session', label: 'Séances' },
  { value: 'billing', label: 'Factures & provisions' },
  { value: 'cash', label: 'Encaissements' },
];

/** The folder's events month by month, with a quick entry of a session. */
@Component({
  selector: 'mbj-timeline',
  imports: [FormsModule, ChfPipe, SwissDatePipe],
  templateUrl: './timeline.html',
  styleUrl: './timeline.css',
})
export class Timeline {
  private readonly store = inject(FolderStore);
  private readonly honoraries = inject(HonorariesService);

  readonly folderId = input.required<number>();
  readonly items = input.required<TimelineItem[]>();
  readonly selectedKey = input<string | null>(null);
  readonly canRecord = input(false);
  readonly selectItem = output<TimelineItem>();

  private readonly minutesInput = viewChild<ElementRef<HTMLInputElement>>('minutesInput');

  protected readonly filters = FILTERS;
  protected readonly filter = signal<Filter>('all');
  protected readonly months = computed(() => {
    const filter = this.filter();
    return groupByMonth(this.items().filter((i) => filter === 'all' || i.category === filter || (filter === 'billing' && i.category === 'opening')));
  });

  // quick entry of a session
  protected readonly wordings = computed(() => {
    const list = this.store.wordings().map((w) => w.text ?? '').filter(Boolean);
    return list.length ? list : ['Séance de médiation', 'Entretien téléphonique', 'Correspondance'];
  });
  protected readonly settings = this.store.settings;
  protected date = todayIso();
  protected minutes = 60;
  protected readonly description = signal('Séance de médiation');
  /** 'meeting' or 'standard' tariff; follows the wording until chosen by hand. */
  protected readonly tariff = signal<'meeting' | 'standard'>('meeting');
  protected readonly minutesValue = signal(60);
  protected readonly rate = computed(() =>
    this.tariff() === 'meeting' ? this.settings().meetingHourlyRate : this.settings().standardHourlyRate,
  );
  protected readonly amount = computed(() => fromCents(Math.round((toCents(this.rate()) * this.minutesValue()) / 60)));
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly saved = signal<string | null>(null);

  /** Called by the "+ Séance" button of the page. */
  focusQuickEntry(): void {
    const input = this.minutesInput()?.nativeElement;
    input?.scrollIntoView({ block: 'center', behavior: 'smooth' });
    input?.focus();
    input?.select();
  }

  protected onDescription(text: string): void {
    this.description.set(text);
    this.tariff.set(/séance/i.test(text) ? 'meeting' : 'standard');
  }

  protected onMinutes(value: number | null): void {
    this.minutes = value ?? 0;
    this.minutesValue.set(Math.max(0, Math.round(value ?? 0)));
  }

  protected async record(): Promise<void> {
    if (this.saving() || this.minutesValue() <= 0 || !this.date) {
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.saved.set(null);
    this.honoraries
      .recordHonorary({
        folderId: this.folderId(),
        honoraryInput: {
          date: this.date,
          description: this.description().trim() || undefined,
          minutes: this.minutesValue(),
          hourlyRate: this.rate(),
        },
      })
      .subscribe({
        next: async (honorary) => {
          await this.store.reload();
          this.saving.set(false);
          this.saved.set(`${honorary.description ?? 'Séance'} du ${honorary.date?.split('-').reverse().join('.')} ajoutée.`);
        },
        error: (e: unknown) => {
          this.saving.set(false);
          this.error.set(problemMessage(e, 'La séance n’a pas pu être enregistrée.'));
        },
      });
  }
}
