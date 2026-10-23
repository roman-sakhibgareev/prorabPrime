package ru.prorabprime.feature.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import ru.prorabprime.domain.model.Expenses
import ru.prorabprime.domain.model.LocalDay
import ru.prorabprime.domain.model.ObjectQuery
import ru.prorabprime.domain.model.PhotoId
import ru.prorabprime.domain.model.asAppError
import ru.prorabprime.ui.SnackbarNotifier
import ru.prorabprime.ui.StateOwner
import ru.prorabprime.ui.launchCatching
import ru.prorabprime.ui.toUiText

/**
 * Small enough to own its state directly: it reads two flows and keeps the filter and the form, so a
 * separate StateHolder would only forward calls.
 */
internal class ExpensesViewModel(
    private val actions: ExpensesActions,
    private val notifier: SnackbarNotifier,
    private val today: () -> LocalDay,
) : ViewModel(),
    StateOwner<ExpensesState> {

    private val _state = MutableStateFlow(ExpensesState())
    override val state: StateFlow<ExpensesState> = _state.asStateFlow()

    private val form = ExpenseFormHandler(viewModelScope, _state, actions, notifier, today)

    private val selection = ExpensesSelection(viewModelScope, _state, actions, notifier) { expenses }

    /** What was last read, so a new filter can be applied without reading again. */
    private var expenses: Expenses? = null

    init {
        actions.observeExpenses()
            .onEach { result ->
                result
                    .onSuccess {
                        expenses = it
                        _state.update { old -> report(it, old) }
                    }.onFailure { failure ->
                        _state.update { it.copy(status = ExpensesStatus.Error(failure.asAppError().toUiText())) }
                    }
            }.launchIn(viewModelScope)
        actions.observeObjects(ObjectQuery())
            .onEach { result ->
                result.onSuccess { objects ->
                    val choices = objects.map { ObjectChoiceUi(it.id.value, it.title ?: it.address) }
                    _state.update { it.copy(objects = choices.toImmutableList()) }
                }
            }.launchIn(viewModelScope)
    }

    fun onEvent(event: ExpensesEvent) {
        when (event) {
            is ExpensesEvent.FilterSelected -> selectFilter(event.filter)

            is ExpensesEvent.PaymentFilterSelected -> selectPaymentFilter(event.filter)

            is ExpensesEvent.ReceiptPaidChanged -> setPaid(event)

            ExpensesEvent.AddClicked -> form.open()

            is ExpensesEvent.RowLongPressed,
            is ExpensesEvent.RowToggled,
            ExpensesEvent.SelectionClosed,
            ExpensesEvent.ShareClicked,
            ExpensesEvent.SelectionPaidClicked,
            -> selection.onEvent(event)

            is ExpenseFormEvent -> form.onEvent(event)
        }
    }

    private fun selectFilter(filter: ExpenseFilter) = reselect { it.copy(filter = filter) }

    private fun selectPaymentFilter(filter: PaymentStatusFilter) = reselect { it.copy(paymentFilter = filter) }

    private fun reselect(change: (ExpensesState) -> ExpensesState) {
        val read = expenses
        _state.update { old -> change(old).let { if (read == null) it else report(read, it) } }
    }

    /** The local copy changes first, so the row moves between the lists at once; a refusal is reported. */
    private fun setPaid(event: ExpensesEvent.ReceiptPaidChanged) {
        launchCatching(onFailure = { notifier.showError(it.asAppError().toUiText()) }) {
            actions.setReceiptPaid(PhotoId(event.photoId), event.paid)
                .onFailure { notifier.showError(it.asAppError().toUiText()) }
        }
    }

    /** The report with the filters of [old], and what the report does not own — the objects and the form — kept. */
    private fun report(read: Expenses, old: ExpensesState) = read.toState(today(), old.filter, old.paymentFilter)
        .copy(
            objects = old.objects,
            form = old.form,
            selecting = old.selecting,
            selected = old.selected,
            isSharing = old.isSharing,
        ).let(selection::keepVisible)
}
