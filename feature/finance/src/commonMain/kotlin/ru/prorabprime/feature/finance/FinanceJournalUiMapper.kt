package ru.prorabprime.feature.finance

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import ru.prorabprime.domain.model.JournalEntry
import ru.prorabprime.domain.model.JournalTotals
import ru.prorabprime.domain.model.Money

private const val PLUS = "+ "
private const val MINUS = "− "

internal fun JournalEntry.toRowUi() = JournalRowUi(
    payment = payment,
    objectTitle = objectTitle,
    amount = (if (isIncome) PLUS else MINUS) + Money.format(payment.amountKopecks),
    date = payment.paidOn.format(),
    isIncome = isIncome,
)

internal fun JournalTotals.toUi() = JournalTotalsUi(
    income = Money.format(incomeKopecks),
    expense = Money.format(expenseKopecks),
    balance = Money.format(balanceKopecks),
    isDeficit = balanceKopecks < 0,
)

/** The objects that have a payment, by title. */
internal fun List<JournalEntry>.toObjectChoices(): ImmutableList<JournalObjectUi> = distinctBy { it.objectId }
    .map { JournalObjectUi(it.objectId, it.objectTitle) }
    .sortedBy { it.title.lowercase() }
    .toImmutableList()
