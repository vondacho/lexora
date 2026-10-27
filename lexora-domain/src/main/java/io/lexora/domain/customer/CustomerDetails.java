package io.lexora.domain.customer;

/**
 * @param title   free text salutation ("Madame", "Monsieur", "Mme &amp; M.", ...)
 * @param billing true when this customer pays the whole invoice instead of his own share
 */
public record CustomerDetails(
        String title,
        String firstName,
        String lastName,
        PostalAddress address,
        ContactDetails contact,
        String comment,
        boolean billing,
        ActivityPeriod activity) {
}
