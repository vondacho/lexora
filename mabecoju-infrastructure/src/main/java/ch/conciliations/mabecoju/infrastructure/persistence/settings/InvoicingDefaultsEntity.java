package ch.conciliations.mabecoju.infrastructure.persistence.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Single-row table of invoicing defaults. */
@Entity
@Table(name = "invo_defaults")
public class InvoicingDefaultsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "defa_id")
    Integer id;

    @Column(name = "defa_tariff_meeting", nullable = false)
    Float tariffMeeting = 0f;

    @Column(name = "defa_tariff_other", nullable = false)
    Float tariffOther = 0f;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "defa_invo_delay", nullable = false)
    Integer invoiceDelay = 0;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "defa_req_delay", nullable = false)
    Integer requestDelay = 0;

    @Column(name = "defa_tva_no", length = 10)
    String vatNumber;

    @Column(name = "defa_tva_rate", nullable = false)
    Float vatRate = 0f;
}
