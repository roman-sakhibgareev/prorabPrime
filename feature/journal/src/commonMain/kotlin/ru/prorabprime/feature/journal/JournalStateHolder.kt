package ru.prorabprime.feature.journal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ru.prorabprime.domain.model.FieldProblem
import ru.prorabprime.ui.DialogModel
import ru.prorabprime.ui.StateOwner
import ru.prorabprime.ui.UiText

internal interface IJournalStateHolder : StateOwner<JournalState> {
    fun showJournal(journal: JournalUi)

    fun showLoading()

    fun showError(message: UiText)

    /** Types into the line at the foot of the screen; a typed letter takes its error mark off. */
    fun setDraft(text: String)

    fun setDraftError(problem: FieldProblem?)

    fun setSending(sending: Boolean)

    /** The line went out: the field is emptied and ready for the next. */
    fun draftSent()

    fun openEditor(editor: NoteEditorUi)

    /** Applies [transform] to the open form; does nothing when none is open. */
    fun editForm(transform: (NoteEditorUi) -> NoteEditorUi)

    fun closeEditor()

    /** Shows [dialog] about the note [pendingId]; both null closes it and forgets what it would have done. */
    fun setDialog(dialog: DialogModel?, pendingId: String? = null)
}

internal class JournalStateHolder : IJournalStateHolder {

    private val _state = MutableStateFlow(JournalState())
    override val state: StateFlow<JournalState> = _state.asStateFlow()

    override fun showJournal(journal: JournalUi) = _state.update {
        it.copy(status = JournalStatus.Content, journal = journal)
    }

    override fun showLoading() = _state.update { it.copy(status = JournalStatus.Loading) }

    override fun showError(message: UiText) = _state.update { it.copy(status = JournalStatus.Error(message)) }

    override fun setDraft(text: String) = _state.update { it.copy(draft = text, draftError = null) }

    override fun setDraftError(problem: FieldProblem?) = _state.update { it.copy(draftError = problem) }

    override fun setSending(sending: Boolean) = _state.update { it.copy(isSending = sending) }

    override fun draftSent() = _state.update { it.copy(draft = "", draftError = null, isSending = false) }

    override fun openEditor(editor: NoteEditorUi) = _state.update { it.copy(editor = editor) }

    override fun editForm(transform: (NoteEditorUi) -> NoteEditorUi) = _state.update { state ->
        state.copy(editor = state.editor?.let(transform))
    }

    override fun closeEditor() = _state.update { it.copy(editor = null) }

    override fun setDialog(dialog: DialogModel?, pendingId: String?) = _state.update {
        it.copy(dialog = dialog, pendingDeleteId = pendingId)
    }
}
