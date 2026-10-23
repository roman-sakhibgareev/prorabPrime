package ru.prorabprime.feature.expenses

import kotlinx.collections.immutable.toImmutableList
import ru.prorabprime.domain.model.ExpenseItem
import ru.prorabprime.domain.model.ExpenseKind
import ru.prorabprime.domain.model.ExpenseStats
import ru.prorabprime.domain.model.Expenses
import ru.prorabprime.domain.model.LocalDay
import ru.prorabprime.domain.model.Money
import ru.prorabprime.domain.model.stats

/** What the screen shows for [expenses] as of [today], with the list narrowed to [filter]. */
internal fun Expenses.toState(
    today: LocalDay,
    filter: ExpenseFilter,
    paymentFilter: PaymentStatusFilter = PaymentStatusFilter.ALL,
): ExpensesState {
    val stats = stats(today)
    val receipts = items.filter { it.kind == ExpenseKind.RECEIPT }
    return ExpensesState(
        status = ExpensesStatus.Content,
        filter = filter,
        paymentFilter = paymentFilter,
        paidCount = receipts.count { it.isPaid },
        unpaidCount = receipts.count { !it.isPaid },
        summary = stats.toSummary(),
        byObject = stats.objectBars().toImmutableList(),
        byMonth = stats.monthBars().toImmutableList(),
        rows = items.filter { filter.admits(it) && paymentFilter.admits(it) }.map { it.toRow() }.toImmutableList(),
        allRows = items.size,
    )
}

private fun ExpenseStats.toSummary() = SummaryUi(
    total = Money.format(totalKopecks),
    thisMonth = Money.format(thisMonthKopecks),
    materials = Money.format(receiptsKopecks),
    crew = Money.format(crewKopecks),
    paid = Money.format(totalKopecks - unpaidKopecks),
    unpaid = Money.format(unpaidKopecks),
    withoutAmount = withoutAmount,
)

private fun ExpenseStats.objectBars(): List<BarUi> {
    val largest = byObject.maxOfOrNull { it.kopecks } ?: 0L
    return byObject.map {
        BarUi(
            key = it.objectId.value,
            label = it.title,
            value = Money.format(it.kopecks),
            fraction = share(it.kopecks, largest),
            detail = it.count.toString(),
            objectId = it.objectId.value,
        )
    }
}

private fun ExpenseStats.monthBars(): List<BarUi> {
    val largest = byMonth.maxOfOrNull { it.kopecks } ?: 0L
    return byMonth.map {
        BarUi(it.month, monthLabel(it.month), Money.format(it.kopecks), share(it.kopecks, largest))
    }
}

private fun share(value: Long, largest: Long): Float = if (largest <= 0L) 0f else value.toFloat() / largest.toFloat()

private fun ExpenseFilter.admits(item: ExpenseItem): Boolean = when (this) {
    ExpenseFilter.ALL -> true
    ExpenseFilter.MATERIALS -> item.kind == ExpenseKind.RECEIPT
    ExpenseFilter.CREW -> item.kind == ExpenseKind.CREW
}

/** A payment to the crew is paid by being made, so only receipts pass a narrowing status. */
private fun PaymentStatusFilter.admits(item: ExpenseItem): Boolean = when (this) {
    PaymentStatusFilter.ALL -> true
    PaymentStatusFilter.UNPAID -> item.kind == ExpenseKind.RECEIPT && !item.isPaid
    PaymentStatusFilter.PAID -> item.kind == ExpenseKind.RECEIPT && item.isPaid
}

private fun ExpenseItem.toRow() = ExpenseRowUi(
    id = id,
    objectId = objectId.value,
    objectTitle = objectTitle,
    day = day.format(),
    isReceipt = kind == ExpenseKind.RECEIPT,
    amount = amountKopecks?.let(Money::format),
    note = note,
    photoId = photoId?.value,
    isUnpaid = !isPaid,
)
