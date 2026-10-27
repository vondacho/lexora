import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';


export const routes: Routes = [
  {
    path: 'connexion',
    data: { titleKey: 'login' },
    loadComponent: () => import('./features/login/login').then((m) => m.Login),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./shell/shell').then((m) => m.Shell),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'a-traiter' },
      {
        path: 'a-traiter',
        data: { titleKey: 'todo' },
        loadComponent: () => import('./features/home/home').then((m) => m.Home),
      },
      {
        path: 'dossiers/:id',
        data: { titleKey: 'folder' },
        loadComponent: () => import('./features/folder/folder-page').then((m) => m.FolderPage),
      },
      {
        path: 'dossiers',
        pathMatch: 'full',
        data: { titleKey: 'folders' },
        loadComponent: () => import('./features/folders/folders-page').then((m) => m.FoldersPage),
      },
      {
        path: 'debiteurs',
        data: { titleKey: 'debtors' },
        loadComponent: () => import('./features/debtors/debtors-page').then((m) => m.DebtorsPage),
      },
      {
        path: 'encaissements',
        data: { titleKey: 'bank' },
        loadComponent: () => import('./features/bank/bank-page').then((m) => m.BankPage),
      },
      {
        path: 'depenses',
        data: { titleKey: 'expenses' },
        loadComponent: () => import('./features/expenses/expenses-page').then((m) => m.ExpensesPage),
      },
      {
        path: 'tva',
        data: { titleKey: 'vat' },
        loadComponent: () => import('./features/vat/vat-page').then((m) => m.VatPage),
      },
      {
        path: 'syntheses',
        data: { titleKey: 'summaries' },
        loadComponent: () => import('./features/summaries/summaries-page').then((m) => m.SummariesPage),
      },
      {
        path: 'modeles',
        data: { titleKey: 'templates' },
        loadComponent: () => import('./features/templates/templates-page').then((m) => m.TemplatesPage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
