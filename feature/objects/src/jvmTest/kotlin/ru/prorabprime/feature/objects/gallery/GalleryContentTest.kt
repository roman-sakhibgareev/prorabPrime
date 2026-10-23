package ru.prorabprime.feature.objects.gallery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import org.junit.Test
import ru.prorabprime.designsystem.theme.ProrabTheme
import ru.prorabprime.domain.model.ServerFilePath

@OptIn(ExperimentalTestApi::class)
class GalleryContentTest {

    private val events = mutableListOf<GalleryEvent>()
    private val opened = mutableListOf<String>()
    private var backed = 0

    private fun tile(id: String, amount: String? = null) = GalleryPhotoUi(
        id,
        ServerFilePath("/files/$id.jpg"),
        ServerFilePath("/files/$id-full.jpg"),
        isCover = false,
        isPending = false,
        quarterTurns = 0,
        amount,
    )

    private val photos = persistentListOf(tile("p1"), tile("p2", "790 ₽"), tile("p3"))

    @Composable
    private fun show(state: GalleryState) {
        ProrabTheme { GalleryContent(state, { events += it }, { opened += it }, { backed++ }) }
    }

    @Test
    fun `the title says which folder, and a tile with a sum shows it`() = runComposeUiTest {
        setContent { show(GalleryState(status = GalleryStatus.Content, receipts = true, photos = photos)) }

        onNodeWithText("Чеки").assertIsDisplayed()
        onNodeWithText("790 ₽").assertIsDisplayed()
        onNodeWithContentDescription("Назад").performClick()
        assertThat(backed).isEqualTo(1)
    }

    @Test
    fun `a folder with nothing in it says so and offers no selecting`() = runComposeUiTest {
        setContent { show(GalleryState(status = GalleryStatus.Content)) }

        onNodeWithText("Здесь пока пусто").assertIsDisplayed()
        onNodeWithText("Выбрать").assertDoesNotExist()
    }

    @Test
    fun `the select button starts selecting`() = runComposeUiTest {
        setContent { show(GalleryState(status = GalleryStatus.Content, photos = photos)) }

        onNodeWithText("Выбрать").performClick()

        assertThat(events).containsExactly(GalleryEvent.SelectingToggled)
    }

    @Test
    fun `while selecting the bar counts, picks all and deletes`() = runComposeUiTest {
        setContent {
            show(
                GalleryState(
                    status = GalleryStatus.Content,
                    photos = photos,
                    selecting = true,
                    selected = persistentSetOf("p1", "p3"),
                ),
            )
        }

        onNodeWithText("Выбрано: 2").assertIsDisplayed()
        onNodeWithContentDescription("Выбрать все").performClick()
        onNodeWithText("Отправить").performClick()
        onNodeWithContentDescription("Удалить выбранные").performClick()
        onNodeWithContentDescription("Отменить выбор").performClick()

        assertThat(events).containsExactly(
            GalleryEvent.SelectAllToggled,
            GalleryEvent.ShareClicked,
            GalleryEvent.DeleteClicked,
            GalleryEvent.SelectingToggled,
        ).inOrder()
    }

    @Test
    fun `on a narrow phone the count stays on one line beside the actions`() = runComposeUiTest {
        val many = (1..12).map { tile("p$it") }
        setContent {
            Box(Modifier.width(360.dp)) {
                show(
                    GalleryState(
                        status = GalleryStatus.Content,
                        photos = many.toPersistentList(),
                        selecting = true,
                        selected = many.map { it.id }.toPersistentSet(),
                    ),
                )
            }
        }

        val layouts = mutableListOf<TextLayoutResult>()
        onNodeWithText("Выбрано: 12").assertIsDisplayed().fetchSemanticsNode().config
            .getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
        assertThat(layouts.single().lineCount).isEqualTo(1)
        assertThat(layouts.single().hasVisualOverflow).isFalse()
        onNodeWithText("Отправить").assertIsDisplayed()
    }
}
