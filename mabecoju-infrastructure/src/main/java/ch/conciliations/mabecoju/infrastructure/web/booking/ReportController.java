package ch.conciliations.mabecoju.infrastructure.web.booking;

import ch.conciliations.mabecoju.application.reporting.ExpenseReport;
import ch.conciliations.mabecoju.application.reporting.IncomeReport;
import ch.conciliations.mabecoju.application.reporting.MonthlyOverview;
import ch.conciliations.mabecoju.application.reporting.ReportingUseCases;
import ch.conciliations.mabecoju.application.reporting.UserStatistics;
import ch.conciliations.mabecoju.domain.shared.Period;
import ch.conciliations.mabecoju.infrastructure.web.Json;
import ch.conciliations.mabecoju.infrastructure.web.PdfResponses;
import ch.conciliations.mabecoju.infrastructure.web.Permissions;
import ch.conciliations.mabecoju.infrastructure.web.api.ReportsApi;
import ch.conciliations.mabecoju.infrastructure.web.api.model.ExpenseLineDto;
import ch.conciliations.mabecoju.infrastructure.web.api.model.ExpenseReportDto;
import ch.conciliations.mabecoju.infrastructure.web.api.model.FolderRefDto;
import ch.conciliations.mabecoju.infrastructure.web.api.model.IncomeLineDto;
import ch.conciliations.mabecoju.infrastructure.web.api.model.IncomeReportDto;
import ch.conciliations.mabecoju.infrastructure.web.api.model.MonthDto;
import ch.conciliations.mabecoju.infrastructure.web.api.model.MonthFiguresDto;
import ch.conciliations.mabecoju.infrastructure.web.api.model.PaymentKindDto;
import ch.conciliations.mabecoju.infrastructure.web.api.model.UserStatisticsDto;
import ch.conciliations.mabecoju.infrastructure.web.api.model.YearOverviewDto;
import org.springframework.core.io.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

import static ch.conciliations.mabecoju.infrastructure.web.Json.amount;
import static ch.conciliations.mabecoju.infrastructure.web.Json.map;

@RestController
class ReportController implements ReportsApi {

    private final ReportingUseCases reports;

    ReportController(ReportingUseCases reports) {
        this.reports = reports;
    }

    @Override
    @PreAuthorize(Permissions.BOOKING_TAX_SUMMARY)
    public IncomeReportDto getIncomeReport(LocalDate from, LocalDate to) {
        IncomeReport r = reports.incomeReport(Period.of(from, to));
        return new IncomeReportDto().from(from).to(to)
                .invoicePayments(map(r.invoicePayments(), ReportController::toDto))
                .invoicePaymentsTotal(amount(r.invoicePaymentsTotal()))
                .advances(map(r.advances(), ReportController::toDto)).advancesTotal(amount(r.advancesTotal()))
                .total(amount(r.total())).vatRatePercent(Json.percent(r.vatRate()))
                .vatDue(amount(r.vatDue()));
    }

    @Override
    @PreAuthorize(Permissions.BOOKING_TAX_SUMMARY)
    public ExpenseReportDto getExpenseReport(LocalDate from, LocalDate to) {
        ExpenseReport r = reports.expenseReport(Period.of(from, to));
        return new ExpenseReportDto().from(from).to(to)
                .payments(map(r.payments(), p -> new ExpenseLineDto().id(p.id().value()).paidOn(p.details().paidOn())
                        .kind(Json.sameName(p.details().kind(), PaymentKindDto.class)).creditor(p.details().creditor())
                        .title(p.details().title()).amount(amount(p.amount())).tax(amount(p.tax()))))
                .total(amount(r.total())).taxTotal(amount(r.taxTotal()));
    }

    @Override
    @PreAuthorize(Permissions.BOOKING_TAX_SUMMARY)
    public Resource printPeriodStatement(LocalDate from, LocalDate to) {
        return PdfResponses.inline(reports.printPeriodStatement(Period.of(from, to)));
    }

    @Override
    @PreAuthorize(Permissions.BOOKING_OVERVIEW)
    public List<YearOverviewDto> getMonthlyOverview() {
        return map(reports.monthlyOverview().years(), y -> new YearOverviewDto().year(y.year())
                .months(map(y.months(), ReportController::toDto)).incomes(amount(y.incomes()))
                .expenses(amount(y.expenses())));
    }

    @Override
    @PreAuthorize(Permissions.BOOKING_STATISTICS)
    public UserStatisticsDto getUserStatistics(String username, LocalDate from, LocalDate to) {
        UserStatistics s = reports.userStatistics(username, Period.of(from, to));
        return new UserStatisticsDto().username(s.username()).from(from).to(to)
                .folders(map(s.folders(), f -> new FolderRefDto().id(f.id().value()).name(f.name())))
                .honorariesTotal(amount(s.honorariesTotal())).incomesTotal(amount(s.incomesTotal()));
    }

    private static IncomeLineDto toDto(IncomeReport.IncomeLine l) {
        return new IncomeLineDto().id(l.id()).date(l.date()).folderId(l.folderId().value()).folderName(l.folderName())
                .documentId(l.documentId()).amount(amount(l.amount())).cash(l.cash())
                .ticketNumber(l.ticketNumber()).iban(l.iban());
    }

    private static MonthFiguresDto toDto(MonthlyOverview.MonthFigures m) {
        return new MonthFiguresDto().month(Json.sameName(m.month(), MonthDto.class)).advances(amount(m.advances()))
                .invoicePayments(amount(m.invoicePayments())).incomes(amount(m.incomes()))
                .expenses(amount(m.expenses())).balance(amount(m.balance()));
    }
}
