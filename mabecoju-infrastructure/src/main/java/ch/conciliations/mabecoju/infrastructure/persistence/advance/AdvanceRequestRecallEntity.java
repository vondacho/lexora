package ch.conciliations.mabecoju.infrastructure.persistence.advance;

import ch.conciliations.mabecoju.infrastructure.persistence.support.AuditListener;
import ch.conciliations.mabecoju.infrastructure.persistence.support.Audited;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Table(name = "invo_requests_recalls")
@EntityListeners(AuditListener.class)
public class AdvanceRequestRecallEntity implements Audited {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reca_id")
    Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reca_req_id", nullable = false)
    AdvanceRequestEntity request;

    @Column(name = "reca_date")
    LocalDate date;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "reca_degree", nullable = false)
    Integer degree;

    /** Unused legacy column. */
    @Column(name = "reca_delay")
    LocalDate delay;

    @Column(name = "reca_amount", nullable = false)
    Float amount;

    @Column(name = "reca_mute_user", length = 10)
    String muteUser;

    @Column(name = "reca_mute_date")
    LocalDate muteDate;

    @Override
    public void stamp(LocalDate date, String username) {
        this.muteDate = date;
        this.muteUser = username;
    }
}
