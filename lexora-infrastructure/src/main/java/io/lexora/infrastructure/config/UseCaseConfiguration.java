package io.lexora.infrastructure.config;

import io.lexora.application.advance.AdvanceRequestUseCases;
import io.lexora.application.advance.AdvanceUseCases;
import io.lexora.application.bank.BankImportUseCases;
import io.lexora.application.bank.BankStatementReader;
import io.lexora.application.bank.ImportedBankEntries;
import io.lexora.application.booking.BookingUseCases;
import io.lexora.application.customer.CustomerUseCases;
import io.lexora.application.debtor.DebtorUseCases;
import io.lexora.application.document.DocumentRenderer;
import io.lexora.application.document.DocumentUseCases;
import io.lexora.application.folder.FolderUseCases;
import io.lexora.application.honorary.HonoraryUseCases;
import io.lexora.application.invoice.InvoiceUseCases;
import io.lexora.application.reporting.ReportRenderer;
import io.lexora.application.reporting.ReportingUseCases;
import io.lexora.application.reporting.VatUseCases;
import io.lexora.application.settings.SettingsUseCases;
import io.lexora.application.template.TemplateUseCases;
import io.lexora.application.ticket.TicketUseCases;
import io.lexora.application.user.PasswordHasher;
import io.lexora.application.user.UserUseCases;
import io.lexora.domain.advance.AdvanceRepository;
import io.lexora.domain.advance.AdvanceRequestRepository;
import io.lexora.domain.booking.OutgoingPaymentRepository;
import io.lexora.domain.booking.StandingOrderRepository;
import io.lexora.domain.booking.VatPaymentRepository;
import io.lexora.domain.customer.CustomerRepository;
import io.lexora.domain.folder.FolderRepository;
import io.lexora.domain.folder.FolderStatusRepository;
import io.lexora.domain.honorary.HonoraryRepository;
import io.lexora.domain.invoice.InvoiceRepository;
import io.lexora.domain.settings.SettingsRepository;
import io.lexora.domain.template.DocumentModelRepository;
import io.lexora.domain.template.WordingRepository;
import io.lexora.domain.ticket.TicketRepository;
import io.lexora.domain.user.GrantRepository;
import io.lexora.domain.user.UserRepository;
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
    Clock clock(@Value("${lexora.time-zone:Europe/Zurich}") String timeZone) {
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
