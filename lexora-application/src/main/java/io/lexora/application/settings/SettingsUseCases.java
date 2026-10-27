package io.lexora.application.settings;

import io.lexora.domain.settings.BookingSettings;
import io.lexora.domain.settings.InvoicingSettings;
import io.lexora.domain.settings.SettingsRepository;
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
