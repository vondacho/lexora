package io.lexora.infrastructure.persistence.invoice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Share of an invoice owed by one customer. */
@Entity
@Table(name = "invo_invoices_distrib")
public class InvoiceDistributionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invo_dist_id")
    Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invo_dist_invo_id", nullable = false)
    InvoiceEntity invoice;

    @Column(name = "invo_dist_cust_id", nullable = false)
    Integer customerId;

    @Column(name = "invo_dist_amount", nullable = false)
    Float amount;

    InvoiceDistributionEntity() {
    }

    InvoiceDistributionEntity(InvoiceEntity invoice, Integer customerId, Float amount) {
        this.invoice = invoice;
        this.customerId = customerId;
        this.amount = amount;
    }
}
