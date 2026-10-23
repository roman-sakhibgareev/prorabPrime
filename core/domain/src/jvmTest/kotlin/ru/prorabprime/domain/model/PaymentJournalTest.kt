package ru.prorabprime.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PaymentJournalTest {

    private val kitchen = ObjectId("kitchen")
    private val bath = ObjectId("bath")

    private fun entry(
        id: String,
        objectId: ObjectId,
        side: PaymentSide,
        kopecks: Long,
        day: LocalDay,
        method: PaymentMethod = PaymentMethod.CASH,
    ) = JournalEntry(
        Payment(PaymentId(id), side, kopecks, method, day, note = null),
        objectId,
        objectId.value,
    )

    private val journal = PaymentJournal(
        listOf(
            entry("1", kitchen, PaymentSide.CLIENT, 1_000, LocalDay.of(2026, 10, 18)),
            entry("2", bath, PaymentSide.CREW, 400, LocalDay.of(2026, 10, 3), PaymentMethod.TRANSFER),
            entry("3", kitchen, PaymentSide.CREW, 250, LocalDay.of(2026, 9, 30), PaymentMethod.CARD),
            entry("4", bath, PaymentSide.CLIENT, 700, LocalDay.of(2026, 9, 1), PaymentMethod.OTHER),
        ),
    )

    private fun ids(filter: JournalFilter) = journal.filtered(filter).map { it.payment.id.value }

    @Test
    fun `no filter lets everything through`() {
        assertThat(ids(JournalFilter())).containsExactly("1", "2", "3", "4").inOrder()
    }

    @Test
    fun `a month keeps its own days only, across a year boundary too`() {
        assertThat(ids(JournalFilter(period = JournalPeriod.Month(2026, 10)))).containsExactly("1", "2").inOrder()
        assertThat(JournalPeriod.Month(2026, 1).previous()).isEqualTo(JournalPeriod.Month(2025, 12))
        assertThat(JournalPeriod.Month(2025, 12).next()).isEqualTo(JournalPeriod.Month(2026, 1))
        assertThat(JournalPeriod.monthOf(LocalDay.of(2026, 10, 18))).isEqualTo(JournalPeriod.Month(2026, 10))
    }

    @Test
    fun `object and side narrow the list`() {
        assertThat(ids(JournalFilter(objectId = bath))).containsExactly("2", "4").inOrder()
        assertThat(ids(JournalFilter(side = PaymentSide.CREW))).containsExactly("2", "3").inOrder()
    }

    @Test
    fun `cash is cash and every other method is not`() {
        assertThat(ids(JournalFilter(cash = JournalCash.CASH))).containsExactly("1")
        assertThat(ids(JournalFilter(cash = JournalCash.NON_CASH))).containsExactly("2", "3", "4").inOrder()
    }

    @Test
    fun `filters combine`() {
        val filter = JournalFilter(JournalPeriod.Month(2026, 10), bath, PaymentSide.CREW, JournalCash.NON_CASH)

        assertThat(ids(filter)).containsExactly("2")
    }

    @Test
    fun `totals split income from expenses and give the balance`() {
        val totals = journal.entries.totals()

        assertThat(totals.incomeKopecks).isEqualTo(1_700L)
        assertThat(totals.expenseKopecks).isEqualTo(650L)
        assertThat(totals.balanceKopecks).isEqualTo(1_050L)
    }

    @Test
    fun `totals of nothing are zero, and a deficit is a negative balance`() {
        assertThat(emptyList<JournalEntry>().totals().balanceKopecks).isEqualTo(0L)
        assertThat(journal.filtered(JournalFilter(side = PaymentSide.CREW)).totals().balanceKopecks).isEqualTo(-650L)
    }
}
