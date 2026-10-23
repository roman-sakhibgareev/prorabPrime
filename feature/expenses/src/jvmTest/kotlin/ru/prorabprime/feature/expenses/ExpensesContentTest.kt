package ru.prorabprime.feature.expenses

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Test
import ru.prorabprime.designsystem.theme.ProrabTheme
import ru.prorabprime.domain.model.ExpenseKind
import ru.prorabprime.domain.model.LocalDay
import ru.prorabprime.domain.model.PaymentMethod

@OptIn(ExperimentalTestApi::class)
class ExpensesContentTest {

    private val events = mutableListOf<ExpensesEvent>()
    private val receipts = mutableListOf<Pair<String, String>>()
    private val objects = mutableListOf<String>()
    private var backed = 0

    private val state = ExpensesState(
        status = ExpensesStatus.Content,
        summary = SummaryUi("600 000 ₽", "100 000 ₽", "579 000 ₽", "21 000 ₽", withoutAmount = 1),
        byObject = persistentListOf(BarUi("o1", "Кухня", "400 000 ₽", 1f, "3", "o1")),
        byMonth = persistentListOf(BarUi("2026-10", "Октябрь 2026", "100 000 ₽", 0.5f)),
        rows = persistentListOf(
            ExpenseRowUi(
                "r1", "o1", "Кухня", "03.10.2026", true, "79 000 ₽", "Плитка", "p1",
                isUnpaid = true, isSelectable = true,
            ),
            ExpenseRowUi("r2", "o1", "Кухня", "04.10.2026", true, null, null, "p2", isSelectable = true),
            ExpenseRowUi("c1", "o1", "Кухня", "10.10.2026", false, "21 000 ₽", null, null),
        ),
        allRows = 3,
        paidCount = 1,
        unpaidCount = 4,
    )

    @Composable
    private fun show(state: ExpensesState) {
        ProrabTheme {
            ExpensesContent(
                state = state,
                onEvent = { events += it },
                onOpenReceipt = { objectId, photoId -> receipts += objectId to photoId },
                onOpenObject = { objects += it },
                onBack = { backed++ },
            )
        }
    }

    @Test
    fun `the figures and the sections are shown`() = runComposeUiTest {
        setContent { show(state) }

        onNodeWithText("600 000 ₽").assertIsDisplayed()
        onNodeWithText("579 000 ₽").assertIsDisplayed()
        onNodeWithText("По объектам").assertIsDisplayed()
        onNodeWithText("Чеков без суммы: 1. Откройте чек и впишите сумму, тогда он попадёт в итоги.")
            .assertIsDisplayed()
    }

    @Test
    fun `a receipt without a sum says so`() = runComposeUiTest {
        setContent { show(state) }

        onNode(hasScrollAction()).performScrollToNode(hasText("сумма не указана"))
        onNodeWithText("сумма не указана").assertIsDisplayed()
    }

    @Test
    fun `a receipt opens its photo, a payment opens its object`() = runComposeUiTest {
        setContent { show(state) }

        onNodeWithText("Плитка").performClick()
        onNode(hasScrollAction()).performScrollToNode(hasText("Выплата бригаде", substring = true))
        onNodeWithText("Выплата бригаде", substring = true).performClick()

        assertThat(receipts).containsExactly("o1" to "p1")
        assertThat(objects).containsExactly("o1")
    }

    @Test
    fun `the filter chips send the choice`() = runComposeUiTest {
        setContent { show(state) }

        onNode(hasText("Бригада") and hasClickAction()).performClick()

        assertThat(events).contains(ExpensesEvent.FilterSelected(ExpenseFilter.CREW))
    }

    @Test
    fun `the status chips show the counts and send the choice`() = runComposeUiTest {
        setContent { show(state) }

        onNodeWithText("Оплачены: 1").assertIsDisplayed()
        onNodeWithText("Не оплачены: 4").performClick()
        onNodeWithText("Оплачены: 1").performClick()
        onNodeWithText("Все чеки").performClick()

        assertThat(events).containsExactly(
            ExpensesEvent.PaymentFilterSelected(PaymentStatusFilter.UNPAID),
            ExpensesEvent.PaymentFilterSelected(PaymentStatusFilter.PAID),
            ExpensesEvent.PaymentFilterSelected(PaymentStatusFilter.ALL),
        ).inOrder()
    }

    @Test
    fun `an unpaid receipt is marked paid by its check, without opening the receipt`() = runComposeUiTest {
        setContent { show(state) }

        onNodeWithContentDescription("Отметить чек оплаченным").performClick()

        assertThat(events).containsExactly(ExpensesEvent.ReceiptPaidChanged("p1", paid = true))
        assertThat(receipts).isEmpty()
    }

    @Test
    fun `a paid receipt can be put back from the archive, and a payment has no check`() = runComposeUiTest {
        val archive = state.copy(
            paymentFilter = PaymentStatusFilter.PAID,
            rows = persistentListOf(
                ExpenseRowUi("r2", "o1", "Кухня", "04.10.2026", true, "5 000 ₽", null, "p2"),
                ExpenseRowUi("c1", "o1", "Кухня", "10.10.2026", false, "21 000 ₽", null, null),
            ),
        )
        setContent { show(archive) }

        onNodeWithContentDescription("Вернуть чек в неоплаченные").performClick()

        assertThat(events).containsExactly(ExpensesEvent.ReceiptPaidChanged("p2", paid = false))
        onNodeWithContentDescription("Отметить чек оплаченным").assertDoesNotExist()
    }

    @Test
    fun `a long press on a row asks to start picking`() = runComposeUiTest {
        setContent { show(state) }

        onNodeWithText("Плитка").performTouchInput { longClick() }

        assertThat(events).containsExactly(ExpensesEvent.RowLongPressed("r1"))
        assertThat(receipts).isEmpty()
    }

    @Test
    fun `the top bar has a visible select button, and only when a receipt can be picked`() = runComposeUiTest {
        setContent { show(state) }

        onNodeWithText("Выбрать").performClick()

        assertThat(events).containsExactly(ExpensesEvent.SelectionStarted)
    }

    @Test
    fun `no select button when no receipt has a picture`() = runComposeUiTest {
        setContent { show(state.copy(rows = persistentListOf(state.rows.last()))) }

        onNodeWithText("Выбрать").assertDoesNotExist()
    }

    private val picking = state.copy(selecting = true, selected = persistentSetOf("r1"))

    @Test
    fun `while picking a tap toggles the row instead of opening it, and the bar shows the count`() = runComposeUiTest {
        setContent { show(picking) }

        onNodeWithText("Выбрано: 1").assertIsDisplayed()
        onNodeWithText("Плитка").performClick()

        assertThat(events).containsExactly(ExpensesEvent.RowToggled("r1"))
        assertThat(receipts).isEmpty()
        onNodeWithContentDescription("Добавить расход").assertDoesNotExist()
        onNodeWithContentDescription("Отметить чек оплаченным").assertDoesNotExist()
    }

    @Test
    fun `the bar closes picking, shares and marks the picked receipts`() = runComposeUiTest {
        setContent { show(picking) }

        onNodeWithText("Отправить").performClick()
        onNodeWithContentDescription("Отметить выбранные оплаченными").performClick()
        onNodeWithContentDescription("Отменить выбор").performClick()

        assertThat(events).containsExactly(
            ExpensesEvent.ShareClicked,
            ExpensesEvent.SelectionPaidClicked,
            ExpensesEvent.SelectionClosed,
        ).inOrder()
    }

    @Test
    fun `with nothing picked or a share under way the actions are off`() = runComposeUiTest {
        setContent { show(picking.copy(selected = persistentSetOf())) }

        onNodeWithText("Отправить").assertIsNotEnabled()
        onNodeWithContentDescription("Отметить выбранные оплаченными").assertIsNotEnabled()
    }

    @Test
    fun `an empty status list says so`() = runComposeUiTest {
        setContent { show(state.copy(paymentFilter = PaymentStatusFilter.PAID, rows = persistentListOf())) }

        onNodeWithText("Здесь пока ничего нет.").assertIsDisplayed()
    }

    @Test
    fun `nothing spent says so, and loading and back work`() = runComposeUiTest {
        setContent { show(ExpensesState(status = ExpensesStatus.Content)) }

        onNodeWithText("Расходов пока нет", substring = true).assertIsDisplayed()
        onNodeWithContentDescription("Назад").performClick()
        assertThat(backed).isEqualTo(1)
    }

    @Test
    fun `the plus button asks for the form, also when nothing is spent yet`() = runComposeUiTest {
        setContent { show(ExpensesState(status = ExpensesStatus.Content)) }

        onNodeWithContentDescription("Добавить расход").performClick()

        assertThat(events).containsExactly(ExpensesEvent.AddClicked)
    }

    @Test
    fun `the form for a payment offers the objects and its fields, and sends what is done in it`() = runComposeUiTest {
        val form = NewExpenseUi(day = LocalDay.of(2026, 10, 15), needsObject = true)
        val choices = persistentListOf(ObjectChoiceUi("o1", "Дача"))
        setContent { show(state.copy(form = form, objects = choices)) }

        onNodeWithText("Новый расход").assertIsDisplayed()
        onNodeWithText("Выберите объект").assertIsDisplayed()
        onNodeWithText("15.10.2026").assertIsDisplayed()
        onNodeWithText("Выбрать объект").performClick()
        onNodeWithText("Дача").performClick()
        onNodeWithText("Наличные").assertIsDisplayed()
        onNodeWithText("Перевод").performClick()
        onNodeWithText("Сохранить").performClick()

        assertThat(events).containsExactly(
            ExpenseFormEvent.ObjectChosen("o1"),
            ExpenseFormEvent.MethodChanged(PaymentMethod.TRANSFER),
            ExpenseFormEvent.Save,
        ).inOrder()
    }

    @Test
    fun `the form for a receipt asks for a picture instead of a way of payment`() = runComposeUiTest {
        val form = NewExpenseUi(day = LocalDay.of(2026, 10, 15), kind = ExpenseKind.RECEIPT, needsPicture = true)
        setContent { show(state.copy(form = form)) }

        onNodeWithText("Выбрать фото чека").assertIsDisplayed()
        onNodeWithText("Выберите фото чека").assertIsDisplayed()
        onNodeWithText("Наличные").assertDoesNotExist()
        onNodeWithText("Выплата бригаде").performClick()

        assertThat(events).containsExactly(ExpenseFormEvent.KindChanged(ExpenseKind.CREW))
    }
}
