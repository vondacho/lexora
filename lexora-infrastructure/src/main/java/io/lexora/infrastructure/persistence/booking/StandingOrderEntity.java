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
@Table(name = "book_orders")
@EntityListeners(AuditListener.class)
public class StandingOrderEntity implements Audited {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ord_id")
    Integer id;

    @Column(name = "ord_folder_id")
    Integer folderId;

    @Column(name = "ord_creditor", length = 80)
    String creditor;

    @Column(name = "ord_title", length = 80)
    String title;

    @Column(name = "ord_amount", nullable = false)
    Float amount;

    @Column(name = "ord_tax_rate", nullable = false)
    Float taxRate;

    @Column(name = "ord_tax", nullable = false)
    Float tax;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "ord_day", nullable = false)
    Integer day;

    @Column(name = "ord_enabled", nullable = false)
    Boolean enabled;

    @Column(name = "ord_mute_date")
    LocalDate muteDate;

    @Column(name = "ord_mute_user", length = 10)
    String muteUser;

    @Override
    public void stamp(LocalDate date, String username) {
        this.muteDate = date;
        this.muteUser = username;
    }
}
