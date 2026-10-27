package ch.conciliations.mabecoju.domain.settings;

public interface SettingsRepository {

    InvoicingSettings invoicing();

    void save(InvoicingSettings settings);

    BookingSettings booking();

    void save(BookingSettings settings);
}
