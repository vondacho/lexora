package io.lexora.infrastructure.persistence.honorary;

import io.lexora.infrastructure.persistence.support.AuditListener;
import io.lexora.infrastructure.persistence.support.Audited;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "invo_honoraries")
@EntityListeners(AuditListener.class)
public class HonoraryEntity implements Audited {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hono_id")
    Integer id;

    @Column(name = "hono_date")
    LocalDate date;

    @Column(name = "hono_folder_id", nullable = false)
    Integer folderId;

    @Column(name = "hono_desc", length = 1024)
    String description;

    @Column(name = "hono_minutes")
    Integer minutes;

    /** Hourly rate. */
    @Column(name = "hono_tariff")
    Float tariff;

    @Column(name = "hono_expense_1")
    Float expense1;

    @Column(name = "hono_expense_2")
    Float expense2;

    @Column(name = "hono_expense_3")
    Float expense3;

    @Column(name = "hono_expense_4")
    Float expense4;

    @Column(name = "hono_expense_desc_1", length = 50)
    String expenseDescription1;

    @Column(name = "hono_expense_desc_2", length = 50)
    String expenseDescription2;

    @Column(name = "hono_expense_desc_3", length = 50)
    String expenseDescription3;

    @Column(name = "hono_expense_desc_4", length = 50)
    String expenseDescription4;

    @Column(name = "hono_amount", nullable = false)
    Float amount;

    @Column(name = "hono_invo_id")
    Integer invoiceId;

    @Column(name = "hono_mute_date")
    LocalDate muteDate;

    @Column(name = "hono_mute_user", length = 10)
    String muteUser;

    @Override
    public void stamp(LocalDate date, String username) {
        this.muteDate = date;
        this.muteUser = username;
    }
}
