package ru.prorabprime.feature.journal

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import ru.prorabprime.domain.model.FieldProblem
import ru.prorabprime.domain.model.LocalDay
import ru.prorabprime.domain.model.ObjectField
import ru.prorabprime.ui.DialogModel
import ru.prorabprime.ui.UiText

@Immutable
internal sealed interface JournalStatus {
    // Declared most-likely first; every `when` over this mirrors the order.
    data object Content : JournalStatus

    data object Loading : JournalStatus

    data class Error(
        val message: UiText,
    ) : JournalStatus
}

/** One line of the log; [time] is the wall-clock `HH:mm` it was written at. */
@Immutable
internal data class NoteUi(
    val id: String,
    val text: String,
    val time: String,
    val isPending: Boolean = false,
)

/** The lines of one day, under the day's heading. */
@Immutable
internal data class JournalDayUi(
    val day: LocalDay,
    val heading: DayHeading,
    val notes: ImmutableList<NoteUi>,
)

@Immutable
internal sealed interface DayHeading {
    data object Today : DayHeading

    data object Yesterday : DayHeading

    /** `dd.MM.yyyy`. */
    data class Date(
        val text: String,
    ) : DayHeading
}

@Immutable
internal data class JournalUi(
    val days: ImmutableList<JournalDayUi> = persistentListOf(),
) {
    val isEmpty: Boolean get() = days.isEmpty()

    /** The number of lines in all, so the list can tell a new one has come. */
    val count: Int get() = days.sumOf { it.notes.size }
}

/** The form open over the log to change or delete one line. */
@Immutable
internal data class NoteEditorUi(
    val noteId: String,
    val text: String,
    val errors: ImmutableMap<ObjectField, FieldProblem> = persistentMapOf(),
    val isSaving: Boolean = false,
)

@Immutable
internal data class JournalState(
    val status: JournalStatus = JournalStatus.Loading,
    val journal: JournalUi? = null,
    /** The line being typed at the foot of the screen. */
    val draft: String = "",
    val draftError: FieldProblem? = null,
    val isSending: Boolean = false,
    val editor: NoteEditorUi? = null,
    val dialog: DialogModel? = null,
    val pendingDeleteId: String? = null,
)

internal sealed interface JournalEvent {
    data object Retry : JournalEvent

    data class DraftChanged(
        val text: String,
    ) : JournalEvent

    data object SendClicked : JournalEvent

    data class NoteClicked(
        val noteId: String,
    ) : JournalEvent

    data class EditorTextChanged(
        val text: String,
    ) : JournalEvent

    data object EditorSaveClicked : JournalEvent

    data class DeleteClicked(
        val noteId: String,
    ) : JournalEvent

    data object EditorDismissed : JournalEvent

    data object DialogConfirmed : JournalEvent

    data object DialogDismissed : JournalEvent
}
