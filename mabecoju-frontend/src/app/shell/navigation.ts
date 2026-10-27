/** Screens of the sidebar, in the order of the mockup. */
export interface NavItem {
  path: string;
  label: string;
}

export const NAV_ITEMS: NavItem[] = [
  { path: '/a-traiter', label: 'À traiter' },
  { path: '/dossiers', label: 'Dossiers' },
  { path: '/debiteurs', label: 'Débiteurs & rappels' },
  { path: '/encaissements', label: 'Encaissements' },
  { path: '/depenses', label: 'Dépenses' },
  { path: '/tva', label: 'Décompte TVA' },
  { path: '/syntheses', label: 'Synthèses' },
  { path: '/modeles', label: 'Modèles de lettres' },
];
