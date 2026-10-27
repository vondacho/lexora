package ch.conciliations.mabecoju.infrastructure.web.booking;

import ch.conciliations.mabecoju.application.reporting.VatUseCases;
import ch.conciliations.mabecoju.domain.booking.VatPayment;
import ch.conciliations.mabecoju.domain.booking.VatPaymentId;
import ch.conciliations.mabecoju.domain.shared.Period;
import ch.conciliations.mabecoju.infrastructure.web.Json;
import ch.conciliations.mabecoju.infrastructure.web.Permissions;
import ch.conciliations.mabecoju.infrastructure.web.api.VatApi;
import ch.conciliations.mabecoju.infrastructure.web.api.model.VatPaymentDto;
import ch.conciliations.mabecoju.infrastructure.web.api.model.VatPaymentInputDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static ch.conciliations.mabecoju.infrastructure.web.Json.amount;

@RestController
class VatController implements VatApi {

    private final VatUseCases vat;

    VatController(VatUseCases vat) {
        this.vat = vat;
    }

    @Override
    @PreAuthorize(Permissions.BOOKING_TAX_SUMMARY)
    public List<VatPaymentDto> listVatPayments() {
        return Json.map(vat.listPayments(), VatController::toDto);
    }

    @Override
    @PreAuthorize(Permissions.BOOKING_VAT_PAYMENT)
    public VatPaymentDto recordVatPayment(VatPaymentInputDto input) {
        return toDto(vat.recordPayment(Period.of(input.getFrom(), input.getTo()),
                input.getAmount() == null ? null : Json.money(input.getAmount()), input.getPaidOn()));
    }

    @Override
    @PreAuthorize(Permissions.BOOKING_VAT_PAYMENT)
    public void deleteVatPayment(Long id) {
        vat.deletePayment(VatPaymentId.of(id));
    }

    private static VatPaymentDto toDto(VatPayment p) {
        return new VatPaymentDto().id(p.id().value()).from(p.period().from()).to(p.period().to())
                .revenue(amount(p.revenue())).vatRatePercent(Json.percent(p.rate())).vatDue(amount(p.vatDue()))
                .amount(amount(p.amount())).paidOn(p.paidOn())
                .expenseId(p.expenseId() == null ? null : p.expenseId().value());
    }
}
