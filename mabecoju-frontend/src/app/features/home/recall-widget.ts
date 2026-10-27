import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DebtorsOverview } from '../../api/generated';
import { ChfPipe, formatChf, formatDate } from '../../shared/format';
import { texts } from '../../i18n/i18n';
import { debtsToRecall, DebtToRecall, Loadable } from './home.model';

@Component({
  selector: 'mbj-recall-widget',
  imports: [RouterLink, ChfPipe],
  template: `
    <section class="card widget" aria-labelledby="recall-title">
      <div class="widget-head">
        <div>
          <h2 id="recall-title">{{ tx().home.recall.title }}</h2>
          <p class="muted">{{ tx().home.recall.subtitle }}</p>
        </div>
        @if (top(); as t) {
          <span class="widget-total warn" [attr.aria-label]="tx().home.recall.totalLabel(chf(t.total))">{{ t.total | chf }}</span>
        }
      </div>
      @switch (data().state) {
        @case ('loading') {
          <p class="empty">{{ tx().common.loading }}</p>
        }
        @case ('error') {
          <p class="problem">{{ tx().home.recall.failed }}</p>
        }
        @case ('ready') {
          @if (top(); as t) {
            @if (t.items.length === 0) {
              <p class="empty">{{ tx().home.recall.none }}</p>
            } @else {
              <ul class="rows">
                @for (d of t.items; track d.reference) {
                  <li>
                    <span class="badge" [class.request]="d.kind === 'request'" aria-hidden="true">{{ d.kind === 'invoice' ? 'F' : 'P' }}</span>
                    <div class="row-main">
                      <a [routerLink]="['/dossiers', d.folderId]">{{ d.reference }} · {{ d.folderName }}</a>
                      <span class="row-sub">
                        {{ tx().home.recall.overdue }} <strong class="warn">{{ tx().home.recall.days(d.daysOverdue) }}</strong> · {{ recallText(d) }}
                      </span>
                    </div>
                    <span class="amount">{{ d.remaining | chf }}</span>
                    <a class="btn btn-warn btn-small recall" routerLink="/debiteurs" [queryParams]="{ document: d.reference }">{{ tx().home.recall.action(d.nextRecall) }}</a>
                  </li>
                }
              </ul>
              @if (t.count > t.items.length) {
                <p class="widget-foot"><a routerLink="/debiteurs">{{ tx().home.recall.seeAll(t.count) }}</a></p>
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
  protected readonly tx = texts;
  protected readonly chf = formatChf;

  protected readonly top = computed(() => {
    const data = this.data();
    return data.state === 'ready' ? debtsToRecall(data.value) : null;
  });

  protected recallText(debt: DebtToRecall): string {
    const t = texts().home.recall;
    if (debt.recallCount === 0) {
      return t.noRecall;
    }
    return t.recalls(debt.recallCount, debt.lastRecallDate ? formatDate(debt.lastRecallDate, false) : null);
  }
}
