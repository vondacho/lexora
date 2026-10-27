package ch.conciliations.mabecoju.infrastructure.persistence.settings;

import ch.conciliations.mabecoju.domain.settings.BookingSettings;
import ch.conciliations.mabecoju.domain.settings.InvoicingSettings;
import ch.conciliations.mabecoju.domain.settings.SettingsRepository;
import org.springframework.stereotype.Repository;

import static ch.conciliations.mabecoju.infrastructure.persistence.support.Conversions.money;
import static ch.conciliations.mabecoju.infrastructure.persistence.support.Conversions.rate;
import static ch.conciliations.mabecoju.infrastructure.persistence.support.Conversions.toFloat;

/** The defaults live in the first row of single-row tables; the row is created when missing. */
@Repository
class SettingsRepositoryAdapter implements SettingsRepository {

    private final InvoicingDefaultsJpaRepository invoicing;
    private final BookingDefaultsJpaRepository booking;

    SettingsRepositoryAdapter(InvoicingDefaultsJpaRepository invoicing, BookingDefaultsJpaRepository booking) {
        this.invoicing = invoicing;
        this.booking = booking;
    }

    @Override
    public InvoicingSettings invoicing() {
        InvoicingDefaultsEntity e = invoicing.findFirstByOrderByIdAsc().orElseGet(InvoicingDefaultsEntity::new);
        return new InvoicingSettings(money(e.tariffMeeting), money(e.tariffOther), e.invoiceDelay, e.requestDelay,
                e.vatNumber, rate(e.vatRate));
    }

    @Override
    public void save(InvoicingSettings settings) {
        InvoicingDefaultsEntity e = invoicing.findFirstByOrderByIdAsc().orElseGet(InvoicingDefaultsEntity::new);
        e.tariffMeeting = toFloat(settings.meetingHourlyRate());
        e.tariffOther = toFloat(settings.standardHourlyRate());
        e.invoiceDelay = settings.invoiceDelayDays();
        e.requestDelay = settings.requestDelayDays();
        e.vatNumber = settings.vatNumber();
        e.vatRate = toFloat(settings.vatRate());
        invoicing.saveAndFlush(e);
    }

    @Override
    public BookingSettings booking() {
        BookingDefaultsEntity e = booking.findFirstByOrderByIdAsc().orElseGet(BookingDefaultsEntity::new);
        return new BookingSettings(rate(e.vatRate));
    }

    @Override
    public void save(BookingSettings settings) {
        BookingDefaultsEntity e = booking.findFirstByOrderByIdAsc().orElseGet(BookingDefaultsEntity::new);
        e.vatRate = toFloat(settings.vatRate());
        booking.saveAndFlush(e);
    }
}
