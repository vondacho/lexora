import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DebtorsOverview } from '../../api/generated';
import { ChfPipe } from '../../shared/format';
import { texts } from '../../i18n/i18n';
import { foldersToBill, Loadable } from './home.model';

@Component({
  selector: 'mbj-bill-widget',
  imports: [RouterLink, ChfPipe],
  template: `
    <section class="card widget" aria-labelledby="bill-title">
      <div class="widget-head">
        <div>
          <h2 id="bill-title">{{ tx().home.bill.title }}</h2>
          <p class="muted">{{ tx().home.bill.subtitle }}</p>
        </div>
        @if (top(); as t) {
          <span class="widget-total">{{ t.total | chf }}</span>
        }
      </div>
      @switch (data().state) {
        @case ('loading') {
          <p class="empty">{{ tx().common.loading }}</p>
        }
        @case ('error') {
          <p class="problem">{{ tx().home.bill.failed }}</p>
        }
        @case ('ready') {
          @if (top(); as t) {
            @if (t.items.length === 0) {
              <p class="empty">{{ tx().home.bill.none }}</p>
            } @else {
              <ol class="rows">
                @for (f of t.items; track f.folderId; let i = $index) {
                  <li>
                    <span class="rank" aria-hidden="true">{{ i + 1 }}</span>
                    <div class="row-main">
                      <a [routerLink]="['/dossiers', f.folderId]">{{ f.folderName }}</a>
                      <span class="row-sub">
                        #{{ f.folderId }} · {{ tx().home.bill.toInvoice(f.toBeInvoiced | chf) }} ·
                        {{ f.available > 0 ? tx().home.bill.advances(f.available | chf) : tx().home.bill.noAdvance }}
                      </span>
                    </div>
                    <span class="amount">{{ f.uncovered | chf }}</span>
                    <a class="btn btn-primary btn-small bill" [routerLink]="['/dossiers', f.folderId]" [attr.aria-label]="tx().home.bill.actionLabel(f.folderName)">{{ tx().home.bill.action }}</a>
                  </li>
                }
              </ol>
              @if (t.count > t.items.length) {
                <p class="widget-foot muted">{{ tx().home.bill.count(t.count) }}</p>
              }
            }
          }
        }
      }
    </section>
  `,
  styles: `
    .rank { width: 18px; color: var(--muted); font-size: 13px; }
    .bill { margin-left: auto; }
  `,
})
export class BillWidget {
  readonly data = input.required<Loadable<DebtorsOverview>>();
  protected readonly tx = texts;

  protected readonly top = computed(() => {
    const data = this.data();
    return data.state === 'ready' ? foldersToBill(data.value) : null;
  });
}
