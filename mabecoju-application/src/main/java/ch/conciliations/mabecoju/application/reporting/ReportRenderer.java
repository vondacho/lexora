package ch.conciliations.mabecoju.application.reporting;

/** Port producing the PDF of the period statements. */
public interface ReportRenderer {

    byte[] render(IncomeReport incomes, ExpenseReport expenses);
}
