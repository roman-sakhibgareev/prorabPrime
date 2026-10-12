package ru.prorabprime.feature.journal

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import ru.prorabprime.designsystem.theme.ProrabTheme
import ru.prorabprime.domain.model.FieldProblem
import ru.prorabprime.domain.model.LocalDay
import ru.prorabprime.ui.UiText

@OptIn(ExperimentalTestApi::class)
class JournalContentTest {

    private val events = mutableListOf<JournalEvent>()
    private var backs = 0

    private val journal = JournalUi(
        persistentListOf(
            JournalDayUi(
                LocalDay.of(2026, 10, 12),
                DayHeading.Yesterday,
                persistentListOf(NoteUi("n1", "Стены оштукатурены", "17:40")),
            ),
            JournalDayUi(
                LocalDay.of(2026, 10, 13),
                DayHeading.Today,
                persistentListOf(NoteUi("n2", "Заказать штукатурку", "10:10")),
            ),
        ),
    )

    private fun androidx.compose.ui.test.ComposeUiTest.show(state: JournalState) = setContent {
        ProrabTheme { JournalContent(state, { events += it }, { backs++ }) }
    }

    @Test
    fun `every line shows its time under its day`() = runComposeUiTest {
        show(JournalState(status = JournalStatus.Content, journal = journal))

        onNodeWithText("Вчера").assertIsDisplayed()
        onNodeWithText("Сегодня").assertIsDisplayed()
        onNodeWithText("17:40").assertIsDisplayed()
        onNodeWithText("Стены оштукатурены").assertIsDisplayed()
        onNodeWithText("Заказать штукатурку").assertIsDisplayed()
    }

    @Test
    fun `a tap on a line opens it`() = runComposeUiTest {
        show(JournalState(status = JournalStatus.Content, journal = journal))

        onNodeWithText("Стены оштукатурены").performClick()

        assertThat(events).containsExactly(JournalEvent.NoteClicked("n1"))
    }

    @Test
    fun `an empty log says what it is for`() = runComposeUiTest {
        show(JournalState(status = JournalStatus.Content, journal = JournalUi()))

        onNodeWithText("Записей пока нет").assertIsDisplayed()
    }

    @Test
    fun `the send button waits for text and sends it`() = runComposeUiTest {
        show(JournalState(status = JournalStatus.Content, journal = journal))
        onNodeWithContentDescription("Добавить запись").assertIsNotEnabled()

        show(JournalState(status = JournalStatus.Content, journal = journal, draft = "Привезли плитку"))
        onNodeWithContentDescription("Добавить запись").assertIsEnabled().performClick()

        assertThat(events).containsExactly(JournalEvent.SendClicked)
    }

    @Test
    fun `an error offers a retry and back leaves`() = runComposeUiTest {
        show(JournalState(status = JournalStatus.Error(UiText.Raw("Сервер не отвечает"))))

        onNodeWithText("Повторить").performClick()
        onNodeWithContentDescription("Назад").performClick()

        assertThat(events).containsExactly(JournalEvent.Retry)
        assertThat(backs).isEqualTo(1)
    }

    @Test
    fun `the open line can be saved or deleted`() = runComposeUiTest {
        show(
            JournalState(
                status = JournalStatus.Content,
                journal = journal,
                editor = NoteEditorUi("n1", "Стены оштукатурены"),
            ),
        )

        onNodeWithText("Сохранить").performClick()
        onNodeWithText("Удалить").performClick()

        assertThat(events)
            .containsExactly(JournalEvent.EditorSaveClicked, JournalEvent.DeleteClicked("n1")).inOrder()
    }

    @Test
    fun `a blank draft shows its problem`() = runComposeUiTest {
        show(
            JournalState(
                status = JournalStatus.Content,
                journal = journal,
                draft = " ",
                draftError = FieldProblem.REQUIRED,
            ),
        )

        onNodeWithText("Напишите хоть что-нибудь").assertIsDisplayed()
    }
}
