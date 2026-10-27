import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DebtorsOverview } from '../../api/generated';
import { ChfPipe } from '../../shared/format';
import { foldersToBill, Loadable } from './home.model';

@Component({
  selector: 'mbj-bill-widget',
  imports: [RouterLink, ChfPipe],
  template: `
    <section class="card widget" aria-labelledby="bill-title">
      <div class="widget-head">
        <div>
          <h2 id="bill-title">À facturer</h2>
          <p class="muted">Top 5 · honoraires non couverts par les provisions</p>
        </div>
        @if (top(); as t) {
          <span class="widget-total">{{ t.total | chf }}</span>
        }
      </div>
      @switch (data().state) {
        @case ('loading') {
          <p class="empty">Chargement…</p>
        }
        @case ('error') {
          <p class="problem">Les dossiers à facturer n’ont pas pu être chargés.</p>
        }
        @case ('ready') {
          @if (top(); as t) {
            @if (t.items.length === 0) {
              <p class="empty">Aucun honoraire en attente de facturation.</p>
            } @else {
              <ol class="rows">
                @for (f of t.items; track f.folderId; let i = $index) {
                  <li>
                    <span class="rank" aria-hidden="true">{{ i + 1 }}</span>
                    <div class="row-main">
                      <a [routerLink]="['/dossiers', f.folderId]">{{ f.folderName }}</a>
                      <span class="row-sub">
                        #{{ f.folderId }} · à facturer {{ f.toBeInvoiced | chf }} ·
                        {{ f.available > 0 ? 'provisions ' + (f.available | chf) : 'aucune provision' }}
                      </span>
                    </div>
                    <span class="amount">{{ f.uncovered | chf }}</span>
                    <a class="btn btn-primary btn-small bill" [routerLink]="['/dossiers', f.folderId]" [attr.aria-label]="'Facturer ' + f.folderName">Facturer</a>
                  </li>
                }
              </ol>
              @if (t.count > t.items.length) {
                <p class="widget-foot muted">{{ t.count }} dossiers à facturer en tout</p>
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

  protected readonly top = computed(() => {
    const data = this.data();
    return data.state === 'ready' ? foldersToBill(data.value) : null;
  });
}
