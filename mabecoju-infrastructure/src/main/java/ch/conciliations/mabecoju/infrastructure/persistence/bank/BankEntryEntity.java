package ch.conciliations.mabecoju.infrastructure.persistence.bank;

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

/** New in v2 (changeset 0007): a bank transaction imported from a camt.054 notification. */
@Entity
@Table(name = "invo_bank_entries")
@EntityListeners(AuditListener.class)
public class BankEntryEntity implements Audited {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bank_id")
    Integer id;

    @Column(name = "bank_ref", nullable = false, length = 80)
    String reference;

    @Column(name = "bank_date")
    LocalDate date;

    @Column(name = "bank_amount", nullable = false, precision = 12, scale = 2)
    BigDecimal amount;

    @Column(name = "bank_iban", length = 34)
    String iban;

    @Column(name = "bank_mute_date")
    LocalDate muteDate;

    @Column(name = "bank_mute_user", length = 10)
    String muteUser;

    @Override
    public void stamp(LocalDate date, String username) {
        this.muteDate = date;
        this.muteUser = username;
    }
}
