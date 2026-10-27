import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, firstValueFrom, forkJoin, map, of } from 'rxjs';
import { BookingService, NextExecution, OutgoingPayment, StandingOrder } from '../../api/generated';
import { AuthService } from '../../core/auth.service';
import { ChfPipe, SwissDatePipe } from '../../shared/format';
import { todayIso } from '../../shared/money';
import { problemMessage } from '../../shared/problem';
import { expenseFigures, KindFilter, kindLabel, monthLabel, ofMonth, orderOf, paymentLabel, shiftMonth, visiblePayments } from './expenses.model';
import { OrderEditor } from './order-editor';
import { PaymentEditor } from './payment-editor';

type Tab = 'payments' | 'orders';

interface Books {
  payments: OutgoingPayment[];
  orders: StandingOrder[];
  next: NextExecution | null;
}

const FILTERS: { value: KindFilter; label: string }[] = [
  { value: 'all', label: 'Tout' },
  { value: 'BANK', label: 'Normal' },
  { value: 'EXPENSE', label: 'Frais' },
  { value: 'PERMANENT', label: 'Ordres' },
  { value: 'unpaid', label: 'À payer' },
];

/** Office expenses: one-off payments month by month, and the standing orders booked every month. */
@Component({
  selector: 'mbj-expenses-page',
  imports: [ChfPipe, SwissDatePipe, PaymentEditor, OrderEditor],
  templateUrl: './expenses-page.html',
  styleUrl: './expenses-page.css',
})
export class ExpensesPage {
  private readonly booking = inject(BookingService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly can = {
    create: this.auth.hasGrant('booking:payment_create'),
    modify: this.auth.hasGrant('booking:payment_modify'),
    orders: this.auth.hasGrant('booking:orders_list'),
    orderCreate: this.auth.hasGrant('booking:order_create'),
    orderModify: this.auth.hasGrant('booking:order_modify'),
  };

  private readonly route = inject(ActivatedRoute);
  protected readonly tab = toSignal(
    this.route.queryParamMap.pipe(map((q): Tab => (q.get('onglet') === 'ordres' ? 'orders' : 'payments'))),
    { initialValue: 'payments' as Tab },
  );
  protected readonly filters = FILTERS;
  protected readonly filter = signal<KindFilter>('all');
  protected readonly month = signal(todayIso().slice(0, 7));
  protected readonly monthName = computed(() => monthLabel(this.month()));
  protected readonly ofMonthName = computed(() => ofMonth(this.month()));

  protected readonly state = signal<'loading' | 'ready' | 'error'>('loading');
  protected readonly data = signal<Books>({ payments: [], orders: [], next: null });

  protected readonly rows = computed(() => visiblePayments(this.data().payments, this.month(), this.filter()));
  protected readonly shownTotal = computed(() => this.rows().reduce((s, p) => s + Math.round((p.amount ?? 0) * 100), 0) / 100);
  protected readonly shownTax = computed(() => this.rows().reduce((s, p) => s + Math.round((p.tax ?? 0) * 100), 0) / 100);
  protected readonly figures = computed(() => expenseFigures(this.data().payments, this.data().orders, this.month()));

  /** 'new', a payment id, or null. */
  protected readonly selectedPayment = signal<number | 'new' | null>(null);
  protected readonly editedPayment = computed(() => {
    const id = this.selectedPayment();
    return typeof id === 'number' ? this.data().payments.find((p) => p.id === id) ?? null : null;
  });
  protected readonly selectedOrder = signal<number | 'new' | null>(null);
  protected readonly editedOrder = computed(() => {
    const id = this.selectedOrder();
    return typeof id === 'number' ? this.data().orders.find((o) => o.id === id) ?? null : null;
  });
  protected readonly ordersTotal = computed(() => this.figures().ordersTotal);

  protected readonly flash = signal<string | null>(null);
  protected readonly executing = signal(false);
  protected readonly confirmExecution = signal(false);
  protected readonly executionError = signal<string | null>(null);
  protected readonly nextMonth = computed(() => this.data().next?.month ?? null);
  protected readonly activeOrders = computed(() => this.data().orders.filter((o) => o.enabled));

  protected readonly kind = kindLabel;
  protected readonly typeOf = paymentLabel;

  constructor() {
    // ?mois=2026-10 opens that month (link from the VAT screen)
    const month = this.route.snapshot.queryParamMap.get('mois');
    if (month && /^\d{4}-(0[1-9]|1[0-2])$/.test(month)) {
      this.month.set(month);
    }
    void this.reload();
  }

  protected readonly label = monthLabel;

  protected setTab(tab: Tab): void {
    this.flash.set(null);
    void this.router.navigate([], { queryParams: { onglet: tab === 'orders' ? 'ordres' : null }, replaceUrl: true });
  }

  protected moveMonth(delta: number): void {
    this.month.set(shiftMonth(this.month(), delta));
    if (this.filter() === 'unpaid') {
      this.filter.set('all');
    }
  }

  protected newPayment(): void {
    this.setTab('payments');
    this.selectedPayment.set('new');
  }

  protected newOrder(): void {
    this.setTab('orders');
    this.selectedOrder.set('new');
  }

  protected selectPayment(p: OutgoingPayment): void {
    this.flash.set(null);
    this.selectedPayment.set(this.selectedPayment() === p.id ? null : p.id);
  }

  protected showOrderOf(p: OutgoingPayment): void {
    const order = orderOf(p, this.data().orders);
    this.setTab('orders');
    this.selectedOrder.set(order?.id ?? null);
    if (!order) {
      this.flash.set('L’ordre de ce paiement n’existe plus.');
    }
  }

  protected async afterPayment(message: string, keep: number | null): Promise<void> {
    await this.reload();
    this.selectedPayment.set(keep);
    this.flash.set(message);
    const p = keep === null ? null : this.data().payments.find((x) => x.id === keep);
    if (p?.date && !p.date.startsWith(this.month()) && this.filter() !== 'unpaid') {
      this.month.set(p.date.slice(0, 7));
    }
  }

  protected async afterOrder(message: string, keep: number | null): Promise<void> {
    await this.reload();
    this.selectedOrder.set(keep);
    this.flash.set(message);
  }

  /** Books the active orders for the next month to execute (as the API computes it), after a confirmation. */
  protected async execute(): Promise<void> {
    const month = this.nextMonth();
    if (!month || this.executing()) {
      return;
    }
    if (!this.confirmExecution()) {
      this.confirmExecution.set(true);
      return;
    }
    this.executing.set(true);
    this.executionError.set(null);
    try {
      const booked = await firstValueFrom(this.booking.executeStandingOrders({ executionInput: { month } }));
      this.confirmExecution.set(false);
      await this.reload();
      this.month.set(month);
      this.flash.set(`${booked.length} paiement${booked.length > 1 ? 's' : ''} comptabilisé${booked.length > 1 ? 's' : ''} pour ${monthLabel(month)}.`);
    } catch (e: unknown) {
      this.executionError.set(problemMessage(e, 'Les ordres n’ont pas pu être comptabilisés.'));
    } finally {
      this.executing.set(false);
    }
  }

  /** Loads the books; the previous figures stay on screen while it runs. */
  private async reload(): Promise<void> {
    try {
      const books = await firstValueFrom(
        forkJoin({
          payments: this.booking.listOfficePayments(),
          orders: this.can.orders ? this.booking.listOfficeStandingOrders() : of([] as StandingOrder[]),
          next: this.booking.getNextExecution({}).pipe(catchError(() => of(null))),
        }),
      );
      this.data.set(books);
      this.state.set('ready');
    } catch {
      if (this.state() === 'ready') {
        this.flash.set('Les dépenses n’ont pas pu être rechargées.');
      } else {
        this.state.set('error');
      }
    }
  }
}
