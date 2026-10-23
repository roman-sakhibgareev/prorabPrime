package ru.prorabprime.feature.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ru.prorabprime.designsystem.components.ActionChip
import ru.prorabprime.designsystem.components.FilterChip
import ru.prorabprime.designsystem.theme.Spacing
import ru.prorabprime.domain.model.JournalCash
import ru.prorabprime.domain.model.JournalPeriod
import ru.prorabprime.domain.model.LocalDay
import ru.prorabprime.domain.model.ObjectId
import ru.prorabprime.domain.model.PaymentSide
import ru.prorabprime.feature.finance.resources.Res
import ru.prorabprime.feature.finance.resources.financejournal_cash_all
import ru.prorabprime.feature.finance.resources.financejournal_cash_cash
import ru.prorabprime.feature.finance.resources.financejournal_cash_non_cash
import ru.prorabprime.feature.finance.resources.financejournal_month_1
import ru.prorabprime.feature.finance.resources.financejournal_month_10
import ru.prorabprime.feature.finance.resources.financejournal_month_11
import ru.prorabprime.feature.finance.resources.financejournal_month_12
import ru.prorabprime.feature.finance.resources.financejournal_month_2
import ru.prorabprime.feature.finance.resources.financejournal_month_3
import ru.prorabprime.feature.finance.resources.financejournal_month_4
import ru.prorabprime.feature.finance.resources.financejournal_month_5
import ru.prorabprime.feature.finance.resources.financejournal_month_6
import ru.prorabprime.feature.finance.resources.financejournal_month_7
import ru.prorabprime.feature.finance.resources.financejournal_month_8
import ru.prorabprime.feature.finance.resources.financejournal_month_9
import ru.prorabprime.feature.finance.resources.financejournal_month_year
import ru.prorabprime.feature.finance.resources.financejournal_next_month
import ru.prorabprime.feature.finance.resources.financejournal_object_all
import ru.prorabprime.feature.finance.resources.financejournal_period_all
import ru.prorabprime.feature.finance.resources.financejournal_period_month
import ru.prorabprime.feature.finance.resources.financejournal_previous_month
import ru.prorabprime.feature.finance.resources.financejournal_side_all
import ru.prorabprime.feature.finance.resources.financejournal_side_expense
import ru.prorabprime.feature.finance.resources.financejournal_side_income

@Composable
internal fun Filters(
    state: FinanceJournalState,
    onEvent: (FinanceJournalEvent) -> Unit,
    today: LocalDay,
) {
    val filter = state.filter
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.m, vertical = Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        PeriodFilters(filter.period, today) { onEvent(FinanceJournalEvent.PeriodChanged(it)) }
        ObjectFilter(state.objects, filter.objectId) { onEvent(FinanceJournalEvent.ObjectChosen(it)) }
        Choices(
            selected = filter.side,
            choices = listOf(
                null to Res.string.financejournal_side_all,
                PaymentSide.CLIENT to Res.string.financejournal_side_income,
                PaymentSide.CREW to Res.string.financejournal_side_expense,
            ),
            onChoose = { onEvent(FinanceJournalEvent.SideChosen(it)) },
        )
        Choices(
            selected = filter.cash,
            choices = listOf(
                null to Res.string.financejournal_cash_all,
                JournalCash.CASH to Res.string.financejournal_cash_cash,
                JournalCash.NON_CASH to Res.string.financejournal_cash_non_cash,
            ),
            onChoose = { onEvent(FinanceJournalEvent.CashChosen(it)) },
        )
    }
}

@Composable
private fun PeriodFilters(
    period: JournalPeriod,
    today: LocalDay,
    onChange: (JournalPeriod) -> Unit,
) {
    // The month stepper's buttons are taller than the chips; reserve that height so the rows below stay put.
    Row(
        Modifier.heightIn(min = LocalMinimumInteractiveComponentSize.current),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        FilterChip(
            selected = period == JournalPeriod.All,
            onClick = { onChange(JournalPeriod.All) },
            label = { Text(stringResource(Res.string.financejournal_period_all)) },
        )
        FilterChip(
            selected = period is JournalPeriod.Month,
            onClick = { if (period !is JournalPeriod.Month) onChange(JournalPeriod.monthOf(today)) },
            label = { Text(stringResource(Res.string.financejournal_period_month)) },
        )
        if (period is JournalPeriod.Month) {
            IconButton(onClick = { onChange(period.previous()) }) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    stringResource(Res.string.financejournal_previous_month),
                )
            }
            Text(
                stringResource(
                    Res.string.financejournal_month_year,
                    stringResource(monthName(period.month)),
                    period.year,
                ),
                style = MaterialTheme.typography.titleSmall,
            )
            IconButton(onClick = { onChange(period.next()) }) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    stringResource(Res.string.financejournal_next_month),
                )
            }
        }
    }
}

@Composable
private fun ObjectFilter(
    objects: List<JournalObjectUi>,
    selected: ObjectId?,
    onChoose: (ObjectId?) -> Unit,
) {
    // Whether the menu is open is view state with no meaning beyond this chip.
    var open by remember { mutableStateOf(false) }
    val label = objects.find { it.id == selected }?.title ?: stringResource(Res.string.financejournal_object_all)
    Box {
        ActionChip(label = label, onClick = { open = true }, trailingIcon = Icons.Default.ArrowDropDown)
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.financejournal_object_all)) },
                onClick = {
                    open = false
                    onChoose(null)
                },
            )
            objects.forEach { obj ->
                DropdownMenuItem(
                    text = { Text(obj.title) },
                    onClick = {
                        open = false
                        onChoose(obj.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun <T> Choices(
    selected: T?,
    choices: List<Pair<T?, StringResource>>,
    onChoose: (T?) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        choices.forEach { (value, label) ->
            FilterChip(
                selected = value == selected,
                onClick = { onChoose(value) },
                label = { Text(stringResource(label)) },
            )
        }
    }
}

private val MONTH_NAMES: List<StringResource> = listOf(
    Res.string.financejournal_month_1,
    Res.string.financejournal_month_2,
    Res.string.financejournal_month_3,
    Res.string.financejournal_month_4,
    Res.string.financejournal_month_5,
    Res.string.financejournal_month_6,
    Res.string.financejournal_month_7,
    Res.string.financejournal_month_8,
    Res.string.financejournal_month_9,
    Res.string.financejournal_month_10,
    Res.string.financejournal_month_11,
    Res.string.financejournal_month_12,
)

private fun monthName(month: Int): StringResource = MONTH_NAMES[month - 1]
