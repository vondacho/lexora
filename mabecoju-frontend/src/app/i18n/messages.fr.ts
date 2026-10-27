import { commonFr } from './common.i18n';
import { templatesFr } from '../features/templates/templates.i18n';
import { summariesFr } from '../features/summaries/summaries.i18n';
import { vatFr } from '../features/vat/vat.i18n';
import { expensesFr } from '../features/expenses/expenses.i18n';
import { bankFr } from '../features/bank/bank.i18n';
import { debtorsFr } from '../features/debtors/debtors.i18n';
import { foldersFr } from '../features/folders/folders.i18n';
import { folderFr } from '../features/folder/folder.i18n';
import { homeFr } from '../features/home/home.i18n';
import { shellFr } from '../shell/shell.i18n';
import { loginFr } from '../features/login/login.i18n';

/** French texts of the interface, one section per screen. */
export const fr = {
  common: commonFr,
  shell: shellFr,
  login: loginFr,
  home: homeFr,
  folder: folderFr,
  folders: foldersFr,
  debtors: debtorsFr,
  bank: bankFr,
  expenses: expensesFr,
  vat: vatFr,
  summaries: summariesFr,
  templates: templatesFr,
};

export type Messages = typeof fr;
