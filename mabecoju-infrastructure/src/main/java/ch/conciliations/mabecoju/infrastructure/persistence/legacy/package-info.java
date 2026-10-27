/**
 * Tables of the legacy schema no longer used by the business logic. The links between payment slips and documents of
 * {@code invo_invoices_tickets} and {@code invo_requests_tickets} were copied into {@code invo_tickets} by Liquibase
 * change 0004; {@code invo_recalls} was superseded by {@code invo_invoices_recalls}. They are mapped read-only so that
 * the whole data model is described by JPA entities, until they are dropped.
 */
package ch.conciliations.mabecoju.infrastructure.persistence.legacy;
