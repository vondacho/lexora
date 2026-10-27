package ch.conciliations.mabecoju.infrastructure.persistence.advance;

import ch.conciliations.mabecoju.infrastructure.persistence.support.AuditListener;
import ch.conciliations.mabecoju.infrastructure.persistence.support.Audited;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Advance (provision) request, legacy table {@code invo_requests}. */
@Entity
@Table(name = "invo_requests")
@EntityListeners(AuditListener.class)
public class AdvanceRequestEntity implements Audited {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "req_id")
    Integer id;

    @Column(name = "req_folder_id", nullable = false)
    Integer folderId;

    @Column(name = "req_date")
    LocalDate date;

    @Column(name = "req_amount", nullable = false)
    Float amount;

    @Column(name = "req_payed_date")
    LocalDate paidOn;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "req_model_id")
    Integer modelCode;

    @Column(name = "req_meeting_date")
    LocalDate meetingDate;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "req_delay", nullable = false)
    Integer delay;

    @Column(name = "req_hono_sum", nullable = false)
    Float honorariesSum;

    @Column(name = "req_depo_sum")
    Float advancedSum;

    @Column(name = "req_comment", columnDefinition = "text")
    String comment;

    @Column(name = "req_mute_date")
    LocalDate muteDate;

    @Column(name = "req_mute_user", length = 10)
    String muteUser;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("customerId")
    List<AdvanceRequestDistributionEntity> distribution = new ArrayList<>();

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("date desc, id desc")
    List<AdvanceRequestRecallEntity> recalls = new ArrayList<>();

    @Override
    public void stamp(LocalDate date, String username) {
        this.muteDate = date;
        this.muteUser = username;
    }
}
