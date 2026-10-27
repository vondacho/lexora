package io.lexora.infrastructure.persistence.ticket;

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
@Table(name = "invo_tickets")
@EntityListeners(AuditListener.class)
public class TicketEntity implements Audited {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tick_id")
    Integer id;

    @Column(name = "tick_number", length = 128, nullable = false)
    String number;

    @Column(name = "tick_folder_id")
    Integer folderId;

    @Column(name = "tick_customer_id")
    Integer customerId;

    /** Invoice the slip was issued for (added by Liquibase change 0003). */
    @Column(name = "tick_invo_id")
    Integer invoiceId;

    /** Advance request the slip was issued for (added by Liquibase change 0003). */
    @Column(name = "tick_req_id")
    Integer requestId;

    @Column(name = "tick_mute_date")
    LocalDate muteDate;

    @Column(name = "tick_mute_user", length = 10)
    String muteUser;

    @Override
    public void stamp(LocalDate date, String username) {
        this.muteDate = date;
        this.muteUser = username;
    }
}
