package ru.prorabprime.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import ru.prorabprime.designsystem.theme.ProrabColors
import ru.prorabprime.designsystem.theme.ProrabTheme
import ru.prorabprime.designsystem.theme.Spacing

/** A group of facts about one thing, on a soft panel, so a screen reads as a few blocks and not as a long list. */
@Composable
fun InfoCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
            content = content,
        )
    }
}

/**
 * A fact: its name small and quiet, its value under it. [emphasized] sets the value in the card-title size for the
 * fact that matters most; [onClick] makes it a link-colored action, as a phone number is.
 */
@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    valueColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
) {
    val color = when {
        onClick != null -> ProrabColors.link
        valueColor == Color.Unspecified -> MaterialTheme.colorScheme.onSurface
        else -> valueColor
    }
    Column(
        modifier = (if (onClick != null) modifier.clickable(onClick = onClick) else modifier).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            color = color,
        )
    }
}

@Preview
@Composable
private fun InfoCardPreview() {
    ProrabTheme {
        InfoCard(Modifier.padding(Spacing.m)) {
            InfoRow("Клиент", "Роман", emphasized = true)
            InfoRow("Телефон", "+7 912 225-21-26", onClick = {})
            InfoRow("Заметки", "Ключи у соседа")
        }
    }
}
