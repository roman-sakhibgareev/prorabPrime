package ru.prorabprime.feature.objects.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ru.prorabprime.designsystem.components.FilterChip
import ru.prorabprime.designsystem.components.OutlinedButton
import ru.prorabprime.designsystem.components.ProrabBottomSheet
import ru.prorabprime.designsystem.components.SearchField
import ru.prorabprime.designsystem.components.TextButton
import ru.prorabprime.designsystem.icons.ProrabIcons
import ru.prorabprime.designsystem.theme.Spacing
import ru.prorabprime.domain.model.ObjectSort
import ru.prorabprime.domain.model.ObjectStatus
import ru.prorabprime.feature.objects.components.label
import ru.prorabprime.feature.objects.resources.Res
import ru.prorabprime.feature.objects.resources.objectslist_clear_search
import ru.prorabprime.feature.objects.resources.objectslist_filters
import ru.prorabprime.feature.objects.resources.objectslist_filters_count
import ru.prorabprime.feature.objects.resources.objectslist_filters_reset
import ru.prorabprime.feature.objects.resources.objectslist_search
import ru.prorabprime.feature.objects.resources.objectslist_search_close
import ru.prorabprime.feature.objects.resources.objectslist_search_open
import ru.prorabprime.feature.objects.resources.objectslist_sort
import ru.prorabprime.feature.objects.resources.objectslist_sort_address_asc
import ru.prorabprime.feature.objects.resources.objectslist_sort_address_desc
import ru.prorabprime.feature.objects.resources.objectslist_sort_created
import ru.prorabprime.feature.objects.resources.objectslist_sort_updated

private enum class ListSheet { FILTERS, SORT }

/** The filter and sort buttons, each opening a sheet, and the search icon at the end. */
@Composable
internal fun FilterRow(state: ObjectsListState, onEvent: (ObjectsListEvent) -> Unit) {
    // Which sheet is open is view state with no meaning beyond this composable.
    var sheet by remember { mutableStateOf<ListSheet?>(null) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val filters = if (state.statuses.isEmpty()) {
            stringResource(Res.string.objectslist_filters)
        } else {
            stringResource(Res.string.objectslist_filters_count, state.statuses.size)
        }
        SheetButton(ProrabIcons.Filter, filters) { sheet = ListSheet.FILTERS }
        SheetButton(ProrabIcons.Sort, stringResource(Res.string.objectslist_sort)) { sheet = ListSheet.SORT }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = { onEvent(ObjectsListEvent.SearchToggled) }) {
            if (state.searchOpen) {
                Icon(Icons.Default.Clear, stringResource(Res.string.objectslist_search_close))
            } else {
                Icon(Icons.Default.Search, stringResource(Res.string.objectslist_search_open))
            }
        }
    }
    when (sheet) {
        ListSheet.FILTERS -> FiltersSheet(state.statuses, onEvent, onDismiss = { sheet = null })

        ListSheet.SORT -> SortSheet(
            selected = state.sort,
            onSelect = {
                onEvent(ObjectsListEvent.SortSelected(it))
                sheet = null
            },
            onDismiss = { sheet = null },
        )

        null -> Unit
    }
}

@Composable
private fun SheetButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    OutlinedButton(onClick = onClick) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(Spacing.s))
        Text(label)
    }
}

@Composable
private fun FiltersSheet(
    statuses: Set<ObjectStatus>,
    onEvent: (ObjectsListEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    ProrabBottomSheet(title = stringResource(Res.string.objectslist_filters), onDismiss = onDismiss) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            ObjectStatus.entries.forEach { status ->
                FilterChip(
                    selected = status in statuses,
                    onClick = { onEvent(ObjectsListEvent.StatusToggled(status)) },
                    label = { Text(stringResource(status.label)) },
                )
            }
        }
        if (statuses.isNotEmpty()) {
            TextButton(onClick = { onEvent(ObjectsListEvent.FiltersCleared) }) {
                Text(stringResource(Res.string.objectslist_filters_reset))
            }
        }
    }
}

@Composable
private fun SortSheet(
    selected: ObjectSort,
    onSelect: (ObjectSort) -> Unit,
    onDismiss: () -> Unit,
) {
    ProrabBottomSheet(title = stringResource(Res.string.objectslist_sort), onDismiss = onDismiss) {
        ObjectSort.entries.forEach { sort ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(sort) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = sort == selected, onClick = null)
                Spacer(Modifier.size(Spacing.s))
                Text(stringResource(sort.label))
            }
        }
    }
}

@Composable
internal fun SearchField(
    text: String,
    onEvent: (ObjectsListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    SearchField(
        value = text,
        onValueChange = { onEvent(ObjectsListEvent.SearchChanged(it)) },
        placeholder = stringResource(Res.string.objectslist_search),
        clearDescription = stringResource(Res.string.objectslist_clear_search),
        modifier = modifier,
        autoFocus = true,
    )
}

internal val ObjectSort.label: StringResource
    get() = when (this) {
        ObjectSort.ADDRESS_ASC -> Res.string.objectslist_sort_address_asc
        ObjectSort.ADDRESS_DESC -> Res.string.objectslist_sort_address_desc
        ObjectSort.CREATED_NEWEST -> Res.string.objectslist_sort_created
        ObjectSort.UPDATED_NEWEST -> Res.string.objectslist_sort_updated
    }
