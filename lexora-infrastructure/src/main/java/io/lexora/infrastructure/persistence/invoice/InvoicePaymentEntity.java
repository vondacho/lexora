package io.lexora.infrastructure.persistence.invoice;

import io.lexora.infrastructure.persistence.support.AuditListener;
import io.lexora.infrastructure.persistence.support.Audited;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** Money received for an invoice. */
@Entity
@Table(name = "invo_payments")
@EntityListeners(AuditListener.class)
public class InvoicePaymentEntity implements Audited {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pay_id")
    Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pay_invo_id", nullable = false)
    InvoiceEntity invoice;

    /** Read-only copy of the invoice reference, to avoid loading the invoice. */
    @Column(name = "pay_invo_id", insertable = false, updatable = false)
    Integer invoiceId;

    @Column(name = "pay_cust_id")
    Integer customerId;

    @Column(name = "pay_date")
    LocalDate date;

    @Column(name = "pay_tick_id")
    Integer ticketId;

    @Column(name = "pay_amount", nullable = false)
    Float amount;

    @Column(name = "pay_iban", length = 25)
    String iban;

    @Column(name = "pay_cash")
    Boolean cash;

    @Column(name = "pay_mute_date")
    LocalDate muteDate;

    @Column(name = "pay_mute_user", length = 10)
    String muteUser;

    @Override
    public void stamp(LocalDate date, String username) {
        this.muteDate = date;
        this.muteUser = username;
    }
}
