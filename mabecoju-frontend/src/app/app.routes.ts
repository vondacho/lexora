import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';


export const routes: Routes = [
  {
    path: 'connexion',
    title: 'Connexion — mabecoju',
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
        title: 'À traiter — mabecoju',
        loadComponent: () => import('./features/home/home').then((m) => m.Home),
      },
      {
        path: 'dossiers/:id',
        title: 'Dossier — mabecoju',
        loadComponent: () => import('./features/folder/folder-page').then((m) => m.FolderPage),
      },
      {
        path: 'dossiers',
        pathMatch: 'full',
        title: 'Dossiers — mabecoju',
        loadComponent: () => import('./features/folders/folders-page').then((m) => m.FoldersPage),
      },
      {
        path: 'debiteurs',
        title: 'Débiteurs & rappels — mabecoju',
        loadComponent: () => import('./features/debtors/debtors-page').then((m) => m.DebtorsPage),
      },
      {
        path: 'encaissements',
        title: 'Encaissements — mabecoju',
        loadComponent: () => import('./features/bank/bank-page').then((m) => m.BankPage),
      },
      {
        path: 'depenses',
        title: 'Dépenses — mabecoju',
        loadComponent: () => import('./features/expenses/expenses-page').then((m) => m.ExpensesPage),
      },
      {
        path: 'tva',
        title: 'Décompte TVA — mabecoju',
        loadComponent: () => import('./features/vat/vat-page').then((m) => m.VatPage),
      },
      {
        path: 'syntheses',
        title: 'Synthèses — mabecoju',
        loadComponent: () => import('./features/summaries/summaries-page').then((m) => m.SummariesPage),
      },
      {
        path: 'modeles',
        title: 'Modèles de lettres — mabecoju',
        loadComponent: () => import('./features/templates/templates-page').then((m) => m.TemplatesPage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
