package io.lexora.domain.invoice;

import io.lexora.domain.folder.FolderId;

/** An invoice payment together with the invoice and folder it belongs to, for listings and reports. */
public record InvoicePaymentRecord(InvoiceId invoiceId, FolderId folderId, InvoicePayment payment) {
}
