package io.lexora.infrastructure.persistence.booking;

import io.lexora.infrastructure.persistence.support.AuditListener;
import io.lexora.infrastructure.persistence.support.Audited;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Table(name = "book_payments")
@EntityListeners(AuditListener.class)
public class OutgoingPaymentEntity implements Audited {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pay_id")
    Integer id;

    @Column(name = "pay_folder_id")
    Integer folderId;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "pay_kind", nullable = false)
    Integer kind;

    @Column(name = "pay_date")
    LocalDate date;

    @Column(name = "pay_no", length = 20)
    String number;

    @Column(name = "pay_creditor", length = 80)
    String creditor;

    @Column(name = "pay_title", length = 80)
    String title;

    @Column(name = "pay_amount", nullable = false)
    Float amount;

    @Column(name = "pay_tax_rate", nullable = false)
    Float taxRate;

    @Column(name = "pay_tax", nullable = false)
    Float tax;

    @Column(name = "pay_ticket", length = 80)
    String ticketNumber;

    @Column(name = "pay_payed_date")
    LocalDate paidOn;

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
