package ch.conciliations.mabecoju.domain.invoice;

import ch.conciliations.mabecoju.domain.folder.FolderId;

/** An invoice payment together with the invoice and folder it belongs to, for listings and reports. */
public record InvoicePaymentRecord(InvoiceId invoiceId, FolderId folderId, InvoicePayment payment) {
}
