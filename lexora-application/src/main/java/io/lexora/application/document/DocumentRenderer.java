package io.lexora.application.document;

import io.lexora.domain.billing.PaymentReference;

/** Port producing the PDF of a document. */
public interface DocumentRenderer {

    /** References to print on QR-bills, depending on the configured creditor account; NONE without QR-bill. */
    PaymentReference.Style paymentReferenceStyle();

    byte[] render(PrintJob job);
}
