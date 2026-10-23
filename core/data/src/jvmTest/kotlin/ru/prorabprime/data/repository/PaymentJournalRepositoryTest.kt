package ru.prorabprime.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.prorabprime.domain.model.LocalDay
import ru.prorabprime.domain.model.ObjectId
import ru.prorabprime.domain.model.PaymentDraft
import ru.prorabprime.domain.model.PaymentMethod
import ru.prorabprime.domain.model.PaymentSide

class PaymentJournalRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val phone by lazy { OfflineFixture(folder.root).also { runTest { it.db.load() } } }

    private val kitchen get() = ObjectId(phone.server.objectId)

    private suspend fun entries() = phone.journal.observeJournal().first().getOrThrow().entries

    private fun draft(
        side: PaymentSide,
        kopecks: Long,
        day: LocalDay,
        method: PaymentMethod = PaymentMethod.CASH,
    ) = PaymentDraft(side = side, amountKopecks = kopecks, method = method, paidOn = day)

    @Test
    fun `payments come with the object they belong to, newest day first`() = runTest {
        phone.engine.sync()
        phone.server.offline = true
        phone.finance.addPayment(kitchen, draft(PaymentSide.CLIENT, 100, LocalDay.of(2026, 10, 1))).getOrThrow()
        phone.finance.addPayment(kitchen, draft(PaymentSide.CREW, 200, LocalDay.of(2026, 10, 18))).getOrThrow()
        phone.finance.addPayment(kitchen, draft(PaymentSide.CLIENT, 300, LocalDay.of(2026, 10, 18))).getOrThrow()

        val journal = entries()

        assertThat(journal.map { it.payment.amountKopecks }).containsExactly(300L, 200L, 100L).inOrder()
        assertThat(journal.map { it.objectTitle }.distinct()).containsExactly("Кухня")
        assertThat(journal.all { it.objectId == kitchen }).isTrue()
        assertThat(journal.all { it.payment.isPending }).isTrue()
    }

    @Test
    fun `an edit and a delete show in the journal at once`() = runTest {
        phone.engine.sync()
        phone.server.offline = true
        phone.finance.addPayment(kitchen, draft(PaymentSide.CLIENT, 100, LocalDay.of(2026, 10, 1))).getOrThrow()
        val id = entries().single().payment.id

        phone.finance.updatePayment(id, draft(PaymentSide.CREW, 450, LocalDay.of(2026, 10, 2), PaymentMethod.CARD))
            .getOrThrow()
        val edited = entries().single()
        assertThat(edited.payment.amountKopecks).isEqualTo(450L)
        assertThat(edited.payment.side).isEqualTo(PaymentSide.CREW)
        assertThat(edited.payment.method).isEqualTo(PaymentMethod.CARD)

        phone.finance.deletePayment(id).getOrThrow()
        assertThat(entries()).isEmpty()
    }

    @Test
    fun `an empty phone says no payments only once it has asked the server`() = runTest {
        phone.server.objects = emptyList()
        phone.engine.sync()

        assertThat(entries()).isEmpty()
    }
}
