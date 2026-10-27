package ch.conciliations.mabecoju.domain.honorary;

import ch.conciliations.mabecoju.domain.shared.DomainException;
import ch.conciliations.mabecoju.domain.shared.ErrorCode;
import ch.conciliations.mabecoju.domain.shared.Money;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * What was done: time spent at an hourly rate plus up to {@value #MAX_EXPENSES} expenses.
 */
public record HonoraryWork(LocalDate date, String description, int minutes, Money hourlyRate, List<Expense> expenses) {

    public static final int MAX_EXPENSES = 4;

    public HonoraryWork {
        Objects.requireNonNull(date, "date");
        hourlyRate = hourlyRate == null ? Money.ZERO : hourlyRate;
        expenses = expenses == null ? List.of() : List.copyOf(expenses);
        if (minutes < 0) {
            throw new DomainException(ErrorCode.NEGATIVE_VALUE, "Minutes cannot be negative").with("field", "minutes");
        }
        if (expenses.size() > MAX_EXPENSES) {
            throw new DomainException(ErrorCode.TOO_MANY_EXPENSES, "An honorary holds at most " + MAX_EXPENSES + " expenses")
                    .with("max", MAX_EXPENSES);
        }
    }
}
