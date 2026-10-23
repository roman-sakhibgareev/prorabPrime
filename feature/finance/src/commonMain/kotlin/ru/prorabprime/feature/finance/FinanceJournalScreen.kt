package ru.prorabprime.feature.finance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.time.Clock
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ru.prorabprime.designsystem.components.ActionChip
import ru.prorabprime.designsystem.components.DialogHost
import ru.prorabprime.designsystem.components.EmptyMessage
import ru.prorabprime.designsystem.components.ErrorMessage
import ru.prorabprime.designsystem.components.FilterChip
import ru.prorabprime.designsystem.components.LoadingBox
import ru.prorabprime.designsystem.components.PendingMark
import ru.prorabprime.designsystem.components.TopAppBar
import ru.prorabprime.designsystem.theme.Spacing
import ru.prorabprime.domain.model.JournalCash
import ru.prorabprime.domain.model.JournalFilter
import ru.prorabprime.domain.model.JournalPeriod
import ru.prorabprime.domain.model.LocalDay
import ru.prorabprime.domain.model.ObjectId
import ru.prorabprime.domain.model.PaymentPurpose
import ru.prorabprime.domain.model.PaymentSide
import ru.prorabprime.feature.finance.resources.Res
import ru.prorabprime.feature.finance.resources.financejournal_back
import ru.prorabprime.feature.finance.resources.financejournal_balance
import ru.prorabprime.feature.finance.resources.financejournal_cash_all
import ru.prorabprime.feature.finance.resources.financejournal_cash_cash
import ru.prorabprime.feature.finance.resources.financejournal_cash_non_cash
import ru.prorabprime.feature.finance.resources.financejournal_empty
import ru.prorabprime.feature.finance.resources.financejournal_expense
import ru.prorabprime.feature.finance.resources.financejournal_income
import ru.prorabprime.feature.finance.resources.financejournal_month_year
import ru.prorabprime.feature.finance.resources.financejournal_next_month
import ru.prorabprime.feature.finance.resources.financejournal_nothing_found
import ru.prorabprime.feature.finance.resources.financejournal_object_all
import ru.prorabprime.feature.finance.resources.financejournal_period_all
import ru.prorabprime.feature.finance.resources.financejournal_period_month
import ru.prorabprime.feature.finance.resources.financejournal_previous_month
import ru.prorabprime.feature.finance.resources.financejournal_reset
import ru.prorabprime.feature.finance.resources.financejournal_side_all
import ru.prorabprime.feature.finance.resources.financejournal_side_expense
import ru.prorabprime.feature.finance.resources.financejournal_side_income
import ru.prorabprime.feature.finance.resources.financejournal_title
import ru.prorabprime.ui.UiText

/** The payments of every object in one list, with filters and the totals of what is shown. */
@Composable
fun FinanceJournalScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: FinanceJournalViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    FinanceJournalContent(state, viewModel::onEvent, onBack, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FinanceJournalContent(
    state: FinanceJournalState,
    onEvent: (FinanceJournalEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDay = LocalDay.ofInstant(Clock.System.now()),
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.financejournal_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.financejournal_back))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (val status = state.status) {
                FinanceJournalStatus.Content ->
                    if (state.allCount == 0) {
                        EmptyMessage(UiText.Resource(Res.string.financejournal_empty))
                    } else {
                        Journal(state, onEvent, today)
                    }

                FinanceJournalStatus.Loading -> LoadingBox()

                is FinanceJournalStatus.Error -> ErrorMessage(
                    status.message,
                    onRetry = { onEvent(FinanceJournalEvent.Retry) },
                )
            }
        }
    }
    FinanceEditorHost(state.editor) { event ->
        if (event is PaymentEvent) onEvent(FinanceJournalEvent.Form(event))
    }
    DialogHost(
        dialog = state.dialog,
        onConfirm = { onEvent(FinanceJournalEvent.DialogConfirmed) },
        onDismiss = { onEvent(FinanceJournalEvent.DialogDismissed) },
    )
}

@Composable
private fun Journal(
    state: FinanceJournalState,
    onEvent: (FinanceJournalEvent) -> Unit,
    today: LocalDay,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        item { Filters(state, onEvent, today) }
        item { Totals(state.totals) }
        if (state.rows.isEmpty()) {
            item { NothingFound(onReset = { onEvent(FinanceJournalEvent.FiltersReset) }) }
        }
        items(state.rows, key = { it.id }) { row ->
            JournalRow(row, onClick = { onEvent(FinanceJournalEvent.RowClicked(row.id)) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun Totals(totals: JournalTotalsUi) {
    Card(Modifier.fillMaxWidth().padding(horizontal = Spacing.m, vertical = Spacing.s)) {
        Row(Modifier.padding(Spacing.m), horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Total(Res.string.financejournal_income, totals.income, incomeColor(), Modifier.weight(1f))
            Total(Res.string.financejournal_expense, totals.expense, expenseColor(), Modifier.weight(1f))
            Total(
                Res.string.financejournal_balance,
                totals.balance,
                if (totals.isDeficit) expenseColor() else MaterialTheme.colorScheme.onSurface,
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Total(
    label: StringResource,
    value: String,
    color: Color,
    modifier: Modifier,
) {
    Column(modifier) {
        Text(
            stringResource(label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.titleSmall, color = color)
    }
}

@Composable
private fun NothingFound(onReset: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(Spacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Text(
            stringResource(Res.string.financejournal_nothing_found),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        androidx.compose.material3.TextButton(onClick = onReset) {
            Text(stringResource(Res.string.financejournal_reset))
        }
    }
}

@Composable
private fun JournalRow(row: JournalRowUi, onClick: () -> Unit) {
    ListItem(
        overlineContent = { Text(row.date) },
        headlineContent = { Text(row.objectTitle) },
        supportingContent = {
            Text(
                listOfNotNull(
                    stringResource(row.payment.method.label),
                    row.payment.purpose.takeIf { row.isIncome && it == PaymentPurpose.EXTRAS }
                        ?.let { stringResource(it.label) },
                    row.payment.note,
                ).joinToString(" · "),
            )
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    row.amount,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (row.isIncome) incomeColor() else expenseColor(),
                )
                if (row.payment.isPending) PendingMark()
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun incomeColor(): Color = MaterialTheme.colorScheme.primary

@Composable
private fun expenseColor(): Color = MaterialTheme.colorScheme.error
