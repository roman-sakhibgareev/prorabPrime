package ru.prorabprime.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import ru.prorabprime.designsystem.theme.Sizes
import ru.prorabprime.designsystem.theme.Spacing

/**
 * The app's text field: as tall as a button and the search line, with its name above it rather than inside, so
 * the name stays on screen while typing. [error] is drawn under the field and turns its outline red.
 */
@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    caption: String? = null,
    placeholder: String = "",
    error: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large
    // One node for the caption and the field, so a screen reader names the field and a test finds it by its caption.
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface),
        cursorBrush = SolidColor(colors.primary),
        modifier = modifier.fillMaxWidth(),
        // The caption and the error are part of the decoration, so they belong to the field for a screen reader
        // and a test alike.
        decorationBox = { inner ->
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                if (caption != null) Text(caption, style = MaterialTheme.typography.bodyMedium)
                Box(
                    modifier = Modifier.fillMaxWidth().heightIn(min = Sizes.control)
                        .background(colors.surfaceContainerLowest, shape)
                        .border(1.dp, if (error == null) colors.outline else colors.error, shape)
                        .padding(horizontal = Spacing.m, vertical = if (singleLine) 0.dp else Spacing.s),
                    contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
                ) {
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
                if (error != null) Text(error, style = MaterialTheme.typography.bodyMedium, color = colors.error)
            }
        },
    )
}
