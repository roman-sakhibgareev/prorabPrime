package ru.prorabprime.feature.journal

import ru.prorabprime.domain.usecase.DeleteNoteUseCase
import ru.prorabprime.domain.usecase.ObserveNotesUseCase
import ru.prorabprime.domain.usecase.SaveNoteUseCase

/** The use cases the journal screen calls, in one collaborator to keep the ViewModel's constructor short. */
internal class JournalActions(
    val observeNotes: ObserveNotesUseCase,
    val saveNote: SaveNoteUseCase,
    val deleteNote: DeleteNoteUseCase,
)
