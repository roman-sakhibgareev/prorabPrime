package ru.prorabprime.designsystem.components

import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import ru.prorabprime.designsystem.theme.ProrabTheme

@OptIn(ExperimentalTestApi::class)
class ComponentsTest {

    @Test
    fun `a section header shows its title and its action`() = runComposeUiTest {
        setContent { ProrabTheme { SectionHeader("Контакты", action = { Text("Добавить") }) } }

        onNodeWithText("Контакты").assertIsDisplayed()
        onNodeWithText("Добавить").assertIsDisplayed()
    }

    @Test
    fun `an info row shows its name and value, and a row with an action can be tapped`() = runComposeUiTest {
        var taps = 0
        setContent {
            ProrabTheme {
                InfoCard {
                    InfoRow("Адрес", "Тверская, 5")
                    InfoRow("Телефон", "+7 900 123-45-67", onClick = { taps++ })
                }
            }
        }

        onNodeWithText("Адрес").assertIsDisplayed()
        onNodeWithText("Тверская, 5").assertIsDisplayed()
        onNodeWithText("+7 900 123-45-67").performClick()

        assertThat(taps).isEqualTo(1)
    }

    @Test
    fun `an action chip clicks, and shows its dot only when given a description for it`() = runComposeUiTest {
        var taps = 0
        val withDot = mutableStateOf(false)
        setContent {
            ProrabTheme {
                ActionChip(
                    "Фильтры",
                    onClick = { taps++ },
                    badgeDescription = if (withDot.value) "Фильтры включены" else null,
                )
            }
        }

        onNodeWithText("Фильтры").performClick()
        onNodeWithContentDescription("Фильтры включены").assertDoesNotExist()
        withDot.value = true
        onNodeWithContentDescription("Фильтры включены").assertIsDisplayed()

        assertThat(taps).isEqualTo(1)
    }

    @Test
    fun `a search line shows its cross only with text, and the cross clears the text`() = runComposeUiTest {
        val text = mutableStateOf("")
        setContent {
            ProrabTheme { SearchField(text.value, onValueChange = { text.value = it }, placeholder = "Поиск") }
        }

        onNodeWithContentDescription("Очистить").assertDoesNotExist()
        text.value = "asd"
        onNodeWithContentDescription("Очистить").performClick()

        assertThat(text.value).isEmpty()
        onNodeWithContentDescription("Очистить").assertDoesNotExist()
    }
}
