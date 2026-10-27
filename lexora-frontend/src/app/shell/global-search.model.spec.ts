import { Folder } from '../api/generated';
import { matchScreens, mergeFolders, parseQuery } from './global-search.model';

describe('global search', () => {
  it('recognises document references', () => {
    expect(parseQuery(' F-12 ').document).toEqual({ kind: 'invoice', id: 12 });
    expect(parseQuery('p3').document).toEqual({ kind: 'request', id: 3 });
    expect(parseQuery('f - 7').document).toEqual({ kind: 'invoice', id: 7 });
    expect(parseQuery('F-12').words).toBe(false);
    expect(parseQuery('Fa').document).toBeUndefined();
  });

  it('recognises folder numbers, slip references and words', () => {
    expect(parseQuery('#42').number).toBe(42);
    expect(parseQuery('7').number).toBe(7);
    expect(parseQuery('7').words).toBe(false);
    const qr = parseQuery('21 00000 00003 13947 14300 09017');
    expect(qr.slip).toBe('210000000003139471430009017');
    expect(qr.number).toBeUndefined();
    expect(parseQuery('rf18 5390 0754 7034').slip).toBe('RF18539007547034');
    expect(parseQuery('Dupont').words).toBe(true);
    expect(parseQuery('D').words).toBe(false);
  });

  it('finds screens by any part of their name, accents ignored', () => {
    const screens = [{ label: 'Dépenses' }, { label: 'Décompte TVA' }, { label: 'Débiteurs & rappels' }];
    expect(matchScreens('depen', screens)).toEqual([{ label: 'Dépenses' }]);
    expect(matchScreens('tva', screens)).toEqual([{ label: 'Décompte TVA' }]);
    expect(matchScreens('d', screens)).toEqual([]);
  });

  it('lists each folder once, the number first', () => {
    const f = (id: number): Folder => ({ id, name: 'F' + id });
    const merged = mergeFolders([f(2)], [f(1), f(2)], [f(3), f(1)]);
    expect(merged.map((m) => [m.folder.id, m.via])).toEqual([
      [2, 'number'],
      [1, 'name'],
      [3, 'customer'],
    ]);
    expect(mergeFolders([], [f(1), f(2), f(3)], [], 2)).toHaveLength(2);
  });
});
