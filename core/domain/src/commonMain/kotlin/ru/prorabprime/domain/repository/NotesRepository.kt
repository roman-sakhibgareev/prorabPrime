package ru.prorabprime.domain.repository

import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import ru.prorabprime.domain.model.Note
import ru.prorabprime.domain.model.NoteDraft
import ru.prorabprime.domain.model.NoteId
import ru.prorabprime.domain.model.ObjectId

/** The log of an object. The observed flow reloads after every write made here. */
interface NotesRepository {
    /** Oldest first. */
    fun observeNotes(objectId: ObjectId): Flow<Result<ImmutableList<Note>>>

    suspend fun add(objectId: ObjectId, draft: NoteDraft): Result<Unit>

    /** Changes the text only; a line keeps the time it was written. */
    suspend fun update(id: NoteId, draft: NoteDraft): Result<Unit>

    suspend fun delete(id: NoteId): Result<Unit>
}
