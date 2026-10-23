package ru.prorabprime.feature.finance

import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ru.prorabprime.domain.model.JournalFilter
import ru.prorabprime.domain.model.Payment
import ru.prorabprime.ui.DialogModel
import ru.prorabprime.ui.StateOwner
import ru.prorabprime.ui.UiText

internal interface IFinanceJournalStateHolder :
    StateOwner<FinanceJournalState>,
    PaymentFormHost {
    fun showJournal(
        rows: ImmutableList<JournalRowUi>,
        totals: JournalTotalsUi,
        allCount: Int,
        objects: ImmutableList<JournalObjectUi>,
    )

    /** Asks the journal again: the screen shows the spinner and the attempt counter moves. */
    fun showLoading()

    fun showError(message: UiText)

    fun setFilter(transform: (JournalFilter) -> JournalFilter)

    /** Closes the dialog and forgets what it would have done. */
    fun dismissDialog()
}

internal class FinanceJournalStateHolder : IFinanceJournalStateHolder {

    private val _state = MutableStateFlow(FinanceJournalState())
    override val state: StateFlow<FinanceJournalState> = _state.asStateFlow()

    override val editor: FinanceEditorUi? get() = _state.value.editor

    override fun payment(id: String): Payment? = _state.value.rows.find { it.id == id }?.payment

    override fun showJournal(
        rows: ImmutableList<JournalRowUi>,
        totals: JournalTotalsUi,
        allCount: Int,
        objects: ImmutableList<JournalObjectUi>,
    ) = _state.update {
        it.copy(
            status = FinanceJournalStatus.Content,
            rows = rows,
            totals = totals,
            allCount = allCount,
            objects = objects,
        )
    }

    override fun showLoading() = _state.update {
        it.copy(status = FinanceJournalStatus.Loading, attempt = it.attempt + 1)
    }

    override fun showError(message: UiText) = _state.update { it.copy(status = FinanceJournalStatus.Error(message)) }

    override fun setFilter(transform: (JournalFilter) -> JournalFilter) = _state.update {
        it.copy(filter = transform(it.filter))
    }

    override fun openEditor(editor: FinanceEditorUi) = _state.update { it.copy(editor = editor) }

    override fun editForm(transform: (FinanceEditorUi) -> FinanceEditorUi) = _state.update { state ->
        state.copy(editor = state.editor?.let(transform))
    }

    override fun closeEditor() = _state.update { it.copy(editor = null) }

    override fun askToConfirm(dialog: DialogModel, action: FinanceAction) = _state.update {
        it.copy(dialog = dialog, pendingAction = action)
    }

    override fun dismissDialog() = _state.update { it.copy(dialog = null, pendingAction = null) }
}
