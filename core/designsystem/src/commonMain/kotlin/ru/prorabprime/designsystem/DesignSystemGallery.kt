package ru.prorabprime.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.prorabprime.designsystem.components.ActionChip
import ru.prorabprime.designsystem.components.Button
import ru.prorabprime.designsystem.components.FilterChip
import ru.prorabprime.designsystem.components.InfoCard
import ru.prorabprime.designsystem.components.InfoRow
import ru.prorabprime.designsystem.components.OutlinedButton
import ru.prorabprime.designsystem.components.SearchField
import ru.prorabprime.designsystem.components.SectionHeader
import ru.prorabprime.designsystem.components.TextButton
import ru.prorabprime.designsystem.components.TonalButton
import ru.prorabprime.designsystem.icons.ProrabIcons
import ru.prorabprime.designsystem.theme.ProrabColors
import ru.prorabprime.designsystem.theme.ProrabTheme
import ru.prorabprime.designsystem.theme.Spacing

/**
 * Every token and component of the design system on one scrolling page, to look at them side by side in a
 * preview or on a device. Sample text is literal on purpose: this is a showcase, not a screen.
 */
@Composable
fun DesignSystemGallery(modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            Text("Дизайн-система", style = MaterialTheme.typography.headlineSmall)
            TypeScale()
            Colors()
            Buttons()
            Chips()
            Cards()
            Search()
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        SectionHeader(title)
        content()
    }
}

@Composable
private fun TypeScale() = Group("Шрифты") {
    val type = MaterialTheme.typography
    val scale: List<Triple<String, TextStyle, String>> = listOf(
        Triple("Заголовок экрана", type.headlineSmall, "24/28"),
        Triple("Заголовок секции", type.titleLarge, "20/24"),
        Triple("Заголовок карточки", type.titleMedium, "16/20"),
        Triple("Текст", type.bodyLarge, "16/20"),
        Triple("Вторичный текст", type.bodyMedium, "14/20"),
        Triple("Подпись", type.labelLarge, "14/16"),
        Triple("Мелкая подпись", type.labelMedium, "12/16"),
        Triple("Сноска", type.labelSmall, "10/12"),
    )
    scale.forEach { (name, style, size) ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = style, modifier = Modifier.weight(1f))
            Text(size, style = type.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Colors() = Group("Цвета") {
    val colors = MaterialTheme.colorScheme
    val swatches: List<Pair<String, Color>> = listOf(
        "primary" to colors.primary,
        "primaryContainer" to colors.primaryContainer,
        "secondaryContainer" to colors.secondaryContainer,
        "tertiaryContainer" to colors.tertiaryContainer,
        "surfaceContainerLow" to colors.surfaceContainerLow,
        "surfaceContainerHigh" to colors.surfaceContainerHigh,
        "error" to colors.error,
        "link" to ProrabColors.link,
        "badge" to ProrabColors.badge,
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        swatches.forEach { (name, color) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(48.dp).background(color, RoundedCornerShape(8.dp)))
                Text(name, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun Buttons() = Group("Кнопки") {
    Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Главное действие") }
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        OutlinedButton(onClick = {}) { Text("Второстепенное") }
        TonalButton(onClick = {}) { Text("Мягкое") }
        TextButton(onClick = {}) { Text("Тихое") }
    }
}

@Composable
private fun Chips() = Group("Чипы") {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        FilterChip(selected = true, onClick = {}, label = { Text("Выбран") })
        FilterChip(selected = false, onClick = {}, label = { Text("Не выбран") })
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        ActionChip("Фильтры", onClick = {}, icon = ProrabIcons.Filter, badgeDescription = "Включены")
        ActionChip("По дате изменений", onClick = {}, trailingIcon = Icons.Default.ArrowDropDown)
    }
}

@Composable
private fun Cards() = Group("Блоки") {
    InfoCard {
        InfoRow("Имя", "Роман")
        InfoRow("Телефон", "+7 912 225-21-26", emphasized = true, onClick = {})
    }
}

@Composable
private fun Search() = Group("Поиск") {
    SearchField(value = "", onValueChange = {}, placeholder = "Адрес или название")
}

@Preview
@Composable
private fun DesignSystemGalleryPreview() {
    ProrabTheme { DesignSystemGallery() }
}

@Preview
@Composable
private fun DesignSystemGalleryDarkPreview() {
    ProrabTheme(darkTheme = true) { DesignSystemGallery() }
}
