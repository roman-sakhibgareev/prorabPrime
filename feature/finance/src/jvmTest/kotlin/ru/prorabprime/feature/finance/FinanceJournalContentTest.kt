package ru.prorabprime.feature.finance

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import ru.prorabprime.designsystem.theme.ProrabTheme
import ru.prorabprime.domain.model.JournalCash
import ru.prorabprime.domain.model.JournalFilter
import ru.prorabprime.domain.model.JournalPeriod
import ru.prorabprime.domain.model.LocalDay
import ru.prorabprime.domain.model.ObjectId
import ru.prorabprime.domain.model.Payment
import ru.prorabprime.domain.model.PaymentId
import ru.prorabprime.domain.model.PaymentMethod
import ru.prorabprime.domain.model.PaymentSide
import ru.prorabprime.ui.UiText

@OptIn(ExperimentalTestApi::class)
class FinanceJournalContentTest {

    private val events = mutableListOf<FinanceJournalEvent>()
    private var backs = 0
    private val today = LocalDay.of(2026, 10, 20)

    private val income = Payment(PaymentId("p1"), PaymentSide.CLIENT, 100_000, PaymentMethod.CASH, today, "аванс")
    private val expense = Payment(PaymentId("p2"), PaymentSide.CREW, 40_000, PaymentMethod.TRANSFER, today, null)

    private val content = FinanceJournalState(
        status = FinanceJournalStatus.Content,
        rows = persistentListOf(
            JournalRowUi(income, "Кухня", "+ 1 000 ₽", "20.10.2026", isIncome = true),
            JournalRowUi(expense, "Ванная", "− 400 ₽", "20.10.2026", isIncome = false),
        ),
        totals = JournalTotalsUi("1 000 ₽", "400 ₽", "600 ₽"),
        allCount = 2,
        objects = persistentListOf(
            JournalObjectUi(ObjectId("cellar"), "Подвал"),
            JournalObjectUi(ObjectId("kitchen"), "Кухня"),
        ),
    )

    private fun androidx.compose.ui.test.ComposeUiTest.show(state: FinanceJournalState) = setContent {
        ProrabTheme { FinanceJournalContent(state, { events += it }, { backs++ }, today = today) }
    }

    @Test
    fun `rows show the object, the signed amount, the way and the note, with the totals above`() = runComposeUiTest {
        show(content)

        onNodeWithText("Кухня").assertIsDisplayed()
        onNodeWithText("+ 1 000 ₽").assertIsDisplayed()
        onNodeWithText("− 400 ₽").assertIsDisplayed()
        onNodeWithText("Наличные · аванс").assertIsDisplayed()
        onNodeWithText("Перевод").assertIsDisplayed()
        onAllNodesWithText("Приход").assertCountEquals(2)
        onAllNodesWithText("Расход").assertCountEquals(2)
        onNodeWithText("Баланс").assertIsDisplayed()
        onNodeWithText("600 ₽").assertIsDisplayed()
    }

    @Test
    fun `tapping a row sends its id`() = runComposeUiTest {
        show(content)

        onNodeWithText("− 400 ₽").performClick()

        assertThat(events).containsExactly(FinanceJournalEvent.RowClicked("p2"))
    }

    @Test
    fun `the filters send their choices`() = runComposeUiTest {
        show(content)

        onNodeWithText("Месяц").performClick()
        onNodeWithText("Безнал").performClick()
        onAllNodesWithText("Расход")[0].performClick()

        assertThat(events).contains(FinanceJournalEvent.SideChosen(PaymentSide.CREW))
        assertThat(events).contains(FinanceJournalEvent.PeriodChanged(JournalPeriod.Month(2026, 10)))
        assertThat(events).contains(FinanceJournalEvent.CashChosen(JournalCash.NON_CASH))
    }

    @Test
    fun `a chosen month shows its name and steps to the one before`() = runComposeUiTest {
        show(content.copy(filter = JournalFilter(period = JournalPeriod.Month(2026, 1))))

        onNodeWithText("Январь 2026").assertIsDisplayed()
        onNodeWithContentDescription("Предыдущий месяц").performClick()
        onNodeWithContentDescription("Следующий месяц").performClick()

        assertThat(events).containsExactly(
            FinanceJournalEvent.PeriodChanged(JournalPeriod.Month(2025, 12)),
            FinanceJournalEvent.PeriodChanged(JournalPeriod.Month(2026, 2)),
        ).inOrder()
    }

    @Test
    fun `the object menu lists the objects and sends the one chosen`() = runComposeUiTest {
        show(content)

        onNodeWithText("Все объекты").performClick()
        onNodeWithText("Подвал").performClick()

        assertThat(events).containsExactly(FinanceJournalEvent.ObjectChosen(ObjectId("cellar")))
    }

    @Test
    fun `filters that match nothing say so and offer a reset`() = runComposeUiTest {
        show(content.copy(rows = persistentListOf(), totals = JournalTotalsUi("0 ₽", "0 ₽", "0 ₽")))

        onNodeWithText("По этим условиям платежей нет").assertIsDisplayed()
        onNodeWithText("Сбросить фильтры").performClick()

        assertThat(events).containsExactly(FinanceJournalEvent.FiltersReset)
    }

    @Test
    fun `no payments at all shows the empty message`() = runComposeUiTest {
        show(FinanceJournalState(status = FinanceJournalStatus.Content))

        onNodeWithText("Платежей пока нет", substring = true).assertIsDisplayed()
    }

    @Test
    fun `loading shows no rows`() = runComposeUiTest {
        show(FinanceJournalState())

        onNodeWithText("Журнал платежей").assertIsDisplayed()
        onNodeWithText("Кухня").assertDoesNotExist()
    }

    @Test
    fun `an error offers a retry, and back leaves`() = runComposeUiTest {
        show(FinanceJournalState(status = FinanceJournalStatus.Error(UiText.Raw("Сбой"))))

        onNodeWithText("Сбой").assertIsDisplayed()
        onNodeWithText("Повторить").performClick()
        onNodeWithContentDescription("Назад").performClick()

        assertThat(events).containsExactly(FinanceJournalEvent.Retry)
        assertThat(backs).isEqualTo(1)
    }

    @Test
    fun `the open payment form is the one of the object's screen, and its events are wrapped`() = runComposeUiTest {
        val editor = FinanceEditorUi.Payment(paymentId = "p1", amountText = "1000", day = today)
        show(content.copy(editor = editor))

        onNodeWithText("Сумма, ₽ *").assertIsDisplayed()
        onNodeWithText("Удалить").performClick()
        onNodeWithText("Сохранить").performClick()

        assertThat(events).containsExactly(
            FinanceJournalEvent.Form(PaymentEvent.Delete("p1")),
            FinanceJournalEvent.Form(PaymentEvent.Save),
        ).inOrder()
    }
}
