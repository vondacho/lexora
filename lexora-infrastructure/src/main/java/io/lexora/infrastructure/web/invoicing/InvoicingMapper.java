package io.lexora.infrastructure.web.invoicing;

import io.lexora.application.advance.AdvanceRequestSituation;
import io.lexora.domain.advance.Advance;
import io.lexora.domain.advance.AdvanceRequest;
import io.lexora.domain.advance.AdvanceRequestTerms;
import io.lexora.domain.customer.ActivityPeriod;
import io.lexora.domain.customer.ContactDetails;
import io.lexora.domain.customer.Customer;
import io.lexora.domain.customer.CustomerDetails;
import io.lexora.domain.customer.PostalAddress;
import io.lexora.domain.folder.Folder;
import io.lexora.domain.folder.FolderDetails;
import io.lexora.domain.folder.FolderType;
import io.lexora.domain.honorary.Expense;
import io.lexora.domain.honorary.Honorary;
import io.lexora.domain.honorary.HonoraryWork;
import io.lexora.domain.invoice.Invoice;
import io.lexora.domain.invoice.InvoicePayment;
import io.lexora.domain.invoice.InvoiceTerms;
import io.lexora.infrastructure.web.Json;
import io.lexora.infrastructure.web.api.model.AdvanceDto;
import io.lexora.infrastructure.web.api.model.AdvanceRequestDto;
import io.lexora.infrastructure.web.api.model.AdvanceRequestTermsInputDto;
import io.lexora.infrastructure.web.api.model.CustomerDto;
import io.lexora.infrastructure.web.api.model.CustomerInputDto;
import io.lexora.infrastructure.web.api.model.CustomerShareDto;
import io.lexora.infrastructure.web.api.model.ExpenseDto;
import io.lexora.infrastructure.web.api.model.FolderDto;
import io.lexora.infrastructure.web.api.model.FolderInputDto;
import io.lexora.infrastructure.web.api.model.FolderTypeDto;
import io.lexora.infrastructure.web.api.model.HonoraryDto;
import io.lexora.infrastructure.web.api.model.HonoraryInputDto;
import io.lexora.infrastructure.web.api.model.InvoiceDto;
import io.lexora.infrastructure.web.api.model.InvoicePaymentDto;
import io.lexora.infrastructure.web.api.model.InvoiceTermsInputDto;

import static io.lexora.infrastructure.web.Json.amount;
import static io.lexora.infrastructure.web.Json.id;
import static io.lexora.infrastructure.web.Json.map;
import static io.lexora.infrastructure.web.Json.percent;

/** Mapping between the invoicing domain and the generated API models. */
final class InvoicingMapper {

    private InvoicingMapper() {
    }

    // ----- folders ---------------------------------------------------------------------------------------------------

    static FolderDetails toDetails(FolderInputDto in) {
        return new FolderDetails(Json.sameName(in.getType(), FolderType.class), in.getOpeningDate(), in.getName(),
                in.getPath(), in.getStatus(), in.getFirstDate(), in.getHandledBy(), in.getComment(),
                in.getWayToOurOffice());
    }

    static FolderDto toDto(Folder folder) {
        FolderDetails d = folder.details();
        return new FolderDto(folder.id().value()).number(folder.number())
                .type(Json.sameName(d.type(), FolderTypeDto.class)).openingDate(d.openingDate()).name(d.name())
                .path(d.path()).status(d.status()).firstDate(d.firstDate()).handledBy(d.handledBy())
                .comment(d.comment()).wayToOurOffice(d.wayToOurOffice());
    }

    // ----- customers -------------------------------------------------------------------------------------------------

    static CustomerDetails toDetails(CustomerInputDto in) {
        return new CustomerDetails(in.getTitle(), in.getFirstName(), in.getLastName(),
                new PostalAddress(in.getStreet(), in.getZip(), in.getCity()),
                new ContactDetails(in.getPrivatePhone(), in.getProfessionalPhone(), in.getMobilePhone(),
                        in.getMobilePhone2(), in.getEmail(), in.getEmail2(), in.getEmail3()),
                in.getComment(), Json.isTrue(in.getBilling()), new ActivityPeriod(in.getActiveSince(), in.getActiveUntil()));
    }

    static CustomerDto toDto(Customer customer) {
        CustomerDetails d = customer.details();
        PostalAddress a = d.address();
        ContactDetails c = d.contact();
        ActivityPeriod p = d.activity() == null ? ActivityPeriod.ALWAYS : d.activity();
        return new CustomerDto(customer.id().value()).folderId(customer.folderId().value())
                .fullName(customer.fullName()).title(d.title()).firstName(d.firstName()).lastName(d.lastName())
                .street(a.street()).zip(a.zip()).city(a.city())
                .privatePhone(c.privatePhone()).professionalPhone(c.professionalPhone())
                .mobilePhone(c.mobilePhone()).mobilePhone2(c.mobilePhone2())
                .email(c.email()).email2(c.email2()).email3(c.email3())
                .comment(d.comment()).billing(d.billing()).activeSince(p.since()).activeUntil(p.until());
    }

    // ----- honoraries ------------------------------------------------------------------------------------------------

    static HonoraryWork toWork(HonoraryInputDto in) {
        return new HonoraryWork(in.getDate(), in.getDescription(), Json.intOrZero(in.getMinutes()),
                Json.money(in.getHourlyRate()),
                map(in.getExpenses(), e -> new Expense(Json.money(e.getAmount()), e.getDescription())));
    }

    static HonoraryDto toDto(Honorary h) {
        HonoraryWork w = h.work();
        return new HonoraryDto(h.id().value()).folderId(h.folderId().value()).date(w.date())
                .description(w.description()).minutes(w.minutes()).hourlyRate(amount(w.hourlyRate()))
                .expenses(map(w.expenses(), e -> new ExpenseDto(amount(e.amount())).description(e.description())))
                .amount(amount(h.amount())).invoiceId(id(h.invoiceId()));
    }

    // ----- advances --------------------------------------------------------------------------------------------------

    static AdvanceDto toDto(Advance a) {
        return new AdvanceDto(a.id().value()).folderId(a.folderId().value()).requestId(id(a.requestId()))
                .customerId(id(a.customerId())).date(a.date()).amount(amount(a.amount()))
                .ticketId(id(a.means().ticketId())).iban(a.means().iban()).cash(a.means().cash())
                .invoiceId(id(a.invoiceId()));
    }

    static AdvanceRequestTerms toTerms(AdvanceRequestTermsInputDto in) {
        return new AdvanceRequestTerms(in.getDate(), in.getMeetingDate(), Json.intOrZero(in.getDelayDays()),
                Json.money(in.getAmount()), Json.distribution(in.getShares()), in.getComment());
    }

    static AdvanceRequestDto toDto(AdvanceRequestSituation s) {
        AdvanceRequest r = s.request();
        AdvanceRequestTerms t = r.terms();
        return new AdvanceRequestDto(r.id().value()).folderId(r.folderId().value()).date(t.date())
                .meetingDate(t.meetingDate()).delayDays(t.delayDays()).dueDate(r.dueDate())
                .amount(amount(t.amount())).honorariesSum(amount(r.honorariesSum()))
                .advancedSum(amount(r.advancedSum())).paidSum(amount(s.paidSum()))
                .remainingSum(amount(s.remainingSum())).paid(r.isPaid()).paidOn(r.paidOn())
                .invoiced(s.isInvoiced()).comment(t.comment())
                .shares(t.distribution().shares().keySet().stream()
                        .map(c -> new CustomerShareDto().customerId(c.value()).amount(amount(r.shareOf(c)))
                                .paid(amount(r.paidSumBy(c, s.advances())))
                                .remaining(amount(r.remainingSumFor(c, s.advances()))))
                        .toList())
                .advances(map(s.advances(), InvoicingMapper::toDto))
                .recalls(map(r.recalls(), Json::recall));
    }

    // ----- invoices --------------------------------------------------------------------------------------------------

    static InvoiceTerms toTerms(InvoiceTermsInputDto in) {
        return new InvoiceTerms(in.getDate(), Json.intOrZero(in.getDelayDays()), Json.rate(in.getDiscountRatePercent()),
                Json.money(in.getDiscount()), Json.rate(in.getTaxRatePercent()), Json.distribution(in.getShares()),
                in.getComment());
    }

    static InvoiceDto toDto(Invoice i) {
        InvoiceTerms t = i.terms();
        return new InvoiceDto(i.id().value()).folderId(i.folderId().value()).date(t.date())
                .delayDays(t.delayDays()).dueDate(i.dueDate())
                .honorariesSum(amount(i.honorariesSum())).advancedSum(amount(i.advancedSum()))
                .discountRatePercent(percent(t.discountRate())).discount(amount(t.discount()))
                .taxRatePercent(percent(t.taxRate())).tax(amount(i.tax())).amount(amount(i.amount()))
                .paidSum(amount(i.paidSum())).remainingSum(amount(i.remainingSum()))
                .paid(i.isPaid()).paidOn(i.paidOn()).comment(t.comment())
                .shares(t.distribution().shares().keySet().stream()
                        .map(c -> new CustomerShareDto().customerId(c.value()).amount(amount(i.shareOf(c)))
                                .paid(amount(i.paidSumBy(c))).remaining(amount(i.remainingSumFor(c))))
                        .toList())
                .payments(map(i.payments(), InvoicingMapper::toDto))
                .recalls(map(i.recalls(), Json::recall));
    }

    static InvoicePaymentDto toDto(InvoicePayment p) {
        return new InvoicePaymentDto().id(p.id().value()).customerId(id(p.customerId())).date(p.date())
                .amount(amount(p.amount())).ticketId(id(p.means().ticketId())).iban(p.means().iban())
                .cash(p.means().cash());
    }
}
