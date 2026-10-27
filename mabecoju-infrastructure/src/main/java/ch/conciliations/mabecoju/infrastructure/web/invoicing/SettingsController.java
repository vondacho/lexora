package ch.conciliations.mabecoju.infrastructure.web.invoicing;

import ch.conciliations.mabecoju.application.settings.SettingsUseCases;
import ch.conciliations.mabecoju.domain.settings.BookingSettings;
import ch.conciliations.mabecoju.domain.settings.InvoicingSettings;
import ch.conciliations.mabecoju.infrastructure.web.Json;
import ch.conciliations.mabecoju.infrastructure.web.Permissions;
import ch.conciliations.mabecoju.infrastructure.web.api.SettingsApi;
import ch.conciliations.mabecoju.infrastructure.web.api.model.BookingSettingsDto;
import ch.conciliations.mabecoju.infrastructure.web.api.model.InvoicingSettingsDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SettingsController implements SettingsApi {

    private final SettingsUseCases settings;

    SettingsController(SettingsUseCases settings) {
        this.settings = settings;
    }

    @Override
    @PreAuthorize(Permissions.AUTHENTICATED)
    public InvoicingSettingsDto getInvoicingSettings() {
        return toDto(settings.invoicingSettings());
    }

    @Override
    @PreAuthorize(Permissions.INVOICING_PARAMETERS)
    public InvoicingSettingsDto changeInvoicingSettings(InvoicingSettingsDto in) {
        return toDto(settings.changeInvoicingSettings(new InvoicingSettings(Json.money(in.getMeetingHourlyRate()),
                Json.money(in.getStandardHourlyRate()), Json.intOrZero(in.getInvoiceDelayDays()),
                Json.intOrZero(in.getRequestDelayDays()), in.getVatNumber(), Json.rate(in.getVatRatePercent()))));
    }

    @Override
    @PreAuthorize(Permissions.AUTHENTICATED)
    public BookingSettingsDto getBookingSettings() {
        return new BookingSettingsDto(Json.percent(settings.bookingSettings().vatRate()));
    }

    @Override
    @PreAuthorize(Permissions.BOOKING_PARAMETERS)
    public BookingSettingsDto changeBookingSettings(BookingSettingsDto in) {
        BookingSettings changed = settings.changeBookingSettings(new BookingSettings(Json.rate(in.getVatRatePercent())));
        return new BookingSettingsDto(Json.percent(changed.vatRate()));
    }

    private static InvoicingSettingsDto toDto(InvoicingSettings s) {
        return new InvoicingSettingsDto(Json.amount(s.meetingHourlyRate()), Json.amount(s.standardHourlyRate()),
                Json.percent(s.vatRate()))
                .invoiceDelayDays(s.invoiceDelayDays()).requestDelayDays(s.requestDelayDays()).vatNumber(s.vatNumber());
    }
}
