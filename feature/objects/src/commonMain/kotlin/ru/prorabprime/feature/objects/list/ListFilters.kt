package ru.prorabprime.feature.objects.list

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ru.prorabprime.designsystem.components.ActionChip
import ru.prorabprime.designsystem.components.FilterChip
import ru.prorabprime.designsystem.components.ProrabBottomSheet
import ru.prorabprime.designsystem.components.SearchField
import ru.prorabprime.designsystem.icons.ProrabIcons
import ru.prorabprime.designsystem.theme.Spacing
import ru.prorabprime.domain.model.ObjectSort
import ru.prorabprime.domain.model.ObjectStatus
import ru.prorabprime.feature.objects.components.label
import ru.prorabprime.feature.objects.resources.Res
import ru.prorabprime.feature.objects.resources.objectslist_filters
import ru.prorabprime.feature.objects.resources.objectslist_filters_active
import ru.prorabprime.feature.objects.resources.objectslist_filters_reset
import ru.prorabprime.feature.objects.resources.objectslist_search
import ru.prorabprime.feature.objects.resources.objectslist_search_close
import ru.prorabprime.feature.objects.resources.objectslist_search_open
import ru.prorabprime.feature.objects.resources.objectslist_sort
import ru.prorabprime.feature.objects.resources.objectslist_sort_address_asc
import ru.prorabprime.feature.objects.resources.objectslist_sort_address_desc
import ru.prorabprime.feature.objects.resources.objectslist_sort_created
import ru.prorabprime.feature.objects.resources.objectslist_sort_updated

/** A search field's height (44dp) and a margin above and below it. */
private val RowHeight = 60.dp

private enum class ListSheet { FILTERS, SORT }

/** The filter and sort buttons, each opening a sheet, and the search icon at the end. */
@Composable
internal fun FilterRow(state: ObjectsListState, onEvent: (ObjectsListEvent) -> Unit) {
    // Which sheet is open is view state with no meaning beyond this composable.
    var sheet by remember { mutableStateOf<ListSheet?>(null) }
    // Both states are one height, so opening the search never moves the list under it.
    Crossfade(
        targetState = state.searchOpen,
        modifier = Modifier.fillMaxWidth().height(RowHeight),
        label = "search",
    ) { searching ->
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.m),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (searching) {
                SearchField(state.search, onEvent, Modifier.weight(1f))
                IconButton(onClick = { onEvent(ObjectsListEvent.SearchToggled) }) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = stringResource(Res.string.objectslist_search_close),
                    )
                }
            } else {
                ActionChip(
                    stringResource(Res.string.objectslist_filters),
                    onClick = { sheet = ListSheet.FILTERS },
                    icon = ProrabIcons.Filter,
                    badgeDescription = stringResource(Res.string.objectslist_filters_active)
                        .takeIf { state.statuses.isNotEmpty() },
                )
                // The chip says how the list is sorted now; the sheet is where to change it.
                ActionChip(
                    stringResource(state.sort.label),
                    onClick = { sheet = ListSheet.SORT },
                    trailingIcon = Icons.Default.ArrowDropDown,
                )
                ActionChip(
                    stringResource(Res.string.objectslist_search_open),
                    onClick = { onEvent(ObjectsListEvent.SearchToggled) },
                    icon = Icons.Default.Search,
                )
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
private fun FiltersSheet(
    statuses: Set<ObjectStatus>,
    onEvent: (ObjectsListEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    ProrabBottomSheet(title = stringResource(Res.string.objectslist_filters), onDismiss = onDismiss) {
        // Always there, so the sheet does not change height; it only works when something is switched on.
        val canReset = statuses.isNotEmpty()
        Text(
            stringResource(Res.string.objectslist_filters_reset),
            style = MaterialTheme.typography.labelLarge,
            color = if (canReset) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .clickable(enabled = canReset) { onEvent(ObjectsListEvent.FiltersCleared) }
                .padding(vertical = Spacing.s),
        )
        // The chips are 32dp tall and 48dp to touch; without this the rows of a wrapped list drift far apart.
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
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
