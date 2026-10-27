import { shellFr } from './shell.i18n';

export type NavKey = keyof typeof shellFr.nav;

/** Screens of the sidebar, in the order of the mockup; their labels come from the texts (shell.nav). */
export interface NavItem {
  path: string;
  key: NavKey;
}

export const NAV_ITEMS: NavItem[] = [
  { path: '/a-traiter', key: 'todo' },
  { path: '/dossiers', key: 'folders' },
  { path: '/debiteurs', key: 'debtors' },
  { path: '/encaissements', key: 'bank' },
  { path: '/depenses', key: 'expenses' },
  { path: '/tva', key: 'vat' },
  { path: '/syntheses', key: 'summaries' },
  { path: '/modeles', key: 'templates' },
];
