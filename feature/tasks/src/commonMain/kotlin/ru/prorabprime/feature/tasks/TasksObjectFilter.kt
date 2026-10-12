package ru.prorabprime.feature.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import org.jetbrains.compose.resources.stringResource
import ru.prorabprime.designsystem.components.OutlinedButton
import ru.prorabprime.designsystem.components.ProrabBottomSheet
import ru.prorabprime.designsystem.components.TextButton
import ru.prorabprime.designsystem.theme.Spacing
import ru.prorabprime.feature.tasks.resources.Res
import ru.prorabprime.feature.tasks.resources.tasks_all_objects
import ru.prorabprime.feature.tasks.resources.tasks_objects_count
import ru.prorabprime.feature.tasks.resources.tasks_objects_show_all
import ru.prorabprime.feature.tasks.resources.tasks_objects_title

/**
 * Names the objects the plan is narrowed to and, on a tap, opens a sheet to tick which ones to show.
 * Nothing ticked is every object.
 */
@Composable
internal fun ObjectFilterButton(
    selectedIds: ImmutableSet<String>,
    objects: ImmutableList<ObjectChoiceUi>,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Whether the sheet is open is view state with no meaning beyond this button.
    var open by remember { mutableStateOf(false) }
    val label = when (selectedIds.size) {
        0 -> stringResource(Res.string.tasks_all_objects)
        1 -> objects.find { it.id in selectedIds }?.title ?: stringResource(Res.string.tasks_all_objects)
        else -> stringResource(Res.string.tasks_objects_count, selectedIds.size)
    }
    OutlinedButton(onClick = { open = true }, modifier = modifier) {
        Text(label)
        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
    }
    if (open) {
        ProrabBottomSheet(title = stringResource(Res.string.tasks_objects_title), onDismiss = { open = false }) {
            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
            ) {
                objects.forEach { choice ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onToggle(choice.id) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = choice.id in selectedIds, onCheckedChange = null)
                        Spacer(Modifier.size(Spacing.s))
                        Text(choice.title)
                    }
                }
            }
            if (selectedIds.isNotEmpty()) {
                TextButton(onClick = onClear) { Text(stringResource(Res.string.tasks_objects_show_all)) }
            }
        }
    }
}
