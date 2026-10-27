package ch.conciliations.mabecoju.application.document;

import ch.conciliations.mabecoju.domain.shared.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Everything a renderer needs to produce a document, already computed and formatted by the application: the
 * renderer only lays it out.
 *
 * @param subject     "Facture", "Demande de provision", "Rappel"
 * @param reference   our reference, "{issuer shortcut}/{document id}"
 * @param modelTitle  title of the letter
 * @param content     letter body, placeholders filled: HTML, or plain text when {@code htmlContent} is false
 * @param pages       pages printed for each copy, in order
 * @param statement   detail of the invoice, for {@link PageKind#INVOICE_STATEMENT} pages; null otherwise
 * @param attachments number of attachments to mention on the first page of each copy
 * @param copies      one copy per recipient, or a single copy when addresses are merged
 */
public record PrintJob(
        String subject,
        String fileName,
        String reference,
        String vatNumber,
        LocalDate date,
        String modelTitle,
        String content,
        boolean htmlContent,
        List<PageKind> pages,
        InvoiceStatement statement,
        int attachments,
        List<Copy> copies) {

    public enum PageKind {
        /** Addresses, reference, title, letter text and signature. */
        LETTER,
        /** Addresses, reference and the detail of the invoice: honoraries, VAT, advances and balance per customer. */
        INVOICE_STATEMENT
    }

    /**
     * @param addressBlocks address lines, the copy's recipient first
     * @param paymentSlip   QR-bill to append to the copy, or null
     */
    public record Copy(List<List<String>> addressBlocks, PaymentSlip paymentSlip) {
    }

    /**
     * Data of a QR-bill payment part.
     *
     * @param reference QR reference or creditor reference
     * @param message   unstructured message ("Facture 123")
     */
    public record PaymentSlip(String debtorName, String debtorStreet, String debtorPostalCode, String debtorTown,
                              Money amount, String reference, String message) {
    }

    public record InvoiceStatement(
            List<Line> honoraries,
            Money honorariesSum,
            Money discount,
            BigDecimal taxRatePercent,
            Money tax,
            Money total,
            List<Balance> balances,
            int delayDays) {

        /** A billed service. */
        public record Line(LocalDate date, String description, int minutes, Money hourlyRate, Money expenses,
                           Money amount) {
        }

        /**
         * What one customer (or all of them, when {@code customerName} is null) has to pay.
         *
         * @param toBear   his part before advances; null when there is a single customer
         */
        public record Balance(String customerName, Money toBear, List<AdvanceLine> advances, Money advancesSum,
                              Money due) {
        }

        public record AdvanceLine(LocalDate date, Money amount) {
        }
    }
}
