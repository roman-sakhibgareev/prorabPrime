package ru.prorabprime.domain.usecase

import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import ru.prorabprime.domain.model.AppError
import ru.prorabprime.domain.model.Note
import ru.prorabprime.domain.model.NoteDraft
import ru.prorabprime.domain.model.NoteId
import ru.prorabprime.domain.model.ObjectId
import ru.prorabprime.domain.model.asFailure
import ru.prorabprime.domain.repository.NotesRepository

class ObserveNotesUseCase(
    private val repository: NotesRepository,
) {
    operator fun invoke(objectId: ObjectId): Flow<Result<ImmutableList<Note>>> = repository.observeNotes(objectId)
}

/** Normalizes and validates the draft; an invalid one never reaches the server. */
class SaveNoteUseCase(
    private val repository: NotesRepository,
) {
    suspend fun create(objectId: ObjectId, draft: NoteDraft): Result<Unit> = checked(draft) {
        repository.add(objectId, it)
    }

    suspend fun update(id: NoteId, draft: NoteDraft): Result<Unit> = checked(draft) { repository.update(id, it) }

    private suspend fun checked(draft: NoteDraft, write: suspend (NoteDraft) -> Result<Unit>): Result<Unit> {
        val normalized = draft.normalized()
        val problems = normalized.validate()
        if (problems.isNotEmpty()) return AppError.Validation(problems).asFailure()
        return write(normalized)
    }
}

class DeleteNoteUseCase(
    private val repository: NotesRepository,
) {
    suspend operator fun invoke(id: NoteId): Result<Unit> = repository.delete(id)
}
