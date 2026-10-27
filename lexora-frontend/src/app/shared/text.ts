/** Text without accents, in lower case, for searches that ignore both. */
export function plain(text: string | undefined | null): string {
  return (text ?? '').normalize('NFD').replace(/\p{M}/gu, '').toLowerCase();
}
