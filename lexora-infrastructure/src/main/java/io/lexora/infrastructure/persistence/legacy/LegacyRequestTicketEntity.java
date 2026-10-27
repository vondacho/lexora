package io.lexora.infrastructure.persistence.legacy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Former link between an advance request, a customer and his payment slip. */
@Entity
@Immutable
@Table(name = "invo_requests_tickets")
public class LegacyRequestTicketEntity {

    @Id
    @Column(name = "req_tick_id")
    Integer id;

    @Column(name = "req_tick_req_id", nullable = false)
    Integer requestId;

    @Column(name = "req_tick_cust_id", nullable = false)
    Integer customerId;

    @Column(name = "req_tick_tick_id", nullable = false)
    Integer ticketId;
}
