import { Component, input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CustomerInput, FolderInput } from '../../api/generated';
import { texts } from '../../i18n/i18n';

/** Fields of a folder, edited in place on the object given. */
@Component({
  selector: 'mbj-folder-fields',
  imports: [FormsModule],
  template: `
    <div class="grid">
      <div class="field">
        <label [for]="id('type')">{{ tx().folders.fields.type }}</label>
        <select [id]="id('type')" class="input" [(ngModel)]="value().type" [ngModelOptions]="{ standalone: true }">
          <option value="INVOICING">{{ tx().common.invoicing }}</option>
          <option value="TUTELAGE">{{ tx().common.tutelage }}</option>
        </select>
      </div>
      <div class="field wide">
        <label [for]="id('name')">{{ tx().folders.fields.name }}</label>
        <input [id]="id('name')" class="input" maxlength="50" required [(ngModel)]="value().name" [ngModelOptions]="{ standalone: true }" [placeholder]="tx().folders.fields.nameHint" />
      </div>
      <div class="field">
        <label [for]="id('status')">{{ tx().folders.fields.status }}</label>
        <input [id]="id('status')" class="input" maxlength="80" [attr.list]="id('statuses')" [(ngModel)]="value().status" [ngModelOptions]="{ standalone: true }" />
        <datalist [id]="id('statuses')">
          @for (s of statuses(); track s) {
            <option [value]="s"></option>
          }
        </datalist>
      </div>
      <div class="field">
        <label [for]="id('handled')">{{ tx().folders.fields.handledBy }}</label>
        <input [id]="id('handled')" class="input" maxlength="10" [(ngModel)]="value().handledBy" [ngModelOptions]="{ standalone: true }" [placeholder]="tx().folders.fields.initials" />
      </div>
      <div class="field">
        <label [for]="id('opening')">{{ tx().folders.fields.openedOn }}</label>
        <input [id]="id('opening')" type="date" class="input" [(ngModel)]="value().openingDate" [ngModelOptions]="{ standalone: true }" />
      </div>
      <div class="field">
        <label [for]="id('first')">{{ tx().folders.fields.firstSession }}</label>
        <input [id]="id('first')" type="date" class="input" [(ngModel)]="value().firstDate" [ngModelOptions]="{ standalone: true }" />
      </div>
      <div class="field wide">
        <label [for]="id('way')">{{ tx().folders.fields.cameThrough }}</label>
        <input [id]="id('way')" class="input" [(ngModel)]="value().wayToOurOffice" [ngModelOptions]="{ standalone: true }" [placeholder]="tx().folders.fields.cameThroughHint" />
      </div>
      <div class="field full">
        <label [for]="id('comment')">{{ tx().folders.fields.comment }}</label>
        <textarea [id]="id('comment')" class="input text" rows="2" [(ngModel)]="value().comment" [ngModelOptions]="{ standalone: true }"></textarea>
      </div>
    </div>
  `,
  styleUrl: './fields.css',
})
export class FolderFields {
  readonly value = input.required<FolderInput>();
  protected readonly tx = texts;
  readonly statuses = input<string[]>([]);
  readonly prefix = input('folder');
  protected id(name: string): string {
    return this.prefix() + '-' + name;
  }
}

/** Fields of a customer, edited in place on the object given. */
@Component({
  selector: 'mbj-customer-fields',
  imports: [FormsModule],
  template: `
    <div class="grid">
      <div class="field">
        <label [for]="id('title')">{{ tx().folders.fields.title }}</label>
        <input [id]="id('title')" class="input" maxlength="20" list="titles" [(ngModel)]="value().title" [ngModelOptions]="{ standalone: true }" />
        <datalist id="titles"><option value="Madame"></option><option value="Monsieur"></option><option value="Maître"></option></datalist>
      </div>
      <div class="field">
        <label [for]="id('first')">{{ tx().folders.fields.firstName }}</label>
        <input [id]="id('first')" class="input" maxlength="50" [(ngModel)]="value().firstName" [ngModelOptions]="{ standalone: true }" />
      </div>
      <div class="field">
        <label [for]="id('last')">{{ tx().folders.fields.lastName }}</label>
        <input [id]="id('last')" class="input" maxlength="50" [(ngModel)]="value().lastName" [ngModelOptions]="{ standalone: true }" />
      </div>
      <div class="field wide">
        <label [for]="id('street')">{{ tx().folders.fields.street }}</label>
        <input [id]="id('street')" class="input" maxlength="128" [(ngModel)]="value().street" [ngModelOptions]="{ standalone: true }" />
      </div>
      <div class="field narrow">
        <label [for]="id('zip')">{{ tx().folders.fields.zip }}</label>
        <input [id]="id('zip')" class="input" maxlength="10" inputmode="numeric" [(ngModel)]="value().zip" [ngModelOptions]="{ standalone: true }" />
      </div>
      <div class="field">
        <label [for]="id('city')">{{ tx().folders.fields.city }}</label>
        <input [id]="id('city')" class="input" maxlength="30" [(ngModel)]="value().city" [ngModelOptions]="{ standalone: true }" />
      </div>
      <div class="field">
        <label [for]="id('mobile')">{{ tx().folders.fields.mobile }}</label>
        <input [id]="id('mobile')" class="input" type="tel" maxlength="20" [(ngModel)]="value().mobilePhone" [ngModelOptions]="{ standalone: true }" />
      </div>
      <div class="field">
        <label [for]="id('private')">{{ tx().folders.fields.privatePhone }}</label>
        <input [id]="id('private')" class="input" type="tel" maxlength="20" [(ngModel)]="value().privatePhone" [ngModelOptions]="{ standalone: true }" />
      </div>
      <div class="field">
        <label [for]="id('prof')">{{ tx().folders.fields.workPhone }}</label>
        <input [id]="id('prof')" class="input" type="tel" maxlength="20" [(ngModel)]="value().professionalPhone" [ngModelOptions]="{ standalone: true }" />
      </div>
      <div class="field wide">
        <label [for]="id('email')">{{ tx().folders.fields.email }}</label>
        <input [id]="id('email')" class="input" type="email" maxlength="50" [(ngModel)]="value().email" [ngModelOptions]="{ standalone: true }" />
      </div>
      @if (full()) {
        <div class="field">
          <label [for]="id('since')">{{ tx().folders.fields.activeSince }}</label>
          <input [id]="id('since')" type="date" class="input" [(ngModel)]="value().activeSince" [ngModelOptions]="{ standalone: true }" />
        </div>
        <div class="field">
          <label [for]="id('until')">{{ tx().folders.fields.activeUntil }}</label>
          <input [id]="id('until')" type="date" class="input" [(ngModel)]="value().activeUntil" [ngModelOptions]="{ standalone: true }" />
        </div>
        <div class="field full">
          <label [for]="id('comment')">{{ tx().folders.fields.comment }}</label>
          <textarea [id]="id('comment')" class="input text" rows="2" [(ngModel)]="value().comment" [ngModelOptions]="{ standalone: true }"></textarea>
        </div>
      }
      <label class="check full"><input type="checkbox" [(ngModel)]="value().billing" [ngModelOptions]="{ standalone: true }" /> {{ tx().folders.fields.billing }}</label>
    </div>
  `,
  styleUrl: './fields.css',
})
export class CustomerFields {
  readonly value = input.required<CustomerInput>();
  protected readonly tx = texts;
  readonly prefix = input('customer');
  /** Mandate dates and comment, shown when editing a customer. */
  readonly full = input(false);
  protected id(name: string): string {
    return this.prefix() + '-' + name;
  }
}

/** Empty strings are sent as absent, so that the API keeps nulls. */
export function cleaned<T extends object>(value: T): T {
  return Object.fromEntries(
    Object.entries(value).map(([k, v]) => [k, typeof v === 'string' ? v.trim() || undefined : v]),
  ) as T;
}

export function hasName(c: CustomerInput): boolean {
  return !!(c.firstName?.trim() || c.lastName?.trim());
}
