package ru.prorabprime.feature.objects.list

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ru.prorabprime.designsystem.components.ActionChip
import ru.prorabprime.designsystem.components.FilterChip
import ru.prorabprime.designsystem.components.FilterChipGroup
import ru.prorabprime.designsystem.components.ProrabBottomSheet
import ru.prorabprime.designsystem.components.SearchField
import ru.prorabprime.designsystem.components.SheetResetAction
import ru.prorabprime.designsystem.components.TextButton
import ru.prorabprime.designsystem.icons.ProrabIcons
import ru.prorabprime.designsystem.theme.Spacing
import ru.prorabprime.domain.model.ObjectSort
import ru.prorabprime.domain.model.ObjectStatus
import ru.prorabprime.feature.objects.components.label
import ru.prorabprime.feature.objects.resources.Res
import ru.prorabprime.feature.objects.resources.objectslist_filters
import ru.prorabprime.feature.objects.resources.objectslist_filters_active
import ru.prorabprime.feature.objects.resources.objectslist_filters_reset
import ru.prorabprime.feature.objects.resources.objectslist_filters_status
import ru.prorabprime.feature.objects.resources.objectslist_search
import ru.prorabprime.feature.objects.resources.objectslist_search_cancel
import ru.prorabprime.feature.objects.resources.objectslist_search_open
import ru.prorabprime.feature.objects.resources.objectslist_sort
import ru.prorabprime.feature.objects.resources.objectslist_sort_address_asc
import ru.prorabprime.feature.objects.resources.objectslist_sort_address_desc
import ru.prorabprime.feature.objects.resources.objectslist_sort_created
import ru.prorabprime.feature.objects.resources.objectslist_sort_updated

/** A search field's height (48dp) and a margin above and below it. */
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
                TextButton(onClick = { onEvent(ObjectsListEvent.SearchToggled) }) {
                    Text(stringResource(Res.string.objectslist_search_cancel))
                }
            } else {
                ListChips(
                    state,
                    onEvent,
                    onFilters = { sheet = ListSheet.FILTERS },
                    onSort = { sheet = ListSheet.SORT },
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
        SheetResetAction(
            stringResource(Res.string.objectslist_filters_reset),
            enabled = statuses.isNotEmpty(),
            onClick = { onEvent(ObjectsListEvent.FiltersCleared) },
        )
        FilterChipGroup(stringResource(Res.string.objectslist_filters_status)) {
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

/** The three buttons of the closed row; see [CollapsingChips]. */
@Composable
private fun ListChips(
    state: ObjectsListState,
    onEvent: (ObjectsListEvent) -> Unit,
    onFilters: () -> Unit,
    onSort: () -> Unit,
) {
    val filtersLabel = stringResource(Res.string.objectslist_filters)
    val filtersDot = stringResource(Res.string.objectslist_filters_active)
        .takeIf { state.statuses.isNotEmpty() }
    val searchLabel = stringResource(Res.string.objectslist_search_open)
    // Sort, search, filters: the sort chip says how the list is ordered now, and its words always
    // fit; when the others do not fit beside it they turn into their icons.
    CollapsingChips(
        sort = {
            ActionChip(
                stringResource(state.sort.label),
                onClick = { onSort() },
                trailingIcon = Icons.Default.ArrowDropDown,
            )
        },
        search = { iconOnly ->
            ActionChip(
                searchLabel,
                onClick = { onEvent(ObjectsListEvent.SearchToggled) },
                icon = Icons.Default.Search,
                iconOnly = iconOnly,
            )
        },
        filters = { iconOnly ->
            ActionChip(
                filtersLabel,
                onClick = { onFilters() },
                icon = ProrabIcons.Filter,
                badgeDescription = filtersDot,
                iconOnly = iconOnly,
            )
        },
    )
}

/**
 * The sort chip, then search, then filters. Filters lose their word first, then search; the sort chip keeps
 * its own, so [sort] gets whatever the two icons leave. Each part sees whether to draw as an icon only.
 */
@Composable
private fun CollapsingChips(
    sort: @Composable () -> Unit,
    search: @Composable (iconOnly: Boolean) -> Unit,
    filters: @Composable (iconOnly: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gap = Spacing.s
    SubcomposeLayout(modifier) { constraints ->
        val gapPx = gap.roundToPx()
        val loose = Constraints()
        // Words kept on both, then none on the filters, then none on either.
        val steps = listOf(false to false, false to true, true to true)
        var measurables = emptyList<Measurable>()
        for (step in steps) {
            val (searchIcon, filtersIcon) = step
            measurables = subcompose(step) {
                sort()
                search(searchIcon)
                filters(filtersIcon)
            }
            val width = measurables.sumOf { it.maxIntrinsicWidth(Int.MAX_VALUE) } + 2 * gapPx
            if (width <= constraints.maxWidth) break
        }
        val tail = measurables.drop(1).map { it.measure(loose) }
        val sortRoom = (constraints.maxWidth - tail.sumOf { it.width } - 2 * gapPx).coerceAtLeast(0)
        val sortPlaceable = measurables.first().measure(Constraints(maxWidth = sortRoom))
        val placeables = listOf(sortPlaceable) + tail
        val height = placeables.maxOf { it.height }
        layout(constraints.maxWidth, height) {
            var x = 0
            placeables.forEach {
                it.placeRelative(x, (height - it.height) / 2)
                x += it.width + gapPx
            }
        }
    }
}
