package ch.conciliations.mabecoju.infrastructure.persistence.honorary;

import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.honorary.Expense;
import ch.conciliations.mabecoju.domain.honorary.Honorary;
import ch.conciliations.mabecoju.domain.honorary.HonoraryId;
import ch.conciliations.mabecoju.domain.honorary.HonoraryRepository;
import ch.conciliations.mabecoju.domain.honorary.HonoraryWork;
import ch.conciliations.mabecoju.domain.invoice.InvoiceId;
import ch.conciliations.mabecoju.domain.shared.Money;
import ch.conciliations.mabecoju.domain.shared.Period;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static ch.conciliations.mabecoju.infrastructure.persistence.support.Conversions.money;
import static ch.conciliations.mabecoju.infrastructure.persistence.support.Conversions.reference;
import static ch.conciliations.mabecoju.infrastructure.persistence.support.Conversions.toFloat;
import static ch.conciliations.mabecoju.infrastructure.persistence.support.Conversions.toInt;

@Repository
class HonoraryRepositoryAdapter implements HonoraryRepository {

    private final HonoraryJpaRepository jpa;

    HonoraryRepositoryAdapter(HonoraryJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Honorary> findById(HonoraryId id) {
        return jpa.findById(toInt(id.value())).map(HonoraryRepositoryAdapter::toDomain);
    }

    @Override
    public List<Honorary> findByFolder(FolderId folderId) {
        return toDomain(jpa.findByFolderIdOrderByDateAscIdAsc(toInt(folderId.value())));
    }

    @Override
    public List<Honorary> findNotInvoicedByFolder(FolderId folderId) {
        return toDomain(jpa.findByFolderIdAndInvoiceIdIsNullOrderByDateAscIdAsc(toInt(folderId.value())));
    }

    @Override
    public List<Honorary> findByInvoice(InvoiceId invoiceId) {
        return toDomain(jpa.findByInvoiceIdOrderByDateAscIdAsc(toInt(invoiceId.value())));
    }

    @Override
    public List<Honorary> findBetween(Period period) {
        return toDomain(jpa.findByDateBetweenOrderByDateAscIdAsc(period.from(), period.to()));
    }

    @Override
    public Map<FolderId, Money> sumNotInvoicedByFolder() {
        Map<FolderId, Money> sums = new LinkedHashMap<>();
        for (Object[] row : jpa.sumNotInvoicedByFolder()) {
            sums.put(FolderId.of(((Number) row[0]).longValue()), money(((Number) row[1]).doubleValue()));
        }
        return sums;
    }

    @Override
    public Honorary save(Honorary honorary) {
        HonoraryEntity e = honorary.id() == null
                ? new HonoraryEntity()
                : jpa.findById(toInt(honorary.id().value())).orElseThrow();
        HonoraryWork work = honorary.work();
        e.folderId = toInt(honorary.folderId().value());
        e.date = work.date();
        e.description = work.description();
        e.minutes = work.minutes();
        e.tariff = toFloat(work.hourlyRate());
        Expense[] expenses = new Expense[HonoraryWork.MAX_EXPENSES];
        for (int i = 0; i < work.expenses().size(); i++) {
            expenses[i] = work.expenses().get(i);
        }
        e.expense1 = expenseAmount(expenses[0]);
        e.expense2 = expenseAmount(expenses[1]);
        e.expense3 = expenseAmount(expenses[2]);
        e.expense4 = expenseAmount(expenses[3]);
        e.expenseDescription1 = expenseDescription(expenses[0]);
        e.expenseDescription2 = expenseDescription(expenses[1]);
        e.expenseDescription3 = expenseDescription(expenses[2]);
        e.expenseDescription4 = expenseDescription(expenses[3]);
        e.amount = toFloat(honorary.amount());
        e.invoiceId = honorary.invoiceId() == null ? null : toInt(honorary.invoiceId().value());
        return toDomain(jpa.saveAndFlush(e));
    }

    @Override
    public void delete(HonoraryId id) {
        jpa.deleteById(toInt(id.value()));
    }

    private static Float expenseAmount(Expense expense) {
        return expense == null ? 0f : toFloat(expense.amount());
    }

    private static String expenseDescription(Expense expense) {
        return expense == null ? null : expense.description();
    }

    private static List<Honorary> toDomain(List<HonoraryEntity> entities) {
        return entities.stream().map(HonoraryRepositoryAdapter::toDomain).toList();
    }

    static Honorary toDomain(HonoraryEntity e) {
        List<Expense> expenses = new ArrayList<>();
        addExpense(expenses, e.expense1, e.expenseDescription1);
        addExpense(expenses, e.expense2, e.expenseDescription2);
        addExpense(expenses, e.expense3, e.expenseDescription3);
        addExpense(expenses, e.expense4, e.expenseDescription4);
        Long invoiceId = reference(e.invoiceId);
        return Honorary.restore(HonoraryId.of(e.id), FolderId.of(e.folderId),
                new HonoraryWork(e.date, e.description, e.minutes == null ? 0 : e.minutes, money(e.tariff), expenses),
                money(e.amount), invoiceId == null ? null : InvoiceId.of(invoiceId));
    }

    /** Keeps the four legacy expense slots in place as long as one of the following slots is used. */
    private static void addExpense(List<Expense> expenses, Float amount, String description) {
        expenses.add(new Expense(money(amount), description));
        if (expenses.size() == HonoraryWork.MAX_EXPENSES) {
            while (!expenses.isEmpty() && isEmpty(expenses.getLast())) {
                expenses.removeLast();
            }
        }
    }

    private static boolean isEmpty(Expense expense) {
        return expense.amount().isZero() && (expense.description() == null || expense.description().isBlank());
    }
}
