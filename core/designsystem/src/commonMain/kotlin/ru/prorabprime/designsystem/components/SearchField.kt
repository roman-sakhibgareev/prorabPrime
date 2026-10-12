package ru.prorabprime.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ru.prorabprime.core.designsystem.resources.Res
import ru.prorabprime.core.designsystem.resources.designsystem_clear
import ru.prorabprime.designsystem.theme.ProrabTheme
import ru.prorabprime.designsystem.theme.Sizes
import ru.prorabprime.designsystem.theme.Spacing

/**
 * A flat search line as tall as a button, not as a form field: it sits above a list and must not eat it.
 * The cross inside clears the text; closing the line is the caller's control beside it.
 */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    /** Takes the keyboard as soon as it appears, for a line that was asked for with an icon. */
    autoFocus: Boolean = false,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (autoFocus) focus.requestFocus() }
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface),
        cursorBrush = SolidColor(colors.primary),
        modifier = modifier.fillMaxWidth().heightIn(min = FieldHeight).focusRequester(focus),
        decorationBox = { inner ->
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = FieldHeight)
                    .background(colors.surfaceContainerHigh, shape)
                    .padding(horizontal = Spacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(IconSize),
                )
                Box(Modifier.weight(1f).padding(horizontal = Spacing.s), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    inner()
                }
                if (value.isNotEmpty()) {
                    IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(ClearSize)) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = stringResource(Res.string.designsystem_clear),
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(IconSize),
                        )
                    }
                }
            }
        },
    )
}

private val FieldHeight = Sizes.control
private val IconSize = 24.dp
private val ClearSize = 32.dp

@Preview
@Composable
private fun SearchFieldPreview() {
    ProrabTheme {
        Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            SearchField(value = "", onValueChange = {}, placeholder = "Адрес или название")
            SearchField(value = "Ленина 5", onValueChange = {}, placeholder = "Адрес или название")
        }
    }
}
