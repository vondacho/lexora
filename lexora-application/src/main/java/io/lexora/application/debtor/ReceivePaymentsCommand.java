package io.lexora.application.debtor;

import io.lexora.domain.advance.AdvanceRequestId;
import io.lexora.domain.customer.CustomerId;
import io.lexora.domain.folder.FolderId;
import io.lexora.domain.invoice.InvoiceId;
import io.lexora.domain.shared.Money;
import io.lexora.domain.ticket.TicketId;

import java.time.LocalDate;
import java.util.List;

/**
 * Payments received from one customer, possibly through one payment slip, spread over several invoices and
 * advance requests (legacy "réception débiteur").
 */
public record ReceivePaymentsCommand(
        FolderId folderId,
        CustomerId customerId,
        TicketId ticketId,
        List<InvoiceLine> invoices,
        List<RequestLine> requests) {

    public ReceivePaymentsCommand {
        invoices = invoices == null ? List.of() : List.copyOf(invoices);
        requests = requests == null ? List.of() : List.copyOf(requests);
    }

    public record InvoiceLine(InvoiceId invoiceId, LocalDate date, Money amount, String iban, boolean cash) {
    }

    public record RequestLine(AdvanceRequestId requestId, LocalDate date, Money amount, String iban, boolean cash) {
    }
}
