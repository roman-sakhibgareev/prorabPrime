package ru.prorabprime.feature.journal

import com.google.common.truth.Truth.assertThat
import kotlin.time.Instant
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import ru.prorabprime.domain.model.AppError
import ru.prorabprime.domain.model.FieldProblem
import ru.prorabprime.domain.model.Note
import ru.prorabprime.domain.model.NoteDraft
import ru.prorabprime.domain.model.NoteId
import ru.prorabprime.domain.model.ObjectField
import ru.prorabprime.domain.model.ObjectId
import ru.prorabprime.domain.usecase.DeleteNoteUseCase
import ru.prorabprime.domain.usecase.ObserveNotesUseCase
import ru.prorabprime.domain.usecase.SaveNoteUseCase
import ru.prorabprime.testing.FakeNotesRepository
import ru.prorabprime.testing.FakeSnackbarNotifier
import ru.prorabprime.testing.MainDispatcherRule
import ru.prorabprime.ui.toUiText

class JournalViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeNotesRepository()
    private val notifier = FakeSnackbarNotifier()
    private val objectId = ObjectId("o1")
    private val now = Instant.parse("2026-10-13T08:00:00Z")

    private val viewModel by lazy {
        val holder = JournalStateHolder()
        JournalViewModel(
            objectId = objectId,
            stateHolder = holder,
            errorHandler = JournalErrorHandler(holder, notifier),
            actions = JournalActions(
                observeNotes = ObserveNotesUseCase(repository),
                saveNote = SaveNoteUseCase(repository),
                deleteNote = DeleteNoteUseCase(repository),
            ),
            mapper = JournalUiMapper(now = { now }, offsetMinutes = { 0 }),
        )
    }

    private val state get() = viewModel.state.value

    private fun withNotes() {
        repository.notes.value = persistentListOf(
            Note(NoteId("n1"), "Стены оштукатурены", Instant.parse("2026-10-12T17:40:00Z")),
            Note(NoteId("n2"), "Заказать штукатурку", Instant.parse("2026-10-13T07:10:00Z")),
        )
    }

    private fun noteTexts() = state.journal?.days?.flatMap { it.notes }?.map { it.text }

    @Test
    fun `the log is shown grouped by day`() {
        withNotes()

        assertThat(state.status).isEqualTo(JournalStatus.Content)
        assertThat(state.journal?.days?.map { it.heading })
            .containsExactly(DayHeading.Yesterday, DayHeading.Today).inOrder()
        assertThat(noteTexts()).containsExactly("Стены оштукатурены", "Заказать штукатурку").inOrder()
    }

    @Test
    fun `a failed first load takes over the screen and retry loads again`() {
        repository.loadError.value = AppError.Network

        assertThat(state.status).isEqualTo(JournalStatus.Error(AppError.Network.toUiText()))

        repository.loadError.value = null
        viewModel.onEvent(JournalEvent.Retry)

        assertThat(state.status).isEqualTo(JournalStatus.Content)
    }

    @Test
    fun `a gone object replaces the screen even after the log was shown`() {
        withNotes()

        repository.loadError.value = AppError.NotFound

        assertThat(state.status).isInstanceOf(JournalStatus.Error::class.java)
    }

    @Test
    fun `a typed line is sent trimmed and empties the field`() {
        withNotes()

        viewModel.onEvent(JournalEvent.DraftChanged("  Привезли плитку "))
        viewModel.onEvent(JournalEvent.SendClicked)

        assertThat(repository.added).containsExactly(objectId to NoteDraft("Привезли плитку"))
        assertThat(state.draft).isEmpty()
        assertThat(state.isSending).isFalse()
    }

    @Test
    fun `a blank line is not sent and marks the field until it is typed in`() {
        withNotes()
        viewModel.onEvent(JournalEvent.DraftChanged("   "))

        viewModel.onEvent(JournalEvent.SendClicked)

        assertThat(repository.added).isEmpty()
        assertThat(state.draftError).isEqualTo(FieldProblem.REQUIRED)
        assertThat(state.isSending).isFalse()

        viewModel.onEvent(JournalEvent.DraftChanged("Привет"))
        assertThat(state.draftError).isNull()
    }

    @Test
    fun `a failed send keeps the text and says so`() {
        withNotes()
        repository.writeError = AppError.Network
        viewModel.onEvent(JournalEvent.DraftChanged("Привезли плитку"))

        viewModel.onEvent(JournalEvent.SendClicked)

        assertThat(state.draft).isEqualTo("Привезли плитку")
        assertThat(state.isSending).isFalse()
        assertThat(notifier.shown).containsExactly(AppError.Network.toUiText())
    }

    @Test
    fun `a line is opened, changed and saved without touching its time`() {
        withNotes()

        viewModel.onEvent(JournalEvent.NoteClicked("n2"))
        assertThat(state.editor).isEqualTo(NoteEditorUi("n2", "Заказать штукатурку"))
        viewModel.onEvent(JournalEvent.EditorTextChanged("Заказать штукатурку и маяки"))
        viewModel.onEvent(JournalEvent.EditorSaveClicked)

        assertThat(repository.updated).containsExactly(NoteId("n2") to NoteDraft("Заказать штукатурку и маяки"))
        assertThat(state.editor).isNull()
    }

    @Test
    fun `an emptied line stays open with the field marked`() {
        withNotes()
        viewModel.onEvent(JournalEvent.NoteClicked("n1"))
        viewModel.onEvent(JournalEvent.EditorTextChanged(""))

        viewModel.onEvent(JournalEvent.EditorSaveClicked)

        assertThat(repository.updated).isEmpty()
        assertThat(state.editor?.errors).containsExactly(ObjectField.NOTE_TEXT, FieldProblem.REQUIRED)
        assertThat(state.editor?.isSaving).isFalse()
    }

    @Test
    fun `deleting asks first and then deletes, and a dismissed dialog forgets it`() {
        withNotes()
        viewModel.onEvent(JournalEvent.NoteClicked("n1"))

        viewModel.onEvent(JournalEvent.DeleteClicked("n1"))
        assertThat(state.editor).isNull()
        assertThat(state.pendingDeleteId).isEqualTo("n1")
        viewModel.onEvent(JournalEvent.DialogDismissed)
        viewModel.onEvent(JournalEvent.DialogConfirmed)
        assertThat(repository.deleted).isEmpty()

        viewModel.onEvent(JournalEvent.DeleteClicked("n1"))
        viewModel.onEvent(JournalEvent.DialogConfirmed)

        assertThat(repository.deleted).containsExactly(NoteId("n1"))
        assertThat(state.dialog).isNull()
    }
}
