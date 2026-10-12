package ru.prorabprime.feature.objects.details

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import ru.prorabprime.designsystem.theme.ProrabTheme
import ru.prorabprime.domain.model.LocalImageRef
import ru.prorabprime.domain.model.ObjectStatus
import ru.prorabprime.ui.DialogModel
import ru.prorabprime.ui.UiText

@OptIn(ExperimentalTestApi::class)
class ObjectDetailsContentTest {

    private val events = mutableListOf<ObjectDetailsEvent>()
    private var edits = 0

    private val details =
        ObjectDetailsUi("Кухня", "Тверская, 5", ObjectStatus.PAUSED, "Иван", "+7 900 123-45-67", "Ключи у соседа")

    @Composable
    private fun show(state: ObjectDetailsState) {
        ProrabTheme { ObjectDetailsContent(state, onEvent = { events += it }, onEdit = { edits++ }, onBack = {}) }
    }

    @Test
    fun `every filled field is shown`() = runComposeUiTest {
        setContent { show(ObjectDetailsState(status = ObjectDetailsStatus.Content, details = details)) }

        onNodeWithText("Тверская, 5").assertIsDisplayed()
        onNodeWithText("На паузе").assertIsDisplayed()
        onNodeWithText("Иван").assertIsDisplayed()
        onNodeWithText("+7 900 123-45-67").assertIsDisplayed()
        onNodeWithText("Ключи у соседа").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `the facts sit in titled cards, and a part with nothing in it is not drawn`() = runComposeUiTest {
        setContent { show(ObjectDetailsState(status = ObjectDetailsStatus.Content, details = details)) }

        onNodeWithText("Клиент").performScrollTo().assertIsDisplayed()
        onNodeWithText("Заметки").performScrollTo().assertIsDisplayed()
        onNodeWithText("Контакты").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `an object with no client and no notes shows neither card`() = runComposeUiTest {
        val bare = ObjectDetailsUi("Кухня", null, ObjectStatus.PLANNED, null, null, null)
        setContent { show(ObjectDetailsState(status = ObjectDetailsStatus.Content, details = bare)) }

        onNodeWithText("Клиент").assertDoesNotExist()
        onNodeWithText("Заметки").assertDoesNotExist()
    }

    @Test
    fun `the phone is a link that can be tapped`() = runComposeUiTest {
        setContent { show(ObjectDetailsState(status = ObjectDetailsStatus.Content, details = details)) }

        onNodeWithText("+7 900 123-45-67").performScrollTo().assertHasClickAction()
    }

    @Test
    fun `tapping the status opens the sheet and picking one changes it`() = runComposeUiTest {
        setContent { show(ObjectDetailsState(status = ObjectDetailsStatus.Content, details = details)) }

        onNodeWithText("На паузе").performClick()
        onNodeWithText("Статус объекта").assertIsDisplayed()
        onNodeWithText("Завершён").performClick()

        assertThat(events).containsExactly(ObjectDetailsEvent.StatusPicked(ObjectStatus.DONE))
    }

    @Test
    fun `the journal and the calendar of the object open from its card`() = runComposeUiTest {
        var journals = 0
        var calendars = 0
        setContent {
            ProrabTheme {
                ObjectDetailsContent(
                    state = ObjectDetailsState(status = ObjectDetailsStatus.Content, details = details),
                    onEvent = {},
                    onEdit = {},
                    onBack = {},
                    onOpenJournal = { journals++ },
                    onOpenCalendar = { calendars++ },
                )
            }
        }

        onNodeWithText("Журнал").performClick()
        onNodeWithText("Календарь").performClick()

        assertThat(journals to calendars).isEqualTo(1 to 1)
    }

    @Test
    fun `the toolbar edits and asks to delete`() = runComposeUiTest {
        setContent { show(ObjectDetailsState(status = ObjectDetailsStatus.Content, details = details)) }

        onNodeWithContentDescription("Редактировать").performClick()
        onNodeWithContentDescription("Удалить объект").performClick()

        assertThat(edits).isEqualTo(1)
        assertThat(events).containsExactly(ObjectDetailsEvent.DeleteClicked)
    }

    @Test
    fun `the confirmation dialog confirms and dismisses`() = runComposeUiTest {
        val dialog = DialogModel.Confirmation(
            UiText.Raw("Удалить объект?"),
            UiText.Raw("Навсегда."),
            UiText.Raw("Удалить"),
            true,
        )
        setContent {
            show(ObjectDetailsState(status = ObjectDetailsStatus.Content, details = details, dialog = dialog))
        }

        onNodeWithText("Навсегда.").assertIsDisplayed()
        onNodeWithText("Удалить").performClick()

        assertThat(events).containsExactly(ObjectDetailsEvent.DialogConfirmed)
    }

    @Test
    fun `the camera tile offers the camera and the gallery`() = runComposeUiTest {
        var takes = 0
        var picks = 0
        setContent {
            ProrabTheme {
                ObjectDetailsContent(
                    state = ObjectDetailsState(status = ObjectDetailsStatus.Content, details = details),
                    onEvent = {},
                    onEdit = {},
                    onBack = {},
                    onTakePhoto = { takes++ },
                    onPickPhotos = { picks++ },
                )
            }
        }

        onNodeWithText("Добавить фото").performClick()
        onNodeWithText("Сфотографировать").performClick()
        onNodeWithText("Добавить фото").performClick()
        onNodeWithText("Выбрать из галереи").performClick()

        assertThat(takes to picks).isEqualTo(1 to 1)
    }

    @Test
    fun `a failed upload shows why and can be retried`() = runComposeUiTest {
        val upload = UploadUi(LocalImageRef("content://a"), failure = UiText.Raw("Фото слишком большое"))
        setContent {
            show(
                ObjectDetailsState(
                    status = ObjectDetailsStatus.Content,
                    details = details,
                    uploads = persistentListOf(upload),
                ),
            )
        }

        onNodeWithText("Фото слишком большое").assertIsDisplayed()
        onNodeWithContentDescription("Повторить загрузку").performClick()

        assertThat(events).containsExactly(ObjectDetailsEvent.RetryUpload(upload.image))
    }

    @Test
    fun `an error offers a retry`() = runComposeUiTest {
        setContent { show(ObjectDetailsState(status = ObjectDetailsStatus.Error(UiText.Raw("Нет связи")))) }

        onNodeWithText("Повторить").performClick()

        assertThat(events).containsExactly(ObjectDetailsEvent.Retry)
    }
}
