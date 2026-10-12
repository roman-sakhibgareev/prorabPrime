package ru.prorabprime.feature.tasks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import org.jetbrains.compose.resources.stringResource
import ru.prorabprime.designsystem.components.ActionChip
import ru.prorabprime.designsystem.components.FilterChip
import ru.prorabprime.designsystem.components.FilterChipGroup
import ru.prorabprime.designsystem.components.ProrabBottomSheet
import ru.prorabprime.designsystem.components.SheetResetAction
import ru.prorabprime.feature.tasks.resources.Res
import ru.prorabprime.feature.tasks.resources.tasks_all_objects
import ru.prorabprime.feature.tasks.resources.tasks_objects_caption
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
    ActionChip(label, onClick = { open = true }, modifier = modifier, trailingIcon = Icons.Default.ArrowDropDown)
    if (open) {
        ProrabBottomSheet(title = stringResource(Res.string.tasks_objects_title), onDismiss = { open = false }) {
            SheetResetAction(
                stringResource(Res.string.tasks_objects_show_all),
                enabled = selectedIds.isNotEmpty(),
                onClick = onClear,
            )
            Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                FilterChipGroup(stringResource(Res.string.tasks_objects_caption)) {
                    objects.forEach { choice ->
                        FilterChip(
                            selected = choice.id in selectedIds,
                            onClick = { onToggle(choice.id) },
                            label = { Text(choice.title) },
                        )
                    }
                }
            }
        }
    }
}
