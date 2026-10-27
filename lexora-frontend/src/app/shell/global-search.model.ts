import { Folder } from '../api/generated';
import { plain } from '../shared/text';

/** What a typed text can be: a document reference (F-12, P-3), a payment slip reference, a folder number, words. */
export interface SearchQuery {
  text: string;
  document?: { kind: 'invoice' | 'request'; id: number };
  /** QR or creditor reference of a payment slip, without spaces. */
  slip?: string;
  /** Folder number (#12 or 12). */
  number?: number;
  /** Has letters: searched in folder and client names. */
  words: boolean;
}

export function parseQuery(raw: string): SearchQuery {
  const text = raw.trim();
  const doc = /^([fp])\s*-?\s*(\d{1,9})$/i.exec(text);
  const compact = text.replace(/\s+/g, '');
  const slip = /^(\d{15,27}|RF\d{2}[0-9A-Z]{1,21})$/i.test(compact) ? compact.toUpperCase() : undefined;
  const number = /^#?\d{1,9}$/.test(text) ? Number(text.replace('#', '')) : undefined;
  return {
    text,
    document: doc ? { kind: doc[1].toLowerCase() === 'f' ? 'invoice' : 'request', id: Number(doc[2]) } : undefined,
    slip,
    number: slip ? undefined : number,
    words: !doc && !slip && /\p{L}/u.test(text) && text.length >= 2,
  };
}

/** Screens whose name contains the text, or a word of it, accents and case ignored. */
export function matchScreens<T extends { label: string }>(text: string, screens: T[]): T[] {
  const wanted = plain(text.trim());
  if (wanted.length < 2) {
    return [];
  }
  return screens.filter((s) => plain(s.label).includes(wanted));
}

export type FolderMatch = { folder: Folder; via: 'number' | 'name' | 'customer' };

/** The folders found by number first, then by name, then by a client's name; each folder once. */
export function mergeFolders(byNumber: Folder[], byName: Folder[], byCustomer: Folder[], limit = 6): FolderMatch[] {
  const seen = new Set<number>();
  const merged: FolderMatch[] = [];
  const add = (folders: Folder[], via: FolderMatch['via']) => {
    for (const folder of folders) {
      if (!seen.has(folder.id)) {
        seen.add(folder.id);
        merged.push({ folder, via });
      }
    }
  };
  add(byNumber, 'number');
  add(byName, 'name');
  add(byCustomer, 'customer');
  return merged.slice(0, limit);
}
