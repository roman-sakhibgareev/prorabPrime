package ru.prorabprime.feature.tasks

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import ru.prorabprime.designsystem.components.OutlinedButton

/**
 * A button that names the chosen object and opens the list of them. [noneLabel] is what "no object"
 * is called here: "all objects" for a filter, "no object" for a task.
 */
@Composable
internal fun ObjectSelector(
    selectedId: String?,
    objects: ImmutableList<ObjectChoiceUi>,
    noneLabel: String,
    onPicked: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Whether the menu is open is view state with no meaning beyond this button.
    var open by remember { mutableStateOf(false) }
    val selectedTitle = objects.find { it.id == selectedId }?.title ?: noneLabel
    Box(modifier) {
        OutlinedButton(onClick = { open = true }) { Text(selectedTitle) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(noneLabel) },
                onClick = {
                    open = false
                    onPicked(null)
                },
            )
            objects.forEach { choice ->
                DropdownMenuItem(
                    text = { Text(choice.title) },
                    onClick = {
                        open = false
                        onPicked(choice.id)
                    },
                )
            }
        }
    }
}
