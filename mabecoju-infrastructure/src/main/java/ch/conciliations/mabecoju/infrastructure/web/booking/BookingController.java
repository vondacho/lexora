package ch.conciliations.mabecoju.infrastructure.web.booking;

import ch.conciliations.mabecoju.application.booking.BookingUseCases;
import ch.conciliations.mabecoju.domain.booking.OutgoingPayment;
import ch.conciliations.mabecoju.domain.booking.OutgoingPaymentDetails;
import ch.conciliations.mabecoju.domain.booking.OutgoingPaymentId;
import ch.conciliations.mabecoju.domain.booking.PaymentKind;
import ch.conciliations.mabecoju.domain.booking.StandingOrder;
import ch.conciliations.mabecoju.domain.booking.StandingOrderDetails;
import ch.conciliations.mabecoju.domain.booking.StandingOrderId;
import ch.conciliations.mabecoju.domain.booking.VatPayment;
import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.infrastructure.web.Json;
import ch.conciliations.mabecoju.infrastructure.web.Permissions;
import ch.conciliations.mabecoju.infrastructure.web.api.BookingApi;
import ch.conciliations.mabecoju.infrastructure.web.api.model.ExecutionInputDto;
import ch.conciliations.mabecoju.infrastructure.web.api.model.NextExecutionDto;
import ch.conciliations.mabecoju.infrastructure.web.api.model.OutgoingPaymentDto;
import ch.conciliations.mabecoju.infrastructure.web.api.model.OutgoingPaymentInputDto;
import ch.conciliations.mabecoju.infrastructure.web.api.model.PaymentKindDto;
import ch.conciliations.mabecoju.infrastructure.web.api.model.StandingOrderDto;
import ch.conciliations.mabecoju.infrastructure.web.api.model.StandingOrderInputDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static ch.conciliations.mabecoju.infrastructure.web.Json.amount;
import static ch.conciliations.mabecoju.infrastructure.web.Json.percent;

/** Books of the office and of tutelage folders. */
@RestController
class BookingController implements BookingApi {

    private final BookingUseCases booking;

    BookingController(BookingUseCases booking) {
        this.booking = booking;
    }

    // ----- outgoing payments -----------------------------------------------------------------------------------

    @Override
    @PreAuthorize(Permissions.BOOKING_PAYMENTS_LIST)
    public List<OutgoingPaymentDto> listOfficePayments() {
        return payments(booking.listPayments(null));
    }

    @Override
    @PreAuthorize(Permissions.BOOKING_PAYMENTS_LIST)
    public List<OutgoingPaymentDto> listFolderPayments(Long folderId) {
        return payments(booking.listPayments(FolderId.of(folderId)));
    }

    @Override
    @PreAuthorize(Permissions.BOOKING_PAYMENT_CREATE)
    public OutgoingPaymentDto recordOfficePayment(OutgoingPaymentInputDto input) {
        return payment(booking.recordPayment(null, toDetails(input)));
    }

    @Override
    @PreAuthorize(Permissions.BOOKING_PAYMENT_CREATE)
    public OutgoingPaymentDto recordFolderPayment(Long folderId, OutgoingPaymentInputDto input) {
        return payment(booking.recordPayment(FolderId.of(folderId), toDetails(input)));
    }

    @Override
    @PreAuthorize(Permissions.BOOKING_PAYMENT_DISPLAY)
    public OutgoingPaymentDto getOutgoingPayment(Long id) {
        return payment(booking.getPayment(OutgoingPaymentId.of(id)));
    }

    @Override
    @PreAuthorize(Permissions.BOOKING_PAYMENT_MODIFY)
    public OutgoingPaymentDto updateOutgoingPayment(Long id, OutgoingPaymentInputDto input) {
        return payment(booking.updatePayment(OutgoingPaymentId.of(id), toDetails(input)));
    }

    @Override
    @PreAuthorize(Permissions.BOOKING_PAYMENT_MODIFY)
    public void deleteOutgoingPayment(Long id) {
        booking.deletePayment(OutgoingPaymentId.of(id));
    }

    // ----- standing orders -------------------------------------------------------------------------------------

    @Override
    @PreAuthorize(Permissions.BOOKING_ORDERS_LIST)
    public List<StandingOrderDto> listOfficeStandingOrders() {
        return Json.map(booking.listOrders(null), BookingController::toDto);
    }

    @Override
    @PreAuthorize(Permissions.BOOKING_ORDERS_LIST)
    public List<StandingOrderDto> listFolderStandingOrders(Long folderId) {
        return Json.map(booking.listOrders(FolderId.of(folderId)), BookingController::toDto);
    }

    @Override
    @PreAuthorize(Permissions.BOOKING_ORDER_CREATE)
    public StandingOrderDto createOfficeStandingOrder(StandingOrderInputDto input) {
        return toDto(booking.createOrder(null, toDetails(input)));
    }

    @Override
    @PreAuthorize(Permissions.BOOKING_ORDER_CREATE)
    public StandingOrderDto createFolderStandingOrder(Long folderId, StandingOrderInputDto input) {
        return toDto(booking.createOrder(FolderId.of(folderId), toDetails(input)));
    }

    @Override
    @PreAuthorize(Permissions.BOOKING_ORDERS_LIST)
    public StandingOrderDto getStandingOrder(Long id) {
        return toDto(booking.getOrder(StandingOrderId.of(id)));
    }

    @Override
    @PreAuthorize(Permissions.BOOKING_ORDER_MODIFY)
    public StandingOrderDto updateStandingOrder(Long id, StandingOrderInputDto input) {
        return toDto(booking.updateOrder(StandingOrderId.of(id), toDetails(input)));
    }

    @Override
    @PreAuthorize(Permissions.BOOKING_ORDER_MODIFY)
    public void deleteStandingOrder(Long id) {
        booking.deleteOrder(StandingOrderId.of(id));
    }

    @Override
    @PreAuthorize(Permissions.BOOKING_ORDERS)
    public NextExecutionDto getNextExecution(Long folderId) {
        YearMonth month = booking.nextExecutionMonth(folderId == null ? null : FolderId.of(folderId));
        return new NextExecutionDto().month(month.toString());
    }

    @Override
    @PreAuthorize(Permissions.BOOKING_ORDER_MODIFY)
    public List<OutgoingPaymentDto> executeStandingOrders(ExecutionInputDto input) {
        Long folderId = input == null ? null : input.getFolderId();
        String month = input == null ? null : input.getMonth();
        return payments(booking.executeOrders(folderId == null ? null : FolderId.of(folderId),
                month == null ? null : YearMonth.parse(month)));
    }

    private static OutgoingPaymentDetails toDetails(OutgoingPaymentInputDto in) {
        return new OutgoingPaymentDetails(Json.sameName(in.getKind(), PaymentKind.class), in.getDate(), in.getNumber(),
                in.getCreditor(), in.getTitle(), in.getTicketNumber(), Json.money(in.getAmount()),
                Json.rate(in.getTaxRatePercent()), in.getPaidOn());
    }

    private List<OutgoingPaymentDto> payments(List<OutgoingPayment> list) {
        Map<OutgoingPaymentId, VatPayment> vat = booking.vatPaymentsByExpense();
        return Json.map(list, p -> toDto(p, vat.get(p.id())));
    }

    private OutgoingPaymentDto payment(OutgoingPayment p) {
        return toDto(p, booking.vatPaymentsByExpense().get(p.id()));
    }

    /** @param vat the VAT payment the expense was booked for, or null */
    private static OutgoingPaymentDto toDto(OutgoingPayment p, VatPayment vat) {
        OutgoingPaymentDetails d = p.details();
        return new OutgoingPaymentDto(p.id().value()).folderId(Json.id(p.folderId()))
                .kind(Json.sameName(d.kind(), PaymentKindDto.class)).date(d.date()).number(d.number())
                .creditor(d.creditor()).title(d.title()).ticketNumber(d.ticketNumber()).amount(amount(d.amount()))
                .taxRatePercent(percent(d.taxRate())).tax(amount(p.tax())).paid(p.isPaid()).paidOn(d.paidOn())
                .editable(!p.isPermanent() && vat == null)
                .vatPaymentId(vat == null ? null : vat.id().value());
    }

    private static StandingOrderDetails toDetails(StandingOrderInputDto in) {
        return new StandingOrderDetails(in.getCreditor(), in.getTitle(), Json.money(in.getAmount()),
                Json.rate(in.getTaxRatePercent()), in.getDayOfMonth(), Json.isTrue(in.getEnabled()));
    }

    private static StandingOrderDto toDto(StandingOrder o) {
        StandingOrderDetails d = o.details();
        return new StandingOrderDto(o.id().value()).folderId(Json.id(o.folderId())).creditor(d.creditor())
                .title(d.title()).amount(amount(d.amount())).taxRatePercent(percent(d.taxRate()))
                .tax(amount(o.tax())).dayOfMonth(d.dayOfMonth()).enabled(d.enabled());
    }
}
