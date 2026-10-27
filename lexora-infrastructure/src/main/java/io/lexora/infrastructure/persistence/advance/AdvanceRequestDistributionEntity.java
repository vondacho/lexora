package io.lexora.infrastructure.persistence.advance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "invo_requests_distrib")
public class AdvanceRequestDistributionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "req_dist_id")
    Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "req_dist_req_id", nullable = false)
    AdvanceRequestEntity request;

    @Column(name = "req_dist_cust_id", nullable = false)
    Integer customerId;

    @Column(name = "req_dist_amount", nullable = false)
    Float amount;

    AdvanceRequestDistributionEntity() {
    }

    AdvanceRequestDistributionEntity(AdvanceRequestEntity request, Integer customerId, Float amount) {
        this.request = request;
        this.customerId = customerId;
        this.amount = amount;
    }
}
