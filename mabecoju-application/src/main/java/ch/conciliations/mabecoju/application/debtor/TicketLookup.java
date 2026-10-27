package ch.conciliations.mabecoju.application.debtor;

import ch.conciliations.mabecoju.domain.advance.Advance;
import ch.conciliations.mabecoju.domain.customer.Customer;
import ch.conciliations.mabecoju.domain.folder.Folder;
import ch.conciliations.mabecoju.domain.invoice.InvoicePaymentRecord;
import ch.conciliations.mabecoju.domain.ticket.Ticket;

import java.util.List;

/** Who a payment slip belongs to and what was paid with it (legacy "recherche débiteur par BVR"). */
public record TicketLookup(
        Ticket ticket,
        Folder folder,
        Customer customer,
        List<InvoicePaymentRecord> payments,
        List<Advance> advances,
        CustomerStatement statement) {
}
