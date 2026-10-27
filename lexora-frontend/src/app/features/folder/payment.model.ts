import { CustomerStatement, OpenItem } from '../../api/generated';
import { toCents } from '../../shared/money';
import { invoiceRef, requestRef } from './timeline.model';

export interface Allocation {
  kind: 'invoice' | 'request';
  documentId: number;
  reference: string;
  remainingCents: number;
  cents: number;
  /** The payment covers what the customer still owed on the document. */
  settles: boolean;
}

export interface PaymentPlan {
  allocations: Allocation[];
  /** What is left once every open document is paid: a spontaneous advance. */
  surplusCents: number;
}

/** Spreads a payment over the customer's open invoices and requests, the oldest due date first. */
export function allocate(amountCents: number, statement: CustomerStatement | null): PaymentPlan {
  const open = [
    ...(statement?.invoices ?? []).map((i) => ({ kind: 'invoice' as const, item: i })),
    ...(statement?.requests ?? []).map((r) => ({ kind: 'request' as const, item: r })),
  ].sort((a, b) => sortKey(a.item).localeCompare(sortKey(b.item)) || a.kind.localeCompare(b.kind));
  let left = Math.max(0, amountCents);
  const allocations: Allocation[] = [];
  for (const { kind, item } of open) {
    const remaining = toCents(item.remaining);
    if (left <= 0 || remaining <= 0) {
      continue;
    }
    const cents = Math.min(left, remaining);
    left -= cents;
    allocations.push({
      kind,
      documentId: item.documentId ?? 0,
      reference: kind === 'invoice' ? invoiceRef(item.documentId) : requestRef(item.documentId),
      remainingCents: remaining,
      cents,
      settles: cents === remaining,
    });
  }
  return { allocations, surplusCents: left };
}

function sortKey(item: OpenItem): string {
  return (item.dueDate ?? item.date ?? '') + '|' + (item.date ?? '');
}
