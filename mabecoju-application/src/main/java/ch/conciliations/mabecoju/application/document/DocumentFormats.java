package ch.conciliations.mabecoju.application.document;

import ch.conciliations.mabecoju.domain.shared.Money;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formats of the legacy documents: "1234.50" and "24 octobre 2026". */
public final class DocumentFormats {

    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private DocumentFormats() {
    }

    public static String amount(Money money) {
        return money == null ? "" : money.amount().toPlainString();
    }

    public static String longDate(LocalDate date) {
        return date == null ? "" : LONG_DATE.format(date);
    }

    public static String shortDate(LocalDate date) {
        return date == null ? "" : SHORT_DATE.format(date);
    }
}
