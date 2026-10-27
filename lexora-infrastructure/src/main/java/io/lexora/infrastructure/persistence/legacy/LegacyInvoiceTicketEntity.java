package io.lexora.infrastructure.persistence.legacy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Former link between an invoice, a customer and his payment slip. */
@Entity
@Immutable
@Table(name = "invo_invoices_tickets")
public class LegacyInvoiceTicketEntity {

    @Id
    @Column(name = "invo_tick_id")
    Integer id;

    @Column(name = "invo_tick_invo_id", nullable = false)
    Integer invoiceId;

    @Column(name = "invo_tick_cust_id", nullable = false)
    Integer customerId;

    @Column(name = "invo_tick_tick_id", nullable = false)
    Integer ticketId;
}
