package io.lexora.application.debtor;

import io.lexora.domain.advance.Advance;
import io.lexora.domain.customer.Customer;
import io.lexora.domain.folder.Folder;
import io.lexora.domain.invoice.InvoicePaymentRecord;
import io.lexora.domain.ticket.Ticket;

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
