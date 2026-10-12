package ru.prorabprime.testing

import kotlin.time.Instant
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import ru.prorabprime.domain.model.AppError
import ru.prorabprime.domain.model.Note
import ru.prorabprime.domain.model.NoteDraft
import ru.prorabprime.domain.model.NoteId
import ru.prorabprime.domain.model.ObjectId
import ru.prorabprime.domain.model.asFailure
import ru.prorabprime.domain.repository.NotesRepository

/** In-memory [NotesRepository]: serves [notes] as the log of any object and records every write. */
class FakeNotesRepository : NotesRepository {

    val notes = MutableStateFlow<List<Note>>(emptyList())

    /** When set, the observed flow emits this failure instead of data. */
    val loadError = MutableStateFlow<AppError?>(null)

    /** When set, every write fails with it instead of writing. */
    var writeError: AppError? = null

    val added = mutableListOf<Pair<ObjectId, NoteDraft>>()
    val updated = mutableListOf<Pair<NoteId, NoteDraft>>()
    val deleted = mutableListOf<NoteId>()

    override fun observeNotes(objectId: ObjectId): Flow<Result<ImmutableList<Note>>> =
        combine(notes, loadError) { all, error ->
            error?.asFailure() ?: Result.success(all.toImmutableList())
        }

    override suspend fun add(objectId: ObjectId, draft: NoteDraft) = write { added += objectId to draft }

    override suspend fun update(id: NoteId, draft: NoteDraft) = write { updated += id to draft }

    override suspend fun delete(id: NoteId) = write { deleted += id }

    private fun write(block: () -> Unit): Result<Unit> {
        writeError?.let { return it.asFailure() }
        block()
        return Result.success(Unit)
    }
}

val TEST_NOTE_TIME: Instant = Instant.parse("2026-09-25T10:30:00Z")

fun aNote(
    id: String = "n1",
    text: String = "Стены оштукатурены",
    createdAt: Instant = TEST_NOTE_TIME,
) = Note(NoteId(id), text, createdAt)

fun noNotes(): ImmutableList<Note> = persistentListOf()
