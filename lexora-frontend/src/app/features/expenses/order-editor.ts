import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { BookingService, StandingOrder, StandingOrderInput } from '../../api/generated';
import { ChfPipe } from '../../shared/format';
import { problemMessage } from '../../shared/problem';
import { texts } from '../../i18n/i18n';
import { includedVat, VAT_RATES } from './expenses.model';

interface Draft {
  creditor: string;
  title: string;
  amount: number | null;
  rate: number;
  day: number;
  enabled: boolean;
}

const EMPTY: Draft = { creditor: '', title: '', amount: null, rate: 0, day: 1, enabled: true };

/** New or existing standing order of the office books. */
@Component({
  selector: 'lx-order-editor',
  imports: [FormsModule, ChfPipe],
  template: `
    <section class="card editor" aria-labelledby="order-edit-title">
      <div class="head">
        <h2 id="order-edit-title">{{ order() ? tx().expenses.editor.editOrder : tx().expenses.editor.newOrder }}</h2>
        <button type="button" class="btn btn-small" (click)="closed.emit()">{{ tx().common.close }}</button>
      </div>
      <form class="form" (ngSubmit)="save()">
        <div class="field"><label for="o-cred">{{ tx().expenses.creditor }}</label><input id="o-cred" name="creditor" class="input" maxlength="80" [(ngModel)]="draft.creditor" (ngModelChange)="touch()" /></div>
        <div class="field"><label for="o-title">{{ tx().expenses.editor.title }}</label><input id="o-title" name="title" class="input" maxlength="80" [(ngModel)]="draft.title" /></div>
        <div class="row3">
          <div class="field"><label for="o-amount">{{ tx().expenses.editor.amountGross }}</label><input id="o-amount" name="amount" type="number" min="0" step="0.05" class="input" [(ngModel)]="draft.amount" (ngModelChange)="touch()" /></div>
          <div class="field">
            <label for="o-rate">{{ tx().expenses.editor.vatRate }}</label>
            <select id="o-rate" name="rate" class="input" [(ngModel)]="draft.rate" (ngModelChange)="touch()">
              @for (r of rates; track r) {
                <option [ngValue]="r">{{ r }} %</option>
              }
            </select>
          </div>
          <div class="field"><label for="o-day">{{ tx().expenses.editor.dayOfMonth }}</label><input id="o-day" name="day" type="number" min="1" max="31" class="input" [(ngModel)]="draft.day" (ngModelChange)="touch()" /></div>
        </div>
        <p class="note muted">{{ tx().expenses.editor.orderVatNote(vat() | chf) }}</p>
        <label class="check"><input type="checkbox" name="enabled" [(ngModel)]="draft.enabled" /> {{ tx().expenses.editor.enabled }}</label>
        <p class="note muted">{{ tx().expenses.editor.orderChangeNote }}</p>
        @if (invalid() && tried()) {
          <p class="problem">{{ tx().expenses.editor.orderInvalid }}</p>
        }
        @if (error(); as message) {
          <p class="problem" role="alert">{{ message }}</p>
        }
        <div class="actions">
          <button type="submit" class="btn btn-primary" [disabled]="busy() || !canModify()">
            {{ busy() ? tx().common.saving : order() ? tx().expenses.editor.saveChanges : tx().expenses.editor.createOrder }}
          </button>
          @if (order() && canModify()) {
            <button type="button" class="btn btn-warn" [disabled]="busy()" (click)="remove()">{{ confirming() ? tx().expenses.editor.confirmDelete : tx().common.delete }}</button>
          }
        </div>
      </form>
    </section>
  `,
  styleUrl: './editor.css',
})
export class OrderEditor {
  protected readonly tx = texts;
  private readonly booking = inject(BookingService);

  readonly order = input<StandingOrder | null>(null);
  readonly canModify = input(false);
  readonly saved = output<number>();
  readonly deleted = output<void>();
  readonly closed = output<void>();

  protected readonly rates = VAT_RATES;
  protected draft: Draft = { ...EMPTY };
  private readonly version = signal(0);
  protected readonly vat = computed(() => (this.version(), includedVat(this.draft.amount ?? 0, this.draft.rate)));
  protected readonly invalid = computed(
    () => (this.version(), !this.draft.creditor.trim() || !(this.draft.amount && this.draft.amount > 0) || !(this.draft.day >= 1 && this.draft.day <= 31)),
  );
  protected readonly busy = signal(false);
  protected readonly tried = signal(false);
  protected readonly confirming = signal(false);
  protected readonly error = signal<string | null>(null);

  constructor() {
    effect(() => {
      const o = this.order();
      this.draft = o
        ? { creditor: o.creditor ?? '', title: o.title ?? '', amount: o.amount ?? null, rate: o.taxRatePercent ?? 0, day: o.dayOfMonth ?? 1, enabled: !!o.enabled }
        : { ...EMPTY };
      this.tried.set(false);
      this.confirming.set(false);
      this.error.set(null);
      this.version.update((v) => v + 1);
    });
  }

  protected touch(): void {
    this.version.update((v) => v + 1);
  }

  protected async save(): Promise<void> {
    this.tried.set(true);
    this.touch();
    if (this.invalid() || this.busy()) {
      return;
    }
    const d = this.draft;
    const body: StandingOrderInput = {
      creditor: d.creditor.trim(),
      title: d.title.trim() || undefined,
      amount: d.amount ?? 0,
      taxRatePercent: d.rate,
      dayOfMonth: Math.round(d.day),
      enabled: d.enabled,
    };
    this.busy.set(true);
    this.error.set(null);
    try {
      const existing = this.order();
      const result = await firstValueFrom(
        existing
          ? this.booking.updateStandingOrder({ id: existing.id, standingOrderInput: body })
          : this.booking.createOfficeStandingOrder({ standingOrderInput: body }),
      );
      this.saved.emit(result.id);
    } catch (e: unknown) {
      this.error.set(problemMessage(e, texts().expenses.editor.orderSaveFailed));
    } finally {
      this.busy.set(false);
    }
  }

  protected async remove(): Promise<void> {
    const o = this.order();
    if (!o) {
      return;
    }
    if (!this.confirming()) {
      this.confirming.set(true);
      return;
    }
    this.busy.set(true);
    try {
      await firstValueFrom(this.booking.deleteStandingOrder({ id: o.id }), { defaultValue: null });
      this.deleted.emit();
    } catch (e: unknown) {
      this.error.set(problemMessage(e, texts().expenses.editor.orderDeleteFailed));
    } finally {
      this.busy.set(false);
      this.confirming.set(false);
    }
  }
}
