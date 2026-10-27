package ch.conciliations.mabecoju.infrastructure.persistence.invoice;

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

@Entity
@Table(name = "invo_invoices")
@EntityListeners(AuditListener.class)
public class InvoiceEntity implements Audited {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invo_id")
    Integer id;

    @Column(name = "invo_folder_id", nullable = false)
    Integer folderId;

    @Column(name = "invo_date")
    LocalDate date;

    @Column(name = "invo_hono_sum", nullable = false)
    Float honorariesSum;

    /** Sum of the deducted advances ("dépôts"). */
    @Column(name = "invo_depo_sum")
    Float advancedSum;

    @Column(name = "invo_disc_rate", nullable = false)
    Float discountRate;

    @Column(name = "invo_disc", nullable = false)
    Float discount;

    @Column(name = "invo_tax_rate", nullable = false)
    Float taxRate;

    @Column(name = "invo_tax", nullable = false)
    Float tax;

    @Column(name = "invo_amount", nullable = false)
    Float amount;

    @Column(name = "invo_payed_date")
    LocalDate paidOn;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "invo_delay", nullable = false)
    Integer delay;

    @Column(name = "invo_comment", columnDefinition = "text")
    String comment;

    @Column(name = "invo_mute_date")
    LocalDate muteDate;

    @Column(name = "invo_mute_user", length = 10)
    String muteUser;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("customerId")
    List<InvoiceDistributionEntity> distribution = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("date desc, id desc")
    List<InvoiceRecallEntity> recalls = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("date asc, id asc")
    List<InvoicePaymentEntity> payments = new ArrayList<>();

    @Override
    public void stamp(LocalDate date, String username) {
        this.muteDate = date;
        this.muteUser = username;
    }
}
