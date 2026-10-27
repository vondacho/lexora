import { Component, computed, effect, ElementRef, inject, input, signal, viewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Customer } from '../../api/generated';
import { AuthService } from '../../core/auth.service';
import { ChfPipe, SwissDatePipe } from '../../shared/format';
import { todayIso } from '../../shared/money';
import { texts } from '../../i18n/i18n';
import { CustomerEditor } from './customer-editor';
import { CustomersPanel } from './customers-panel';
import { FolderEditor } from './folder-editor';
import { FolderStore } from './folder-store';
import { InvoiceComposer } from './invoice-composer';
import { ItemDetail } from './item-detail';
import { PaymentComposer } from './payment-composer';
import { RequestComposer } from './request-composer';
import { Timeline } from './timeline';
import { buildTimeline, folderFigures, TimelineItem } from './timeline.model';

type Panel = 'invoice' | 'request' | 'payment' | 'folder' | 'customer';

/** A folder: its timeline, what is owed, and the actions of the mockup. */
@Component({
  selector: 'lx-folder-page',
  imports: [RouterLink, ChfPipe, SwissDatePipe, Timeline, InvoiceComposer, RequestComposer, PaymentComposer, ItemDetail, CustomersPanel, FolderEditor, CustomerEditor],
  providers: [FolderStore],
  templateUrl: './folder-page.html',
  styleUrl: './folder-page.css',
})
export class FolderPage {
  private readonly store = inject(FolderStore);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** Route parameter. */
  readonly id = input.required<string>();

  private readonly timeline = viewChild(Timeline);
  private readonly side = viewChild<ElementRef<HTMLElement>>('side');

  protected readonly tx = texts;
  protected readonly state = this.store.state;
  protected readonly refreshing = this.store.refreshing;
  protected readonly data = computed(() => {
    const s = this.state();
    return s.state === 'ready' ? s.data : null;
  });
  protected readonly items = computed(() => {
    const data = this.data();
    return data ? buildTimeline(data, todayIso()) : [];
  });
  protected readonly figures = computed(() => {
    const data = this.data();
    return data ? folderFigures(data) : null;
  });

  protected readonly panel = signal<Panel | null>(null);
  protected readonly selectedKey = signal<string | null>(null);
  protected readonly selected = computed(() => this.items().find((i) => i.key === this.selectedKey()) ?? null);
  protected readonly notice = signal<string | null>(null);

  protected readonly can = {
    session: this.auth.hasGrant('invoicing:honorary_create'),
    request: this.auth.hasGrant('invoicing:request_create'),
    payment: this.auth.hasGrant('invoicing:debtor_receipt'),
    invoice: this.auth.hasGrant('invoicing:invoice_create'),
    folder: this.auth.hasGrant('invoicing:folder_modify'),
    customerCreate: this.auth.hasGrant('invoicing:customer_create'),
    customerModify: this.auth.hasGrant('invoicing:customer_modify'),
  };
  /** The client edited in the side panel; null for a new one. */
  protected readonly editedCustomer = signal<Customer | null>(null);

  constructor() {
    effect(() => {
      this.panel.set(null);
      this.selectedKey.set(null);
      this.store.load(Number(this.id()));
    });
  }

  protected addSession(): void {
    this.panel.set(null);
    this.timeline()?.focusQuickEntry();
  }

  protected open(panel: Panel): void {
    this.notice.set(null);
    this.selectedKey.set(null);
    this.panel.set(panel);
    this.revealSide();
  }

  protected editCustomer(customer: Customer | null): void {
    this.editedCustomer.set(customer);
    this.open('customer');
  }

  protected folderDeleted(): void {
    void this.router.navigate(['/dossiers']);
  }

  protected select(item: TimelineItem): void {
    this.panel.set(null);
    this.notice.set(null);
    this.selectedKey.set(this.selectedKey() === item.key ? null : item.key);
    if (this.selectedKey()) {
      this.revealSide();
    }
  }

  /** After an action: reload, then select the item to look at next. */
  protected async afterChange(selectKey: string | null, notice: string | null = null): Promise<void> {
    await this.store.reload();
    this.panel.set(null);
    this.selectedKey.set(selectKey);
    this.notice.set(notice);
    if (selectKey) {
      this.revealSide();
    }
  }

  protected reloadOnly(): void {
    void this.store.reload();
  }

  /** On a narrow screen the side panel is below the timeline: bring it into view. */
  private revealSide(): void {
    setTimeout(() => {
      const side = this.side()?.nativeElement;
      if (side && side.getBoundingClientRect().top > window.innerHeight * 0.6) {
        side.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    });
  }
}
