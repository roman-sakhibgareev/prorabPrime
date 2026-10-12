package ru.prorabprime.feature.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.prorabprime.domain.model.AppError
import ru.prorabprime.domain.model.Note
import ru.prorabprime.domain.model.NoteDraft
import ru.prorabprime.domain.model.NoteId
import ru.prorabprime.domain.model.ObjectField
import ru.prorabprime.domain.model.ObjectId
import ru.prorabprime.domain.model.asAppError
import ru.prorabprime.feature.journal.resources.Res
import ru.prorabprime.feature.journal.resources.journal_delete
import ru.prorabprime.feature.journal.resources.journal_delete_message
import ru.prorabprime.feature.journal.resources.journal_delete_title
import ru.prorabprime.ui.DialogModel
import ru.prorabprime.ui.StateOwner
import ru.prorabprime.ui.UiText
import ru.prorabprime.ui.launchCatching

internal class JournalViewModel(
    private val objectId: ObjectId,
    private val stateHolder: IJournalStateHolder,
    private val errorHandler: IJournalErrorHandler,
    private val actions: JournalActions,
    private val mapper: JournalUiMapper,
) : ViewModel(),
    StateOwner<JournalState> by stateHolder {

    private var loadJob: Job? = null

    init {
        load()
    }

    fun onEvent(event: JournalEvent) {
        when (event) {
            JournalEvent.Retry -> retry()

            is JournalEvent.DraftChanged -> stateHolder.setDraft(event.text)

            JournalEvent.SendClicked -> send()

            is JournalEvent.NoteClicked -> edit(event.noteId)

            is JournalEvent.EditorTextChanged -> stateHolder.editForm {
                it.copy(text = event.text, errors = (it.errors - ObjectField.NOTE_TEXT).toImmutableMap())
            }

            JournalEvent.EditorSaveClicked -> saveEdit()

            is JournalEvent.DeleteClicked -> {
                stateHolder.closeEditor()
                stateHolder.setDialog(DELETE_DIALOG, event.noteId)
            }

            JournalEvent.EditorDismissed -> stateHolder.closeEditor()

            JournalEvent.DialogConfirmed -> deletePending()

            JournalEvent.DialogDismissed -> stateHolder.setDialog(null)
        }
    }

    /** The repository reloads this after any write, so the screen never asks for a refresh. */
    private fun load() {
        loadJob?.cancel()
        loadJob = actions.observeNotes(objectId)
            .onEach(::render)
            .launchIn(viewModelScope)
    }

    private fun retry() {
        stateHolder.showLoading()
        load()
    }

    private fun render(result: Result<ImmutableList<Note>>) {
        result
            .onSuccess { notes -> stateHolder.showJournal(mapper.map(notes)) }
            .onFailure { failure -> viewModelScope.launch { errorHandler.onLoadFailure(failure.asAppError()) } }
    }

    private fun send() {
        val current = state.value
        if (current.isSending) return
        stateHolder.setSending(true)
        launchCatching(onFailure = { onSendFailure(it.asAppError()) }) {
            actions.saveNote.create(objectId, NoteDraft(current.draft))
                .onSuccess { stateHolder.draftSent() }
                .onFailure { onSendFailure(it.asAppError()) }
        }
    }

    private suspend fun onSendFailure(error: AppError) {
        stateHolder.setSending(false)
        val problem = (error as? AppError.Validation)?.fieldErrors?.get(ObjectField.NOTE_TEXT)
        if (error is AppError.Validation) stateHolder.setDraftError(problem) else errorHandler.onActionFailure(error)
    }

    private fun edit(noteId: String) {
        val note = state.value.journal?.days?.flatMap { it.notes }?.find { it.id == noteId } ?: return
        stateHolder.openEditor(NoteEditorUi(note.id, note.text))
    }

    private fun saveEdit() {
        val editor = state.value.editor ?: return
        if (editor.isSaving) return
        stateHolder.editForm { it.copy(isSaving = true) }
        launchCatching(onFailure = { onEditFailure(it.asAppError()) }) {
            actions.saveNote.update(NoteId(editor.noteId), NoteDraft(editor.text))
                .onSuccess { stateHolder.closeEditor() }
                .onFailure { onEditFailure(it.asAppError()) }
        }
    }

    private suspend fun onEditFailure(error: AppError) {
        stateHolder.editForm { it.copy(isSaving = false) }
        if (error is AppError.Validation) {
            stateHolder.editForm { it.copy(errors = error.fieldErrors) }
        } else {
            errorHandler.onActionFailure(error)
        }
    }

    private fun deletePending() {
        val id = state.value.pendingDeleteId
        stateHolder.setDialog(null)
        if (id == null) return
        launchCatching(onFailure = { errorHandler.onActionFailure(it.asAppError()) }) {
            actions.deleteNote(NoteId(id)).onFailure { errorHandler.onActionFailure(it.asAppError()) }
        }
    }

    private companion object {
        val DELETE_DIALOG = DialogModel.Confirmation(
            title = UiText.Resource(Res.string.journal_delete_title),
            message = UiText.Resource(Res.string.journal_delete_message),
            confirmLabel = UiText.Resource(Res.string.journal_delete),
            destructive = true,
        )
    }
}
