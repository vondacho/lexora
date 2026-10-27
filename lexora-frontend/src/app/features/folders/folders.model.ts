import { Folder } from '../../api/generated';

export interface FolderFilter {
  text: string;
  type: '' | 'INVOICING' | 'TUTELAGE';
  status: string;
  handledBy: string;
}

export type Sort = 'name' | 'number' | 'recent';

function plain(text: string | undefined): string {
  return (text ?? '').normalize('NFD').replace(/\p{M}/gu, '').toLowerCase();
}

/** Text matches the number (prefix) or any word of the name (prefix), accents and case ignored. */
export function filterFolders(folders: Folder[], f: FolderFilter): Folder[] {
  const text = plain(f.text.trim());
  return folders.filter((folder) => {
    if (f.type && folder.type !== f.type) return false;
    if (f.status && (folder.status ?? '') !== f.status) return false;
    if (f.handledBy && (folder.handledBy ?? '') !== f.handledBy) return false;
    if (!text) return true;
    if (/^\d+$/.test(text)) return String(folder.number ?? folder.id).startsWith(text);
    const name = plain(folder.name);
    return name.startsWith(text) || name.split(/[\s&.,-]+/).some((word) => word.startsWith(text));
  });
}

export function sortFolders(folders: Folder[], sort: Sort): Folder[] {
  const list = [...folders];
  switch (sort) {
    case 'number':
      return list.sort((a, b) => (b.number ?? b.id) - (a.number ?? a.id));
    case 'recent':
      return list.sort((a, b) => (b.openingDate ?? '').localeCompare(a.openingDate ?? '') || b.id - a.id);
    default:
      return list.sort((a, b) => plain(a.name).localeCompare(plain(b.name)));
  }
}

/** Distinct non-empty values, sorted, for the filters. */
export function distinct(folders: Folder[], key: 'status' | 'handledBy'): string[] {
  return [...new Set(folders.map((f) => f[key] ?? '').filter(Boolean))].sort((a, b) => a.localeCompare(b));
}

export function page<T>(list: T[], index: number, size: number): { items: T[]; pages: number; index: number } {
  const pages = Math.max(1, Math.ceil(list.length / size));
  const i = Math.min(Math.max(0, index), pages - 1);
  return { items: list.slice(i * size, i * size + size), pages, index: i };
}
