import { Component, effect, inject, input, output, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { Customer, CustomerInput, CustomersService } from '../../api/generated';
import { problemMessage } from '../../shared/problem';
import { todayIso } from '../../shared/money';
import { cleaned, CustomerFields, hasName } from '../folders/folder-fields';
import { customerName } from './timeline.model';

/** A client of the folder: added, changed, his mandate ended, or removed when entered by mistake. */
@Component({
  selector: 'mbj-customer-editor',
  imports: [FormsModule, CustomerFields],
  template: `
    <section class="card composer" aria-labelledby="customer-edit-title">
      <div class="composer-head">
        <h2 id="customer-edit-title">{{ customer() ? 'Modifier ' + name(customer()!) : 'Nouveau client' }}</h2>
        <button type="button" class="btn btn-small" (click)="closed.emit()">Fermer</button>
      </div>
      <form (ngSubmit)="save()">
        <mbj-customer-fields [value]="draft" [full]="true" prefix="edit-customer" />
        @if (error(); as message) {
          <p class="problem" role="alert">{{ message }}</p>
        }
        <div class="actions">
          <button type="submit" class="btn btn-primary" [disabled]="busy() || !canSave()">{{ busy() ? 'Enregistrement…' : customer() ? 'Enregistrer' : 'Ajouter le client' }}</button>
          @if (customer() && canModify() && !draft.activeUntil) {
            <button type="button" class="btn" [disabled]="busy()" (click)="endMandate()">Fin de mandat aujourd’hui</button>
          }
          @if (customer() && canModify()) {
            <button type="button" class="btn btn-warn" [disabled]="busy()" (click)="remove()">{{ confirming() ? 'Confirmer la suppression' : 'Supprimer' }}</button>
          }
        </div>
        <p class="note muted">Un client qui a des factures, demandes ou paiements ne se supprime pas : terminez son mandat. Les lettres ne vont qu’aux clients actifs à la date du document.</p>
      </form>
    </section>
  `,
  styleUrl: './composer.css',
  styles: `form { display: flex; flex-direction: column; gap: 12px; }`,
})
export class CustomerEditor {
  private readonly customers = inject(CustomersService);

  readonly folderId = input.required<number>();
  /** null: a new client. */
  readonly customer = input<Customer | null>(null);
  readonly canCreate = input(false);
  readonly canModify = input(false);
  readonly saved = output<string>();
  readonly closed = output<void>();

  protected draft: CustomerInput = {};
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly confirming = signal(false);
  protected readonly name = customerName;

  constructor() {
    effect(() => {
      const c = this.customer();
      this.draft = c
        ? {
            title: c.title, firstName: c.firstName, lastName: c.lastName, street: c.street, zip: c.zip, city: c.city,
            privatePhone: c.privatePhone, professionalPhone: c.professionalPhone, mobilePhone: c.mobilePhone,
            mobilePhone2: c.mobilePhone2, email: c.email, email2: c.email2, email3: c.email3, comment: c.comment,
            billing: c.billing, activeSince: c.activeSince, activeUntil: c.activeUntil,
          }
        : {};
      this.confirming.set(false);
      this.error.set(null);
    });
  }

  protected canSave(): boolean {
    return this.customer() ? this.canModify() : this.canCreate();
  }

  protected async save(): Promise<void> {
    if (!hasName(this.draft)) {
      this.error.set('Indiquez au moins le prénom ou le nom.');
      return;
    }
    const existing = this.customer();
    await this.run(async () => {
      if (existing) {
        await firstValueFrom(this.customers.updateCustomer({ id: existing.id, customerInput: cleaned(this.draft) }));
        this.saved.emit('Client enregistré.');
      } else {
        await firstValueFrom(this.customers.addCustomer({ folderId: this.folderId(), customerInput: cleaned(this.draft) }));
        this.saved.emit('Client ajouté.');
      }
    });
  }

  protected async endMandate(): Promise<void> {
    this.draft = { ...this.draft, activeUntil: todayIso() };
    await this.save();
  }

  protected async remove(): Promise<void> {
    const existing = this.customer();
    if (!existing) return;
    if (!this.confirming()) {
      this.confirming.set(true);
      return;
    }
    this.confirming.set(false);
    await this.run(async () => {
      await firstValueFrom(this.customers.removeCustomer({ id: existing.id }), { defaultValue: null });
      this.saved.emit('Client supprimé.');
    });
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
}
