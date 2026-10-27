import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { of } from 'rxjs';
import { DebtorsOverview, DebtorsService } from '../../api/generated';
import { AuthService } from '../../core/auth.service';
import { longDate, texts } from '../../i18n/i18n';
import { BillWidget } from './bill-widget';
import { debtsToRecall, foldersToBill, loadable, Loadable } from './home.model';
import { MonthWidget } from './month-widget';
import { RecallWidget } from './recall-widget';
import { SearchWidget } from './search-widget';

/** "À traiter": what is waiting for the office, as widgets the user's grants allow. */
@Component({
  selector: 'lx-home',
  imports: [RecallWidget, BillWidget, MonthWidget, SearchWidget],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home {
  private readonly auth = inject(AuthService);
  private readonly debtors = inject(DebtorsService);

  protected readonly tx = texts;
  protected readonly today = new Date();
  protected readonly todayText = computed(() => longDate(this.today));
  protected readonly showDebtors = this.auth.hasGrant('invoicing:debtors_open');
  protected readonly showMonth = this.auth.hasGrant('booking:overview');
  protected readonly showSearch = this.auth.hasGrant('invoicing:folder_search');

  protected readonly overview = toSignal(
    this.showDebtors ? loadable(this.debtors.getDebtorsOverview()) : of<Loadable<DebtorsOverview>>({ state: 'error' }),
    { initialValue: { state: 'loading' } as Loadable<DebtorsOverview> },
  );

  /** Overdue documents plus folders to invoice; null until known. */
  protected readonly pending = computed(() => {
    const overview = this.overview();
    if (overview.state !== 'ready') {
      return null;
    }
    return debtsToRecall(overview.value).count + foldersToBill(overview.value).count;
  });
}
