package io.lexora.domain.settings;

import io.lexora.domain.shared.Money;
import io.lexora.domain.shared.Rate;

/**
 * Defaults of the invoicing module ({@code invo_defaults}).
 *
 * @param meetingHourlyRate  hourly rate proposed for meetings
 * @param standardHourlyRate hourly rate proposed for other services
 * @param invoiceDelayDays   payment term proposed on invoices
 * @param requestDelayDays   payment term proposed on advance requests
 * @param vatNumber          VAT registration number printed on documents
 * @param vatRate            VAT rate proposed on invoices
 */
public record InvoicingSettings(
        Money meetingHourlyRate,
        Money standardHourlyRate,
        int invoiceDelayDays,
        int requestDelayDays,
        String vatNumber,
        Rate vatRate) {
}
