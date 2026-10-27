package io.lexora.application.reporting;

import io.lexora.domain.folder.FolderId;
import io.lexora.domain.shared.Money;
import io.lexora.domain.shared.Period;
import io.lexora.domain.shared.Rate;

import java.time.LocalDate;
import java.util.List;

/**
 * Money cashed in over a period and the VAT owed on it at the net tax rate (legacy "décompte TVA" and
 * "relevé des entrées").
 */
public record IncomeReport(
        Period period,
        List<IncomeLine> invoicePayments,
        List<IncomeLine> advances,
        Rate vatRate) {

    public Money invoicePaymentsTotal() {
        return invoicePayments.stream().collect(Money.summing(IncomeLine::amount));
    }

    public Money advancesTotal() {
        return advances.stream().collect(Money.summing(IncomeLine::amount));
    }

    public Money total() {
        return invoicePaymentsTotal().plus(advancesTotal());
    }

    /** VAT owed for the period: money received × net tax rate. */
    public Money vatDue() {
        return Money.of(vatRate.applyTo(total()));
    }

    /**
     * @param documentId   invoice id for invoice payments, advance request id (if any) for advances
     * @param ticketNumber reference of the payment slip used, if any
     */
    public record IncomeLine(long id, LocalDate date, FolderId folderId, String folderName, Long documentId,
                             Money amount, boolean cash, String ticketNumber, String iban) {
    }
}
