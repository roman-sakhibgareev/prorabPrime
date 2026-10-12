package ru.prorabprime.designsystem

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import org.junit.Test
import ru.prorabprime.designsystem.theme.ProrabTheme

@OptIn(ExperimentalTestApi::class)
class DesignSystemGalleryTest {

    @Test
    fun `the gallery draws in the light theme`() = runComposeUiTest {
        setContent { ProrabTheme(darkTheme = false) { DesignSystemGallery() } }

        onNodeWithText("Дизайн-система").assertIsDisplayed()
    }

    @Test
    fun `the gallery draws in the dark theme`() = runComposeUiTest {
        setContent { ProrabTheme(darkTheme = true) { DesignSystemGallery() } }

        onNodeWithText("Дизайн-система").assertIsDisplayed()
    }
}
