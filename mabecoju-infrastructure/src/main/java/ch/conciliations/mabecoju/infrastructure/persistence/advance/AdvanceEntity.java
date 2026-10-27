package ch.conciliations.mabecoju.infrastructure.persistence.advance;

import ch.conciliations.mabecoju.infrastructure.persistence.support.AuditListener;
import ch.conciliations.mabecoju.infrastructure.persistence.support.Audited;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "invo_advances")
@EntityListeners(AuditListener.class)
public class AdvanceEntity implements Audited {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "adva_id")
    Integer id;

    @Column(name = "adva_fold_id")
    Integer folderId;

    @Column(name = "adva_req_id")
    Integer requestId;

    @Column(name = "adva_cust_id")
    Integer customerId;

    @Column(name = "adva_date")
    LocalDate date;

    @Column(name = "adva_tick_id")
    Integer ticketId;

    @Column(name = "adva_amount", nullable = false)
    Float amount;

    @Column(name = "adva_invo_id")
    Integer invoiceId;

    @Column(name = "adva_cash")
    Boolean cash;

    @Column(name = "adva_iban", length = 25)
    String iban;

    @Column(name = "adva_mute_date")
    LocalDate muteDate;

    @Column(name = "adva_mute_user", length = 10)
    String muteUser;

    @Override
    public void stamp(LocalDate date, String username) {
        this.muteDate = date;
        this.muteUser = username;
    }
}
