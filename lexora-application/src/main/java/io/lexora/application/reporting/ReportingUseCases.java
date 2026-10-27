package io.lexora.application.reporting;

import io.lexora.domain.advance.Advance;
import io.lexora.domain.advance.AdvanceRepository;
import io.lexora.domain.booking.OutgoingPayment;
import io.lexora.domain.booking.OutgoingPaymentRepository;
import io.lexora.domain.folder.Folder;
import io.lexora.domain.folder.FolderId;
import io.lexora.domain.folder.FolderRepository;
import io.lexora.domain.folder.FolderType;
import io.lexora.domain.honorary.Honorary;
import io.lexora.domain.honorary.HonoraryRepository;
import io.lexora.domain.invoice.InvoicePaymentRecord;
import io.lexora.domain.invoice.InvoiceRepository;
import io.lexora.domain.settings.SettingsRepository;
import io.lexora.domain.shared.Money;
import io.lexora.domain.shared.Period;
import io.lexora.application.common.Ids;
import io.lexora.application.document.PrintedDocument;
import io.lexora.domain.ticket.Ticket;
import io.lexora.domain.ticket.TicketId;
import io.lexora.domain.ticket.TicketRepository;
import io.lexora.domain.user.User;
import io.lexora.domain.user.UserRepository;
import io.lexora.domain.user.Username;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Financial reports of the booking module. */
@Transactional
public class ReportingUseCases {

    private final InvoiceRepository invoices;
    private final AdvanceRepository advances;
    private final HonoraryRepository honoraries;
    private final OutgoingPaymentRepository outgoingPayments;
    private final FolderRepository folders;
    private final SettingsRepository settings;
    private final UserRepository users;
    private final TicketRepository tickets;
    private final ReportRenderer renderer;

    public ReportingUseCases(InvoiceRepository invoices, AdvanceRepository advances, HonoraryRepository honoraries,
                             OutgoingPaymentRepository outgoingPayments, FolderRepository folders,
                             SettingsRepository settings, UserRepository users, TicketRepository tickets,
                             ReportRenderer renderer) {
        this.invoices = invoices;
        this.advances = advances;
        this.honoraries = honoraries;
        this.outgoingPayments = outgoingPayments;
        this.folders = folders;
        this.settings = settings;
        this.users = users;
        this.tickets = tickets;
        this.renderer = renderer;
    }

    /**
     * Statements of a period as PDF (legacy "relevés"): incomes with the VAT owed at the net tax rate aside, then expenses.
     * Generated on demand, never stored.
     */
    public PrintedDocument printPeriodStatement(Period period) {
        byte[] pdf = renderer.render(incomeReport(period), expenseReport(period));
        return new PrintedDocument("releves_" + period.from() + "_" + period.to() + ".pdf", pdf);
    }

    /** Invoice payments and advances (of invoicing folders) received in the period, with the VAT owed. */
    public IncomeReport incomeReport(Period period) {
        List<InvoicePaymentRecord> payments = invoices.findPaymentsBetween(period);
        List<Advance> received = advances.findReceivedBetween(period, FolderType.INVOICING);

        Set<FolderId> folderIds = new HashSet<>();
        payments.forEach(p -> folderIds.add(p.folderId()));
        received.forEach(a -> folderIds.add(a.folderId()));
        Map<FolderId, Folder> byId = folders.findAllById(folderIds);
        Function<FolderId, String> nameOf = id -> byId.containsKey(id) ? byId.get(id).name() : null;

        Map<TicketId, String> ticketNumbers = new HashMap<>();
        Function<TicketId, String> numberOf = id -> id == null ? null : ticketNumbers.computeIfAbsent(id,
                key -> tickets.findById(key).map(Ticket::number).orElse(null));
        List<IncomeReport.IncomeLine> paymentLines = payments.stream()
                .map(p -> new IncomeReport.IncomeLine(p.payment().id().value(), p.payment().date(), p.folderId(),
                        nameOf.apply(p.folderId()), p.invoiceId().value(), p.payment().amount(),
                        p.payment().means().cash(), numberOf.apply(p.payment().means().ticketId()),
                        p.payment().means().iban()))
                .toList();
        List<IncomeReport.IncomeLine> advanceLines = received.stream()
                .map(a -> new IncomeReport.IncomeLine(a.id().value(), a.date(), a.folderId(), nameOf.apply(a.folderId()),
                        a.requestId() == null ? null : a.requestId().value(), a.amount(), a.means().cash(),
                        numberOf.apply(a.means().ticketId()), a.means().iban()))
                .toList();
        return new IncomeReport(period, paymentLines, advanceLines, settings.booking().vatRate());
    }

    public ExpenseReport expenseReport(Period period) {
        return new ExpenseReport(period, outgoingPayments.findOfficePaidBetween(period));
    }

    public MonthlyOverview monthlyOverview() {
        Map<Integer, Map<Month, Money[]>> years = new TreeMap<>(Comparator.reverseOrder());
        for (Advance advance : advances.findAll()) {
            add(years, advance.date(), 0, advance.amount());
        }
        for (InvoicePaymentRecord payment : invoices.findAllPayments()) {
            add(years, payment.payment().date(), 1, payment.payment().amount());
        }
        for (OutgoingPayment payment : outgoingPayments.findOfficePaid()) {
            add(years, payment.details().paidOn(), 2, payment.amount());
        }
        List<MonthlyOverview.Year> result = new ArrayList<>();
        years.forEach((year, months) -> {
            List<MonthlyOverview.MonthFigures> figures = new ArrayList<>();
            for (Month month : Month.values()) {
                Money[] values = months.getOrDefault(month, zeros());
                figures.add(new MonthlyOverview.MonthFigures(month, values[0], values[1], values[2]));
            }
            result.add(new MonthlyOverview.Year(year, figures));
        });
        return new MonthlyOverview(result);
    }

    /**
     * Honoraries and incomes of the folders a user handles: those whose handler ({@code fold_handled_by}) is the
     * user's shortcut, or his user name.
     */
    public UserStatistics userStatistics(String username, Period period) {
        User user = Ids.require(users.findByUsername(Username.of(username)), "User", username);
        Set<String> handlers = new LinkedHashSet<>();
        handlers.add(user.username().value());
        if (user.shortcut() != null && !user.shortcut().isBlank()) {
            handlers.add(user.shortcut().trim());
        }
        List<Folder> userFolders = folders.findHandledBy(handlers);
        Set<FolderId> ids = userFolders.stream().map(Folder::id).collect(Collectors.toSet());
        Money honorariesTotal = honoraries.findBetween(period).stream()
                .filter(h -> ids.contains(h.folderId()))
                .collect(Money.summing(Honorary::amount));
        Money advancesTotal = advances.findReceivedBetween(period, FolderType.INVOICING).stream()
                .filter(a -> ids.contains(a.folderId()))
                .collect(Money.summing(Advance::amount));
        Money paymentsTotal = invoices.findPaymentsBetween(period).stream()
                .filter(p -> ids.contains(p.folderId()))
                .collect(Money.summing(p -> p.payment().amount()));
        return new UserStatistics(username, period, userFolders, honorariesTotal, advancesTotal.plus(paymentsTotal));
    }

    private static void add(Map<Integer, Map<Month, Money[]>> years, LocalDate date, int column, Money amount) {
        if (date == null) {
            return;
        }
        Money[] values = years.computeIfAbsent(date.getYear(), y -> new EnumMap<>(Month.class))
                .computeIfAbsent(date.getMonth(), m -> zeros());
        values[column] = values[column].plus(amount);
    }

    private static Money[] zeros() {
        return new Money[]{Money.ZERO, Money.ZERO, Money.ZERO};
    }
}
