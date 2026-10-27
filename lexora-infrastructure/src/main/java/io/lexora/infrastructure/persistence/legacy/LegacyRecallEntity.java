package io.lexora.infrastructure.persistence.legacy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

/** Former invoice reminders table, replaced by {@code invo_invoices_recalls} (empty in the 2023 dump). */
@Entity
@Immutable
@Table(name = "invo_recalls")
public class LegacyRecallEntity {

    @Id
    @Column(name = "reca_id")
    Integer id;

    @Column(name = "reca_invo_id", nullable = false)
    Integer invoiceId;

    @Column(name = "reca_date")
    LocalDate date;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "reca_degree", nullable = false)
    Integer degree;

    @Column(name = "reca_delay")
    LocalDate delay;

    @Column(name = "reca_amount", nullable = false)
    Float amount;

    @Column(name = "reca_mute_user", length = 10)
    String muteUser;

    @Column(name = "reca_mute_date")
    LocalDate muteDate;
}
