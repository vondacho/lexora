package io.lexora.infrastructure.persistence.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Single-row table of booking defaults. */
@Entity
@Table(name = "book_defaults")
public class BookingDefaultsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "defa_id")
    Integer id;

    /** Net tax rate for VAT; the legacy column name says "income tax". */
    @Column(name = "defa_income_tax_rate", nullable = false)
    Float vatRate = 0f;
}
