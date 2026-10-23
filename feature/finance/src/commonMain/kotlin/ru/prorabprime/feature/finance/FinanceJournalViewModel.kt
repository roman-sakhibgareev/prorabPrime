package ru.prorabprime.feature.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.time.Clock
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.prorabprime.domain.model.JournalFilter
import ru.prorabprime.domain.model.LocalDay
import ru.prorabprime.domain.model.PaymentJournal
import ru.prorabprime.domain.model.asAppError
import ru.prorabprime.domain.model.filtered
import ru.prorabprime.domain.model.totals
import ru.prorabprime.ui.StateOwner

internal class FinanceJournalViewModel(
    private val stateHolder: IFinanceJournalStateHolder,
    private val errorHandler: IFinanceErrorHandler,
    private val actions: FinanceJournalActions,
    today: () -> LocalDay = ::journalToday,
) : ViewModel(),
    StateOwner<FinanceJournalState> by stateHolder {

    private val payments = PaymentEditorHandler(
        objectId = null,
        scope = viewModelScope,
        stateHolder = stateHolder,
        errorHandler = errorHandler,
        savePayment = actions.savePayment,
        deletePayment = actions.deletePayment,
        today = today,
    )

    init {
        load()
    }

    fun onEvent(event: FinanceJournalEvent) {
        when (event) {
            FinanceJournalEvent.Retry -> stateHolder.showLoading()
            is FinanceJournalEvent.PeriodChanged -> stateHolder.setFilter { it.copy(period = event.period) }
            is FinanceJournalEvent.ObjectChosen -> stateHolder.setFilter { it.copy(objectId = event.objectId) }
            is FinanceJournalEvent.SideChosen -> stateHolder.setFilter { it.copy(side = event.side) }
            is FinanceJournalEvent.CashChosen -> stateHolder.setFilter { it.copy(cash = event.cash) }
            FinanceJournalEvent.FiltersReset -> stateHolder.setFilter { JournalFilter() }
            is FinanceJournalEvent.RowClicked -> payments.onEvent(PaymentEvent.Edit(event.paymentId))
            is FinanceJournalEvent.Form -> payments.onEvent(event.event)
            FinanceJournalEvent.DialogConfirmed -> runPendingAction()
            FinanceJournalEvent.DialogDismissed -> stateHolder.dismissDialog()
        }
    }

    /** One pipeline for the screen's life: the phone's copy, the filter and the retry counter decide the rest. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun load() {
        state.map { it.attempt }.distinctUntilChanged()
            .flatMapLatest { actions.observeJournal() }
            .combine(state.map { it.filter }.distinctUntilChanged()) { result, filter -> result to filter }
            .onEach { (result, filter) ->
                result
                    .onSuccess { render(it, filter) }
                    .onFailure { viewModelScope.launch { errorHandler.onLoadFailure(it.asAppError()) } }
            }
            .launchIn(viewModelScope)
    }

    private fun render(journal: PaymentJournal, filter: JournalFilter) {
        val shown = journal.filtered(filter)
        stateHolder.showJournal(
            rows = shown.map { it.toRowUi() }.toImmutableList(),
            totals = shown.totals().toUi(),
            allCount = journal.entries.size,
            objects = journal.entries.toObjectChoices(),
        )
    }

    private fun runPendingAction() {
        val action = state.value.pendingAction
        stateHolder.dismissDialog()
        if (action is FinanceAction.DeletePayment) payments.delete(action.paymentId)
    }
}

private fun journalToday(): LocalDay = LocalDay.ofInstant(Clock.System.now())
