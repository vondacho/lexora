import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DebtorsOverview } from '../../api/generated';
import { ChfPipe, formatDate } from '../../shared/format';
import { debtsToRecall, DebtToRecall, Loadable } from './home.model';

@Component({
  selector: 'mbj-recall-widget',
  imports: [RouterLink, ChfPipe],
  template: `
    <section class="card widget" aria-labelledby="recall-title">
      <div class="widget-head">
        <div>
          <h2 id="recall-title">Impayés à relancer</h2>
          <p class="muted">Top 5 · factures (F) et provisions (P) échues, les plus anciennes d’abord</p>
        </div>
        @if (top(); as t) {
          <span class="widget-total warn" [attr.aria-label]="'Total échu ' + (t.total | chf)">{{ t.total | chf }}</span>
        }
      </div>
      @switch (data().state) {
        @case ('loading') {
          <p class="empty">Chargement…</p>
        }
        @case ('error') {
          <p class="problem">Les impayés n’ont pas pu être chargés.</p>
        }
        @case ('ready') {
          @if (top(); as t) {
            @if (t.items.length === 0) {
              <p class="empty">Rien à relancer : aucune facture ni provision échue.</p>
            } @else {
              <ul class="rows">
                @for (d of t.items; track d.reference) {
                  <li>
                    <span class="badge" [class.request]="d.kind === 'request'" aria-hidden="true">{{ d.kind === 'invoice' ? 'F' : 'P' }}</span>
                    <div class="row-main">
                      <a [routerLink]="['/dossiers', d.folderId]">{{ d.reference }} · {{ d.folderName }}</a>
                      <span class="row-sub">
                        échue depuis <strong class="warn">{{ d.daysOverdue }} jours</strong> · {{ recallText(d) }}
                      </span>
                    </div>
                    <span class="amount">{{ d.remaining | chf }}</span>
                    <a class="btn btn-warn btn-small recall" routerLink="/debiteurs" [queryParams]="{ document: d.reference }">Rappel n° {{ d.nextRecall }}</a>
                  </li>
                }
              </ul>
              @if (t.count > t.items.length) {
                <p class="widget-foot"><a routerLink="/debiteurs">Voir les {{ t.count }} documents échus</a></p>
              }
            }
          }
        }
      }
    </section>
  `,
  styles: `
    .warn { color: var(--warn); }
    .recall { margin-left: auto; }
  `,
})
export class RecallWidget {
  readonly data = input.required<Loadable<DebtorsOverview>>();

  protected readonly top = computed(() => {
    const data = this.data();
    return data.state === 'ready' ? debtsToRecall(data.value) : null;
  });

  protected recallText(debt: DebtToRecall): string {
    if (debt.recallCount === 0) {
      return 'aucun rappel';
    }
    const count = debt.recallCount === 1 ? '1 rappel' : `${debt.recallCount} rappels`;
    return debt.lastRecallDate ? `${count}, le ${formatDate(debt.lastRecallDate, false)}` : count;
  }
}
