package ru.prorabprime.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import org.jetbrains.compose.resources.stringResource
import ru.prorabprime.core.designsystem.resources.Res
import ru.prorabprime.core.designsystem.resources.designsystem_close
import ru.prorabprime.designsystem.theme.Spacing

/**
 * A sheet that slides up from the bottom for a choice (filters, sorting, a list to pick from): a title with a
 * cross at its end, then the content. The one place the sheet's experimental API is opted into.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProrabBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m).padding(bottom = Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.designsystem_close))
                }
            }
            content()
        }
    }
}

/** The "reset all" line under a sheet's title. Always drawn, so the sheet keeps its height; [enabled] says it works. */
@Composable
fun SheetResetAction(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        modifier = modifier.clickable(enabled = enabled, onClick = onClick).padding(vertical = Spacing.s),
    )
}

/** A captioned group of [FilterChip]s that wrap onto as many lines as they need. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterChipGroup(
    caption: String,
    modifier: Modifier = Modifier,
    chips: @Composable () -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text(caption, style = MaterialTheme.typography.bodyMedium)
        // The chips are 32dp tall and 48dp to touch; without this the rows of a wrapped list drift far apart.
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) { chips() }
        }
    }
}
