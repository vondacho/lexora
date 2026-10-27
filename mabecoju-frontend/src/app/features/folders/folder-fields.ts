import { Component, input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CustomerInput, FolderInput } from '../../api/generated';

/** Fields of a folder, edited in place on the object given. */
@Component({
  selector: 'mbj-folder-fields',
  imports: [FormsModule],
  template: `
    <div class="grid">
      <div class="field">
        <label [for]="id('type')">Type</label>
        <select [id]="id('type')" class="input" [(ngModel)]="value().type" [ngModelOptions]="{ standalone: true }">
          <option value="INVOICING">Facturation</option>
          <option value="TUTELAGE">Tutelle</option>
        </select>
      </div>
      <div class="field wide">
        <label [for]="id('name')">Nom du dossier</label>
        <input [id]="id('name')" class="input" maxlength="50" required [(ngModel)]="value().name" [ngModelOptions]="{ standalone: true }" placeholder="ex. Dupont c. Dupont" />
      </div>
      <div class="field">
        <label [for]="id('status')">Statut</label>
        <input [id]="id('status')" class="input" maxlength="80" [attr.list]="id('statuses')" [(ngModel)]="value().status" [ngModelOptions]="{ standalone: true }" />
        <datalist [id]="id('statuses')">
          @for (s of statuses(); track s) {
            <option [value]="s"></option>
          }
        </datalist>
      </div>
      <div class="field">
        <label [for]="id('handled')">Géré par</label>
        <input [id]="id('handled')" class="input" maxlength="10" [(ngModel)]="value().handledBy" [ngModelOptions]="{ standalone: true }" placeholder="initiales" />
      </div>
      <div class="field">
        <label [for]="id('opening')">Ouvert le</label>
        <input [id]="id('opening')" type="date" class="input" [(ngModel)]="value().openingDate" [ngModelOptions]="{ standalone: true }" />
      </div>
      <div class="field">
        <label [for]="id('first')">1re séance</label>
        <input [id]="id('first')" type="date" class="input" [(ngModel)]="value().firstDate" [ngModelOptions]="{ standalone: true }" />
      </div>
      <div class="field wide">
        <label [for]="id('way')">Venu par</label>
        <input [id]="id('way')" class="input" [(ngModel)]="value().wayToOurOffice" [ngModelOptions]="{ standalone: true }" placeholder="recommandation, site internet…" />
      </div>
      <div class="field full">
        <label [for]="id('comment')">Commentaire</label>
        <textarea [id]="id('comment')" class="input text" rows="2" [(ngModel)]="value().comment" [ngModelOptions]="{ standalone: true }"></textarea>
      </div>
    </div>
  `,
  styleUrl: './fields.css',
})
export class FolderFields {
  readonly value = input.required<FolderInput>();
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
        <label [for]="id('title')">Civilité</label>
        <input [id]="id('title')" class="input" maxlength="20" list="titles" [(ngModel)]="value().title" [ngModelOptions]="{ standalone: true }" />
        <datalist id="titles"><option value="Madame"></option><option value="Monsieur"></option><option value="Maître"></option></datalist>
      </div>
      <div class="field">
        <label [for]="id('first')">Prénom</label>
        <input [id]="id('first')" class="input" maxlength="50" [(ngModel)]="value().firstName" [ngModelOptions]="{ standalone: true }" />
      </div>
      <div class="field">
        <label [for]="id('last')">Nom</label>
        <input [id]="id('last')" class="input" maxlength="50" [(ngModel)]="value().lastName" [ngModelOptions]="{ standalone: true }" />
      </div>
      <div class="field wide">
        <label [for]="id('street')">Rue et numéro</label>
        <input [id]="id('street')" class="input" maxlength="128" [(ngModel)]="value().street" [ngModelOptions]="{ standalone: true }" />
      </div>
      <div class="field narrow">
        <label [for]="id('zip')">NPA</label>
        <input [id]="id('zip')" class="input" maxlength="10" inputmode="numeric" [(ngModel)]="value().zip" [ngModelOptions]="{ standalone: true }" />
      </div>
      <div class="field">
        <label [for]="id('city')">Localité</label>
        <input [id]="id('city')" class="input" maxlength="30" [(ngModel)]="value().city" [ngModelOptions]="{ standalone: true }" />
      </div>
      <div class="field">
        <label [for]="id('mobile')">Mobile</label>
        <input [id]="id('mobile')" class="input" type="tel" maxlength="20" [(ngModel)]="value().mobilePhone" [ngModelOptions]="{ standalone: true }" />
      </div>
      <div class="field">
        <label [for]="id('private')">Téléphone privé</label>
        <input [id]="id('private')" class="input" type="tel" maxlength="20" [(ngModel)]="value().privatePhone" [ngModelOptions]="{ standalone: true }" />
      </div>
      <div class="field">
        <label [for]="id('prof')">Téléphone prof.</label>
        <input [id]="id('prof')" class="input" type="tel" maxlength="20" [(ngModel)]="value().professionalPhone" [ngModelOptions]="{ standalone: true }" />
      </div>
      <div class="field wide">
        <label [for]="id('email')">E-mail</label>
        <input [id]="id('email')" class="input" type="email" maxlength="50" [(ngModel)]="value().email" [ngModelOptions]="{ standalone: true }" />
      </div>
      @if (full()) {
        <div class="field">
          <label [for]="id('since')">Actif depuis</label>
          <input [id]="id('since')" type="date" class="input" [(ngModel)]="value().activeSince" [ngModelOptions]="{ standalone: true }" />
        </div>
        <div class="field">
          <label [for]="id('until')">Actif jusqu’au</label>
          <input [id]="id('until')" type="date" class="input" [(ngModel)]="value().activeUntil" [ngModelOptions]="{ standalone: true }" />
        </div>
        <div class="field full">
          <label [for]="id('comment')">Commentaire</label>
          <textarea [id]="id('comment')" class="input text" rows="2" [(ngModel)]="value().comment" [ngModelOptions]="{ standalone: true }"></textarea>
        </div>
      }
      <label class="check full"><input type="checkbox" [(ngModel)]="value().billing" [ngModelOptions]="{ standalone: true }" /> Paie toute la facture (au lieu de sa part)</label>
    </div>
  `,
  styleUrl: './fields.css',
})
export class CustomerFields {
  readonly value = input.required<CustomerInput>();
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
