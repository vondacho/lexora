package ch.conciliations.mabecoju.application.debtor;

import ch.conciliations.mabecoju.domain.advance.AdvanceRequestId;
import ch.conciliations.mabecoju.domain.customer.CustomerId;
import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.invoice.InvoiceId;
import ch.conciliations.mabecoju.domain.shared.Money;
import ch.conciliations.mabecoju.domain.ticket.TicketId;

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
