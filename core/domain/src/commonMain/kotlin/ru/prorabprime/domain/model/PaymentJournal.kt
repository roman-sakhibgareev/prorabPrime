package ru.prorabprime.domain.model

/** One payment of one object, as the combined journal lists it. */
data class JournalEntry(
    val payment: Payment,
    val objectId: ObjectId,
    val objectTitle: String,
) {
    /** Money in is what the client paid; money out is what the crew was paid. */
    val isIncome: Boolean get() = payment.side == PaymentSide.CLIENT
}

/** Every payment of every object, newest first. */
data class PaymentJournal(
    val entries: List<JournalEntry>,
)

val PaymentMethod.isCash: Boolean get() = this == PaymentMethod.CASH

enum class JournalCash {
    CASH,
    NON_CASH,
}

/** The days the journal covers: all of them, or one calendar month. */
sealed interface JournalPeriod {
    operator fun contains(day: LocalDay): Boolean

    data object All : JournalPeriod {
        override fun contains(day: LocalDay) = true
    }

    /** [month] is 1 to 12. */
    data class Month(
        val year: Int,
        val month: Int,
    ) : JournalPeriod {
        override fun contains(day: LocalDay) = day.year == year && day.month == month

        fun previous(): Month = if (month == 1) Month(year - 1, DECEMBER) else Month(year, month - 1)

        fun next(): Month = if (month == DECEMBER) Month(year + 1, 1) else Month(year, month + 1)
    }

    companion object {
        private const val DECEMBER = 12

        fun monthOf(day: LocalDay): Month = Month(day.year, day.month)
    }
}

/** What the journal is narrowed to; a null part lets everything through. */
data class JournalFilter(
    val period: JournalPeriod = JournalPeriod.All,
    val objectId: ObjectId? = null,
    /** [PaymentSide.CLIENT] for income, [PaymentSide.CREW] for expenses. */
    val side: PaymentSide? = null,
    val cash: JournalCash? = null,
) {
    fun matches(entry: JournalEntry): Boolean = entry.payment.paidOn in period &&
        (objectId == null || entry.objectId == objectId) &&
        (side == null || entry.payment.side == side) &&
        when (cash) {
            null -> true
            JournalCash.CASH -> entry.payment.method.isCash
            JournalCash.NON_CASH -> !entry.payment.method.isCash
        }
}

data class JournalTotals(
    val incomeKopecks: Long,
    val expenseKopecks: Long,
) {
    val balanceKopecks: Long get() = incomeKopecks - expenseKopecks
}

fun PaymentJournal.filtered(filter: JournalFilter): List<JournalEntry> = entries.filter(filter::matches)

fun List<JournalEntry>.totals(): JournalTotals = JournalTotals(
    incomeKopecks = filter { it.isIncome }.sumOf { it.payment.amountKopecks },
    expenseKopecks = filterNot { it.isIncome }.sumOf { it.payment.amountKopecks },
)
