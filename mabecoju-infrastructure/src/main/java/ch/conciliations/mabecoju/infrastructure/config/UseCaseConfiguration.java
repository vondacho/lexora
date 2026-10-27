package ch.conciliations.mabecoju.infrastructure.config;

import ch.conciliations.mabecoju.application.advance.AdvanceRequestUseCases;
import ch.conciliations.mabecoju.application.advance.AdvanceUseCases;
import ch.conciliations.mabecoju.application.bank.BankImportUseCases;
import ch.conciliations.mabecoju.application.bank.BankStatementReader;
import ch.conciliations.mabecoju.application.bank.ImportedBankEntries;
import ch.conciliations.mabecoju.application.booking.BookingUseCases;
import ch.conciliations.mabecoju.application.customer.CustomerUseCases;
import ch.conciliations.mabecoju.application.debtor.DebtorUseCases;
import ch.conciliations.mabecoju.application.document.DocumentRenderer;
import ch.conciliations.mabecoju.application.document.DocumentUseCases;
import ch.conciliations.mabecoju.application.folder.FolderUseCases;
import ch.conciliations.mabecoju.application.honorary.HonoraryUseCases;
import ch.conciliations.mabecoju.application.invoice.InvoiceUseCases;
import ch.conciliations.mabecoju.application.reporting.ReportRenderer;
import ch.conciliations.mabecoju.application.reporting.ReportingUseCases;
import ch.conciliations.mabecoju.application.reporting.VatUseCases;
import ch.conciliations.mabecoju.application.settings.SettingsUseCases;
import ch.conciliations.mabecoju.application.template.TemplateUseCases;
import ch.conciliations.mabecoju.application.ticket.TicketUseCases;
import ch.conciliations.mabecoju.application.user.PasswordHasher;
import ch.conciliations.mabecoju.application.user.UserUseCases;
import ch.conciliations.mabecoju.domain.advance.AdvanceRepository;
import ch.conciliations.mabecoju.domain.advance.AdvanceRequestRepository;
import ch.conciliations.mabecoju.domain.booking.OutgoingPaymentRepository;
import ch.conciliations.mabecoju.domain.booking.StandingOrderRepository;
import ch.conciliations.mabecoju.domain.booking.VatPaymentRepository;
import ch.conciliations.mabecoju.domain.customer.CustomerRepository;
import ch.conciliations.mabecoju.domain.folder.FolderRepository;
import ch.conciliations.mabecoju.domain.folder.FolderStatusRepository;
import ch.conciliations.mabecoju.domain.honorary.HonoraryRepository;
import ch.conciliations.mabecoju.domain.invoice.InvoiceRepository;
import ch.conciliations.mabecoju.domain.settings.SettingsRepository;
import ch.conciliations.mabecoju.domain.template.DocumentModelRepository;
import ch.conciliations.mabecoju.domain.template.WordingRepository;
import ch.conciliations.mabecoju.domain.ticket.TicketRepository;
import ch.conciliations.mabecoju.domain.user.GrantRepository;
import ch.conciliations.mabecoju.domain.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Wires the framework-free use cases of the application layer. Their {@code jakarta.transaction.Transactional}
 * annotations are honoured by Spring.
 */
@Configuration(proxyBeanMethods = false)
class UseCaseConfiguration {

    @Bean
    Clock clock(@Value("${mabecoju.time-zone:Europe/Zurich}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }

    @Bean
    CustomerUseCases customerUseCases(CustomerRepository customers, FolderRepository folders, InvoiceRepository invoices,
                                      AdvanceRequestRepository requests, AdvanceRepository advances,
                                      TicketRepository tickets) {
        return new CustomerUseCases(customers, folders, invoices, requests, advances, tickets);
    }

    @Bean
    HonoraryUseCases honoraryUseCases(HonoraryRepository honoraries, FolderRepository folders) {
        return new HonoraryUseCases(honoraries, folders);
    }

    @Bean
    InvoiceUseCases invoiceUseCases(InvoiceRepository invoices, HonoraryRepository honoraries,
                                    AdvanceRepository advances, CustomerRepository customers,
                                    FolderRepository folders, TicketRepository tickets,
                                    SettingsRepository settings, Clock clock) {
        return new InvoiceUseCases(invoices, honoraries, advances, customers, folders, tickets, settings, clock);
    }

    @Bean
    AdvanceRequestUseCases advanceRequestUseCases(AdvanceRequestRepository requests, AdvanceRepository advances,
                                                  HonoraryRepository honoraries, CustomerRepository customers,
                                                  FolderRepository folders, SettingsRepository settings,
                                                  TicketRepository tickets, Clock clock) {
        return new AdvanceRequestUseCases(requests, advances, honoraries, customers, folders, settings, tickets,
                clock);
    }

    @Bean
    AdvanceUseCases advanceUseCases(AdvanceRepository advances, AdvanceRequestRepository requests,
                                    CustomerRepository customers, FolderRepository folders,
                                    TicketRepository tickets) {
        return new AdvanceUseCases(advances, requests, customers, folders, tickets);
    }

    @Bean
    TicketUseCases ticketUseCases(TicketRepository tickets, FolderRepository folders, CustomerRepository customers,
                                  AdvanceRepository advances, InvoiceRepository invoices,
                                  AdvanceRequestRepository requests) {
        return new TicketUseCases(tickets, folders, customers, advances, invoices, requests);
    }

    @Bean
    DebtorUseCases debtorUseCases(InvoiceRepository invoices, AdvanceRequestRepository requests,
                                  AdvanceRepository advances, HonoraryRepository honoraries,
                                  CustomerRepository customers, FolderRepository folders,
                                  TicketRepository tickets, Clock clock) {
        return new DebtorUseCases(invoices, requests, advances, honoraries, customers, folders, tickets, clock);
    }

    @Bean
    BankImportUseCases bankImportUseCases(BankStatementReader reader, DebtorUseCases debtors, InvoiceRepository invoices,
                                          AdvanceRequestRepository requests, AdvanceRepository advances,
                                          CustomerRepository customers, FolderRepository folders,
                                          TicketRepository tickets, ImportedBankEntries imported) {
        return new BankImportUseCases(reader, debtors, invoices, requests, advances, customers, folders, tickets,
                imported);
    }

    @Bean
    FolderUseCases folderUseCases(FolderRepository folders, FolderStatusRepository statuses,
                                  CustomerRepository customers, HonoraryRepository honoraries,
                                  InvoiceRepository invoices, AdvanceRequestRepository requests,
                                  AdvanceRepository advances, TicketRepository tickets,
                                  OutgoingPaymentRepository outgoingPayments, StandingOrderRepository standingOrders,
                                  InvoiceUseCases invoiceUseCases, AdvanceRequestUseCases requestUseCases) {
        return new FolderUseCases(folders, statuses, customers, honoraries, invoices, requests, advances, tickets,
                outgoingPayments, standingOrders, invoiceUseCases, requestUseCases);
    }

    @Bean
    TemplateUseCases templateUseCases(DocumentModelRepository models, WordingRepository wordings) {
        return new TemplateUseCases(models, wordings);
    }

    @Bean
    SettingsUseCases settingsUseCases(SettingsRepository settings) {
        return new SettingsUseCases(settings);
    }

    @Bean
    BookingUseCases bookingUseCases(OutgoingPaymentRepository payments, StandingOrderRepository orders,
                                    FolderRepository folders, VatPaymentRepository vatPayments, Clock clock) {
        return new BookingUseCases(payments, orders, folders, vatPayments, clock);
    }

    @Bean
    ReportingUseCases reportingUseCases(InvoiceRepository invoices, AdvanceRepository advances,
                                        HonoraryRepository honoraries, OutgoingPaymentRepository payments,
                                        FolderRepository folders, SettingsRepository settings,
                                        UserRepository users, TicketRepository tickets, ReportRenderer renderer) {
        return new ReportingUseCases(invoices, advances, honoraries, payments, folders, settings, users, tickets,
                renderer);
    }

    @Bean
    VatUseCases vatUseCases(VatPaymentRepository vatPayments, OutgoingPaymentRepository expenses,
                            ReportingUseCases reports) {
        return new VatUseCases(vatPayments, expenses, reports);
    }

    @Bean
    DocumentUseCases documentUseCases(InvoiceRepository invoices, AdvanceRequestRepository requests,
                                      AdvanceRepository advances, HonoraryRepository honoraries,
                                      CustomerRepository customers, DocumentModelRepository models,
                                      SettingsRepository settings, UserRepository users, TicketRepository tickets,
                                      DocumentRenderer renderer) {
        return new DocumentUseCases(invoices, requests, advances, honoraries, customers, models, settings, users,
                tickets, renderer);
    }

    @Bean
    UserUseCases userUseCases(UserRepository users, GrantRepository grants, PasswordHasher passwordHasher) {
        return new UserUseCases(users, grants, passwordHasher);
    }
}
