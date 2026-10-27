import { Folder } from '../../api/generated';
import { distinct, filterFolders, page, sortFolders } from './folders.model';

const folders: Folder[] = [
  { id: 1, number: 1, name: 'Dupont c. Dupont', type: 'INVOICING', status: 'En cours', handledBy: 'dmo', openingDate: '2026-09-01' },
  { id: 7, number: 7, name: 'Rochat c. Rochat', type: 'INVOICING', status: 'En cours', handledBy: 'mmb', openingDate: '2026-10-01' },
  { id: 12, number: 12, name: 'Émery', type: 'TUTELAGE', status: 'récupéré', handledBy: 'dmo', openingDate: '2025-02-01' },
  { id: 15, number: 15, name: 'Martin & Rossi', type: 'INVOICING', handledBy: 'mmb', openingDate: '2026-09-05' },
];
const none = { text: '', type: '' as const, status: '', handledBy: '' };

describe('folder list', () => {
  it('finds folders by number or by any word of the name, without accents', () => {
    expect(filterFolders(folders, { ...none, text: '1' }).map((f) => f.id)).toEqual([1, 12, 15]);
    expect(filterFolders(folders, { ...none, text: 'emer' }).map((f) => f.id)).toEqual([12]);
    expect(filterFolders(folders, { ...none, text: 'rossi' }).map((f) => f.id)).toEqual([15]);
    expect(filterFolders(folders, { ...none, text: 'roch' }).map((f) => f.id)).toEqual([7]);
  });

  it('filters by type, status and person in charge', () => {
    expect(filterFolders(folders, { ...none, type: 'TUTELAGE' }).map((f) => f.id)).toEqual([12]);
    expect(filterFolders(folders, { ...none, status: 'En cours', handledBy: 'mmb' }).map((f) => f.id)).toEqual([7]);
  });

  it('sorts by name, number or opening', () => {
    expect(sortFolders(folders, 'name').map((f) => f.id)).toEqual([1, 12, 15, 7]);
    expect(sortFolders(folders, 'number').map((f) => f.id)).toEqual([15, 12, 7, 1]);
    expect(sortFolders(folders, 'recent').map((f) => f.id)).toEqual([7, 15, 1, 12]);
  });

  it('lists the values of the filters and pages the list', () => {
    expect(distinct(folders, 'status')).toEqual(['En cours', 'récupéré']);
    expect(distinct(folders, 'handledBy')).toEqual(['dmo', 'mmb']);
    expect(page([1, 2, 3, 4, 5], 1, 2)).toEqual({ items: [3, 4], pages: 3, index: 1 });
    expect(page([1, 2, 3], 9, 2)).toEqual({ items: [3], pages: 2, index: 1 });
  });
});
