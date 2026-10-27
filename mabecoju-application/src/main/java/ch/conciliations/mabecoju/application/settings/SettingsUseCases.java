package ch.conciliations.mabecoju.application.settings;

import ch.conciliations.mabecoju.domain.settings.BookingSettings;
import ch.conciliations.mabecoju.domain.settings.InvoicingSettings;
import ch.conciliations.mabecoju.domain.settings.SettingsRepository;
import jakarta.transaction.Transactional;

/** Reading and changing the module defaults. */
@Transactional
public class SettingsUseCases {

    private final SettingsRepository settings;

    public SettingsUseCases(SettingsRepository settings) {
        this.settings = settings;
    }

    public InvoicingSettings invoicingSettings() {
        return settings.invoicing();
    }

    public InvoicingSettings changeInvoicingSettings(InvoicingSettings changed) {
        settings.save(changed);
        return settings.invoicing();
    }

    public BookingSettings bookingSettings() {
        return settings.booking();
    }

    public BookingSettings changeBookingSettings(BookingSettings changed) {
        settings.save(changed);
        return settings.booking();
    }
}
