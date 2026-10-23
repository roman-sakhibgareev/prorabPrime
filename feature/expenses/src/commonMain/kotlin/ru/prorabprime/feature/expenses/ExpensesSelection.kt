package ru.prorabprime.feature.expenses

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import ru.prorabprime.domain.ShareResult
import ru.prorabprime.domain.model.Expenses
import ru.prorabprime.domain.model.PhotoId
import ru.prorabprime.domain.model.ServerFilePath
import ru.prorabprime.domain.model.asAppError
import ru.prorabprime.domain.usecase.MAX_SHARED_IMAGES
import ru.prorabprime.feature.expenses.resources.Res
import ru.prorabprime.feature.expenses.resources.expenses_select_limit
import ru.prorabprime.feature.expenses.resources.expenses_share_failed
import ru.prorabprime.feature.expenses.resources.expenses_share_nothing
import ru.prorabprime.feature.expenses.resources.expenses_share_unsupported
import ru.prorabprime.ui.SnackbarNotifier
import ru.prorabprime.ui.UiText
import ru.prorabprime.ui.launchCatching
import ru.prorabprime.ui.toUiText

/** Picking receipts in the list to send their pictures, or to mark them paid, all at once. */
internal class ExpensesSelection(
    private val scope: CoroutineScope,
    private val state: MutableStateFlow<ExpensesState>,
    private val actions: ExpensesActions,
    private val notifier: SnackbarNotifier,
    /** What was last read: the rows carry no file paths, so the pictures to send come from here. */
    private val expenses: () -> Expenses?,
) {
    fun onEvent(event: ExpensesEvent) {
        when (event) {
            is ExpensesEvent.RowLongPressed -> if (state.value.selecting) toggle(event.id) else start(event.id)
            ExpensesEvent.SelectionStarted -> begin()
            is ExpensesEvent.RowToggled -> toggle(event.id)
            ExpensesEvent.SelectionClosed -> close()
            ExpensesEvent.ShareClicked -> share()
            ExpensesEvent.SelectionPaidClicked -> markPaid()
            else -> Unit
        }
    }

    /** After a new report or a new filter: what is no longer in the list cannot stay picked. */
    fun keepVisible(new: ExpensesState): ExpensesState {
        if (!new.selecting) return new
        val visible = new.rows.filter { it.isSelectable }.map { it.id }.toSet()
        return new.copy(selected = new.selected.filter { it in visible }.toImmutableSet())
    }

    private fun begin() {
        if (state.value.rows.none { it.isSelectable }) return
        state.update { it.copy(selecting = true, selected = persistentSetOf()) }
    }

    private fun start(id: String) {
        if (!selectable(id)) return
        state.update { it.copy(selecting = true, selected = persistentSetOf(id)) }
    }

    private fun toggle(id: String) {
        val current = state.value
        if (!current.selecting || !selectable(id)) return
        if (id in current.selected) {
            state.update { it.copy(selected = (it.selected - id).toImmutableSet()) }
        } else if (current.selected.size >= MAX_SHARED_IMAGES) {
            scope.launchCatching(TAG, onFailure = { }) {
                notifier.showError(
                    UiText.Resource(Res.string.expenses_select_limit, persistentListOf(MAX_SHARED_IMAGES)),
                )
            }
        } else {
            state.update { it.copy(selected = (it.selected + id).toImmutableSet()) }
        }
    }

    private fun close() = state.update { it.copy(selecting = false, selected = persistentSetOf()) }

    private fun selectable(id: String) = state.value.rows.any { it.id == id && it.isSelectable }

    private fun share() {
        val current = state.value
        if (current.isSharing || current.selected.isEmpty()) return
        val paths = pathsOf(current.selected)
        state.update { it.copy(isSharing = true) }
        scope.launchCatching(TAG, onFailure = { failed(it) }) {
            val result = actions.shareImages(paths)
            state.update { it.copy(isSharing = false) }
            when (result) {
                is ShareResult.Shared -> close()

                ShareResult.NothingToShare -> notifier.showError(UiText.Resource(Res.string.expenses_share_nothing))

                is ShareResult.TooMany -> notifier.showError(
                    UiText.Resource(Res.string.expenses_select_limit, persistentListOf(result.limit)),
                )

                ShareResult.Unsupported -> notifier.showError(UiText.Resource(Res.string.expenses_share_unsupported))

                ShareResult.Failed -> notifier.showError(UiText.Resource(Res.string.expenses_share_failed))
            }
        }
    }

    private suspend fun failed(error: Throwable) {
        state.update { it.copy(isSharing = false) }
        notifier.showError(error.asAppError().toUiText())
    }

    /** In the order the list shows them, which is the order they arrive in the messenger. */
    private fun pathsOf(ids: Set<String>): List<ServerFilePath> {
        val byId = expenses()?.items.orEmpty().associateBy { it.id }
        return state.value.rows.filter { it.id in ids }.mapNotNull { byId[it.id]?.photoPath }
    }

    /** All of them paid, unless they already are, in which case all of them unpaid. */
    private fun markPaid() {
        val rows = state.value.let { s -> s.rows.filter { it.id in s.selected } }
        val photoIds = rows.mapNotNull { it.photoId }
        if (photoIds.isEmpty()) return
        val paid = rows.any { it.isUnpaid }
        scope.launchCatching(TAG, onFailure = { notifier.showError(it.asAppError().toUiText()) }) {
            val failure = photoIds.firstNotNullOfOrNull { actions.setReceiptPaid(PhotoId(it), paid).exceptionOrNull() }
            if (failure == null) close() else notifier.showError(failure.asAppError().toUiText())
        }
    }

    private companion object {
        const val TAG = "ExpensesSelection"
    }
}
