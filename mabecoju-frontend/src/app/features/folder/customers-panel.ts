import { Component, input, output } from '@angular/core';
import { Customer } from '../../api/generated';
import { ChfPipe, SwissDatePipe } from '../../shared/format';
import { CustomerDue, customerName } from './timeline.model';

@Component({
  selector: 'mbj-customers-panel',
  imports: [ChfPipe, SwissDatePipe],
  template: `
    <section class="card" aria-labelledby="clients-title">
      <div class="head">
        <h2 id="clients-title">Clients</h2>
        @if (canAdd()) {
          <button type="button" class="btn btn-small" (click)="add.emit()">+ Client</button>
        }
      </div>
      @for (d of dues(); track d.customer.id) {
        <div class="client">
          <span class="initials" aria-hidden="true">{{ initials(d.customer) }}</span>
          <div class="who">
            <div class="name">{{ [d.customer.title, name(d.customer)].filter(present).join(' ') }}</div>
            @if (address(d.customer); as a) {
              <div class="muted">{{ a }}</div>
            }
            @if (contacts(d.customer); as c) {
              <div class="muted">{{ c }}</div>
            }
            <div class="tags">
              <span class="mono" [class.owes]="d.due > 0">{{ d.due > 0 ? 'doit ' + (d.due | chf) : 'rien d’ouvert' }}</span>
              @if (d.customer.billing) {
                <span class="pill">paie tout</span>
              }
              @if (d.customer.activeUntil) {
                <span class="pill neutral">jusqu’au {{ d.customer.activeUntil | swissDate }}</span>
              }
              @if (canEdit()) {
                <button type="button" class="link" (click)="edit.emit(d.customer)" [attr.aria-label]="'Modifier ' + name(d.customer)">Modifier</button>
              }
            </div>
          </div>
        </div>
      } @empty {
        <p class="muted">Aucun client dans ce dossier.</p>
      }
    </section>
  `,
  styles: `
    .head { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 8px; }
    .link { padding: 0; border: 0; background: none; color: var(--accent); text-decoration: underline; cursor: pointer; font-size: 12px; }
    .client { display: flex; gap: 12px; padding: 10px 0; border-top: 1px solid var(--row-line); font-size: 13px; }
    .client:first-of-type { border-top: 0; }
    .initials { width: 36px; height: 36px; flex: none; display: flex; align-items: center; justify-content: center; border-radius: 50%; background: var(--tint); color: var(--accent); font-weight: 600; font-size: 12px; }
    .who { min-width: 0; overflow-wrap: anywhere; }
    .name { font-weight: 600; }
    .tags { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; margin-top: 4px; }
    .owes { color: var(--warn); font-weight: 500; }
    .pill.neutral { background: var(--ground); color: var(--muted); }
  `,
})
export class CustomersPanel {
  readonly dues = input.required<CustomerDue[]>();
  readonly canAdd = input(false);
  readonly canEdit = input(false);
  readonly add = output<void>();
  readonly edit = output<Customer>();

  protected name = customerName;
  protected present = (x: unknown) => !!x;

  protected initials(c: Customer): string {
    return ((c.firstName?.[0] ?? '') + (c.lastName?.[0] ?? '')).toUpperCase() || '?';
  }

  protected address(c: Customer): string {
    return [c.street, [c.zip, c.city].filter(Boolean).join(' ')].filter(Boolean).join(', ');
  }

  protected contacts(c: Customer): string {
    return [c.mobilePhone || c.privatePhone || c.professionalPhone, c.email].filter(Boolean).join(' · ');
  }
}
