package ch.conciliations.mabecoju.infrastructure.persistence.booking;

import ch.conciliations.mabecoju.infrastructure.persistence.support.AuditListener;
import ch.conciliations.mabecoju.infrastructure.persistence.support.Audited;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/** New in v2 (changesets 0005 and 0006): exact decimals rather than the legacy floats. */
@Entity
@Table(name = "book_vat_payments")
@EntityListeners(AuditListener.class)
public class VatPaymentEntity implements Audited {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vat_id")
    Integer id;

    @Column(name = "vat_from", nullable = false)
    LocalDate from;

    @Column(name = "vat_to", nullable = false)
    LocalDate to;

    @Column(name = "vat_revenue", nullable = false, precision = 12, scale = 2)
    BigDecimal revenue;

    @Column(name = "vat_rate", nullable = false, precision = 6, scale = 4)
    BigDecimal rate;

    @Column(name = "vat_amount", nullable = false, precision = 12, scale = 2)
    BigDecimal amount;

    @Column(name = "vat_paid_on", nullable = false)
    LocalDate paidOn;

    /** Office expense booked for the payment ({@code book_payments.pay_id}), changeset 0006. */
    @Column(name = "vat_expense_id")
    Integer expenseId;

    @Column(name = "vat_mute_date")
    LocalDate muteDate;

    @Column(name = "vat_mute_user", length = 10)
    String muteUser;

    @Override
    public void stamp(LocalDate date, String username) {
        this.muteDate = date;
        this.muteUser = username;
    }
}
