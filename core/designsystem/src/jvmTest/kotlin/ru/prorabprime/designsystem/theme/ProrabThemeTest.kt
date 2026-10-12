package ru.prorabprime.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.google.common.truth.Truth.assertThat
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ProrabThemeTest {

    private fun TextStyle.scale() = "${fontSize.value.toInt()}/${lineHeight.value.toInt()}"

    @Test
    fun `the type scale is the one the screens are designed on`() = runComposeUiTest {
        lateinit var scale: Map<String, String>
        setContent {
            ProrabTheme {
                val type = MaterialTheme.typography
                scale = mapOf(
                    "screen" to type.headlineSmall.scale(),
                    "section" to type.titleLarge.scale(),
                    "card" to type.titleMedium.scale(),
                    "text" to type.bodyLarge.scale(),
                    "secondary" to type.bodyMedium.scale(),
                    "label" to type.labelLarge.scale(),
                    "caption" to type.labelMedium.scale(),
                    "footnote" to type.labelSmall.scale(),
                )
            }
        }

        assertThat(scale).containsExactly(
            "screen", "24/28",
            "section", "20/24",
            "card", "16/20",
            "text", "16/20",
            "secondary", "14/20",
            "label", "14/16",
            "caption", "12/16",
            "footnote", "10/12",
        )
    }

    @Test
    fun `headings are heavier than text`() = runComposeUiTest {
        lateinit var weights: List<FontWeight?>
        setContent {
            ProrabTheme {
                val type = MaterialTheme.typography
                weights = listOf(type.titleLarge.fontWeight, type.titleMedium.fontWeight, type.bodyLarge.fontWeight)
            }
        }

        assertThat(weights).containsExactly(FontWeight.SemiBold, FontWeight.SemiBold, FontWeight.Normal).inOrder()
    }

    @Test
    fun `the badge has its own color in each theme`() = runComposeUiTest {
        val colors = mutableListOf<androidx.compose.ui.graphics.Color>()
        setContent {
            ProrabTheme(darkTheme = false) { colors += ProrabColors.badge }
            ProrabTheme(darkTheme = true) { colors += ProrabColors.badge }
        }

        assertThat(colors).hasSize(2)
        assertThat(colors[0]).isNotEqualTo(colors[1])
    }
}
