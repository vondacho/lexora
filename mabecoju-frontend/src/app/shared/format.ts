import { Pipe, PipeTransform } from '@angular/core';

/** Swiss francs as the office writes them: 1'230.40, minus sign for negative amounts. */
export function formatChf(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '–';
  }
  const [units, cents] = Math.abs(value).toFixed(2).split('.');
  const grouped = units.replace(/\B(?=(\d{3})+(?!\d))/g, "'");
  return (value < 0 && Number(value.toFixed(2)) !== 0 ? '−' : '') + grouped + '.' + cents;
}

/** ISO date (2026-10-24) as 24.10.2026, or 24.10 without the year. */
export function formatDate(iso: string | null | undefined, withYear = true): string {
  if (!iso) {
    return '';
  }
  const [year, month, day] = iso.split('-');
  return withYear ? `${day}.${month}.${year}` : `${day}.${month}`;
}

@Pipe({ name: 'chf' })
export class ChfPipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    return formatChf(value);
  }
}

@Pipe({ name: 'swissDate' })
export class SwissDatePipe implements PipeTransform {
  transform(value: string | null | undefined, withYear = true): string {
    return formatDate(value, withYear);
  }
}
