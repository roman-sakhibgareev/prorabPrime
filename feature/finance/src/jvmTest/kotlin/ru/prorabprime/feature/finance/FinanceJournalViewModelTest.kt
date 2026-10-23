package ru.prorabprime.feature.finance

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import ru.prorabprime.domain.model.AppError
import ru.prorabprime.domain.model.JournalCash
import ru.prorabprime.domain.model.JournalEntry
import ru.prorabprime.domain.model.JournalFilter
import ru.prorabprime.domain.model.JournalPeriod
import ru.prorabprime.domain.model.LocalDay
import ru.prorabprime.domain.model.Money
import ru.prorabprime.domain.model.ObjectId
import ru.prorabprime.domain.model.Payment
import ru.prorabprime.domain.model.PaymentId
import ru.prorabprime.domain.model.PaymentJournal
import ru.prorabprime.domain.model.PaymentMethod
import ru.prorabprime.domain.model.PaymentSide
import ru.prorabprime.domain.usecase.DeletePaymentUseCase
import ru.prorabprime.domain.usecase.ObservePaymentJournalUseCase
import ru.prorabprime.domain.usecase.SavePaymentUseCase
import ru.prorabprime.testing.FakeFinanceRepository
import ru.prorabprime.testing.FakePaymentJournalRepository
import ru.prorabprime.testing.FakeSnackbarNotifier
import ru.prorabprime.testing.MainDispatcherRule
import ru.prorabprime.ui.toUiText

class FinanceJournalViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val journal = FakePaymentJournalRepository()
    private val finance = FakeFinanceRepository()
    private val notifier = FakeSnackbarNotifier()
    private val today = LocalDay.of(2026, 10, 20)

    private val viewModel by lazy {
        val holder = FinanceJournalStateHolder()
        FinanceJournalViewModel(
            stateHolder = holder,
            errorHandler = FinanceJournalErrorHandler(holder, notifier),
            actions = FinanceJournalActions(
                ObservePaymentJournalUseCase(journal),
                SavePaymentUseCase(finance),
                DeletePaymentUseCase(finance),
            ),
            today = { today },
        )
    }

    private val state get() = viewModel.state.value

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
        if (objectId == kitchen) "Кухня" else "Ванная",
    )

    private fun withPayments() {
        journal.result.value = Result.success(
            PaymentJournal(
                listOf(
                    entry("1", kitchen, PaymentSide.CLIENT, 100_000, LocalDay.of(2026, 10, 18)),
                    entry("2", bath, PaymentSide.CREW, 40_000, LocalDay.of(2026, 10, 3), PaymentMethod.TRANSFER),
                    entry("3", kitchen, PaymentSide.CREW, 25_000, LocalDay.of(2026, 9, 30), PaymentMethod.CARD),
                ),
            ),
        )
    }

    @Test
    fun `nothing is shown but the spinner until the journal arrives`() {
        assertThat(state.status).isEqualTo(FinanceJournalStatus.Loading)
    }

    @Test
    fun `the rows come signed, with the totals of everything`() {
        withPayments()

        assertThat(state.status).isEqualTo(FinanceJournalStatus.Content)
        assertThat(state.rows.map { it.id }).containsExactly("1", "2", "3").inOrder()
        assertThat(state.rows[0].amount).isEqualTo("+ " + Money.format(100_000))
        assertThat(state.rows[1].amount).isEqualTo("− " + Money.format(40_000))
        assertThat(state.rows[0].date).isEqualTo("18.10.2026")
        assertThat(state.totals.income).isEqualTo(Money.format(100_000))
        assertThat(state.totals.expense).isEqualTo(Money.format(65_000))
        assertThat(state.totals.balance).isEqualTo(Money.format(35_000))
        assertThat(state.totals.isDeficit).isFalse()
        assertThat(state.allCount).isEqualTo(3)
        assertThat(state.objects.map { it.title }).containsExactly("Ванная", "Кухня").inOrder()
    }

    @Test
    fun `a month narrows the rows and the totals, and the count of all stays`() {
        withPayments()

        viewModel.onEvent(FinanceJournalEvent.PeriodChanged(JournalPeriod.Month(2026, 10)))

        assertThat(state.rows.map { it.id }).containsExactly("1", "2").inOrder()
        assertThat(state.totals.expense).isEqualTo(Money.format(40_000))
        assertThat(state.allCount).isEqualTo(3)
    }

    @Test
    fun `object, side and cash filters combine, and a reset clears them`() {
        withPayments()

        viewModel.onEvent(FinanceJournalEvent.ObjectChosen(kitchen))
        assertThat(state.rows.map { it.id }).containsExactly("1", "3").inOrder()

        viewModel.onEvent(FinanceJournalEvent.SideChosen(PaymentSide.CREW))
        assertThat(state.rows.map { it.id }).containsExactly("3")
        assertThat(state.totals.isDeficit).isTrue()

        viewModel.onEvent(FinanceJournalEvent.CashChosen(JournalCash.CASH))
        assertThat(state.rows).isEmpty()
        assertThat(state.allCount).isEqualTo(3)

        viewModel.onEvent(FinanceJournalEvent.FiltersReset)
        assertThat(state.filter).isEqualTo(JournalFilter())
        assertThat(state.rows).hasSize(3)
    }

    @Test
    fun `a change in the journal is shown under the filter that is set`() {
        withPayments()
        viewModel.onEvent(FinanceJournalEvent.SideChosen(PaymentSide.CLIENT))

        journal.result.value = Result.success(
            PaymentJournal(listOf(entry("9", bath, PaymentSide.CLIENT, 7_000, today))),
        )

        assertThat(state.rows.map { it.id }).containsExactly("9")
    }

    @Test
    fun `tapping a row opens the payment form filled in, and saving updates that payment`() {
        withPayments()

        viewModel.onEvent(FinanceJournalEvent.RowClicked("2"))
        val form = state.editor as FinanceEditorUi.Payment
        assertThat(form.paymentId).isEqualTo("2")
        assertThat(form.side).isEqualTo(PaymentSide.CREW)
        assertThat(form.amountText).isEqualTo("400")

        viewModel.onEvent(FinanceJournalEvent.Form(PaymentEvent.AmountChanged("450")))
        viewModel.onEvent(FinanceJournalEvent.Form(PaymentEvent.Save))

        val (id, draft) = finance.updatedPayments.single()
        assertThat(id).isEqualTo(PaymentId("2"))
        assertThat(draft.amountKopecks).isEqualTo(45_000L)
        assertThat(state.editor).isNull()
        assertThat(notifier.successes).isNotEmpty()
    }

    @Test
    fun `an invalid form stays open with the problem marked, and nothing is written`() {
        withPayments()
        viewModel.onEvent(FinanceJournalEvent.RowClicked("1"))

        viewModel.onEvent(FinanceJournalEvent.Form(PaymentEvent.AmountChanged("")))
        viewModel.onEvent(FinanceJournalEvent.Form(PaymentEvent.Save))

        assertThat(finance.updatedPayments).isEmpty()
        assertThat((state.editor as FinanceEditorUi.Payment).errors).isNotEmpty()
    }

    @Test
    fun `deleting asks first and only then deletes`() {
        withPayments()
        viewModel.onEvent(FinanceJournalEvent.RowClicked("3"))

        viewModel.onEvent(FinanceJournalEvent.Form(PaymentEvent.Delete("3")))
        assertThat(state.editor).isNull()
        assertThat(state.dialog).isNotNull()
        assertThat(finance.deletedPayments).isEmpty()

        viewModel.onEvent(FinanceJournalEvent.DialogConfirmed)

        assertThat(finance.deletedPayments).containsExactly(PaymentId("3"))
        assertThat(state.dialog).isNull()
    }

    @Test
    fun `a dismissed delete deletes nothing`() {
        withPayments()
        viewModel.onEvent(FinanceJournalEvent.RowClicked("3"))
        viewModel.onEvent(FinanceJournalEvent.Form(PaymentEvent.Delete("3")))

        viewModel.onEvent(FinanceJournalEvent.DialogDismissed)

        assertThat(finance.deletedPayments).isEmpty()
        assertThat(state.dialog).isNull()
    }

    @Test
    fun `the form cannot add a payment here, there is no object to put it on`() {
        withPayments()

        viewModel.onEvent(FinanceJournalEvent.Form(PaymentEvent.Add(PaymentSide.CLIENT)))

        assertThat(state.editor).isNull()
    }

    @Test
    fun `a failed write is told in a message and the form stays`() {
        withPayments()
        finance.writeError = AppError.Network
        viewModel.onEvent(FinanceJournalEvent.RowClicked("1"))

        viewModel.onEvent(FinanceJournalEvent.Form(PaymentEvent.Save))

        assertThat(notifier.errors).containsExactly(AppError.Network.toUiText())
        assertThat(state.editor).isNotNull()
    }

    @Test
    fun `a failed first load takes over the screen and retry loads again`() {
        journal.result.value = Result.failure(ru.prorabprime.domain.model.AppErrorException(AppError.Network))
        assertThat(state.status).isEqualTo(FinanceJournalStatus.Error(AppError.Network.toUiText()))

        withPayments()
        viewModel.onEvent(FinanceJournalEvent.Retry)

        assertThat(state.status).isEqualTo(FinanceJournalStatus.Content)
        assertThat(state.rows).hasSize(3)
    }

    @Test
    fun `an empty journal is content with a count of zero`() {
        journal.result.value = Result.success(PaymentJournal(emptyList()))

        assertThat(state.status).isEqualTo(FinanceJournalStatus.Content)
        assertThat(state.allCount).isEqualTo(0)
    }
}
