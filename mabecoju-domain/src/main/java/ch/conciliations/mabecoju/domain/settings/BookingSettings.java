package ch.conciliations.mabecoju.domain.settings;

import ch.conciliations.mabecoju.domain.shared.Rate;

/**
 * Defaults of the booking module ({@code book_defaults}).
 *
 * @param vatRate net tax rate ("taux de la dette fiscale nette"): VAT owed is this share of the money received,
 *                VAT included; the VAT paid on expenses is not deducted
 */
public record BookingSettings(Rate vatRate) {
}
