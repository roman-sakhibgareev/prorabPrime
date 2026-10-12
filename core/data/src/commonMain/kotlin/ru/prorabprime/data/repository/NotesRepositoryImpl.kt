package ru.prorabprime.data.repository

import kotlin.time.Clock
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import ru.prorabprime.contract.NoteDto
import ru.prorabprime.data.local.IdFactory
import ru.prorabprime.data.local.Keys
import ru.prorabprime.data.local.LocalDb
import ru.prorabprime.data.local.NoteRow
import ru.prorabprime.data.local.Operation
import ru.prorabprime.data.local.forgetOrQueueDelete
import ru.prorabprime.data.mapper.toDomain
import ru.prorabprime.data.mapper.toRequestDto
import ru.prorabprime.domain.model.AppError
import ru.prorabprime.domain.model.Note
import ru.prorabprime.domain.model.NoteDraft
import ru.prorabprime.domain.model.NoteId
import ru.prorabprime.domain.model.ObjectId
import ru.prorabprime.domain.model.asFailure
import ru.prorabprime.domain.repository.NotesRepository

/** The log of an object, kept on the phone and written there first, with the time it was written on the phone. */
internal class NotesRepositoryImpl(
    private val db: LocalDb,
    private val ids: IdFactory,
    private val clock: Clock,
) : NotesRepository {

    override fun observeNotes(objectId: ObjectId): Flow<Result<ImmutableList<Note>>> = db.changes.transform {
        val id = objectId.value
        when {
            db.objects.rows.value.containsKey(id) -> emit(Result.success(notes(id)))
            db.metaRow.syncTried -> emit(AppError.NotFound.asFailure())
        }
    }

    private fun notes(objectId: String): ImmutableList<Note> {
        val dirty = db.outbox.dirtyKeys()
        return db.notes.rows.value.values.filter { it.objectId == objectId }
            .sortedWith(compareBy({ it.dto.createdAt }, { it.dto.id }))
            .map { it.dto.toDomain().copy(isPending = Keys.note(it.dto.id) in dirty) }
            .toImmutableList()
    }

    override suspend fun add(objectId: ObjectId, draft: NoteDraft): Result<Unit> {
        if (!db.objects.rows.value.containsKey(objectId.value)) return AppError.NotFound.asFailure()
        val id = ids.next()
        val writtenAt = clock.now().toString()
        val request = draft.toRequestDto().copy(id = id, createdAt = writtenAt)
        db.outbox.enqueue(Operation.CreateNote(objectId.value, request))
        db.notes.upsert(NoteRow(objectId.value, NoteDto(id, objectId.value, request.text, writtenAt)))
        return Result.success(Unit)
    }

    override suspend fun update(id: NoteId, draft: NoteDraft): Result<Unit> {
        val row = db.notes.rows.value[id.value] ?: return AppError.NotFound.asFailure()
        val request = draft.toRequestDto()
        db.outbox.enqueue(Operation.UpdateNote(id.value, request))
        db.notes.upsert(row.copy(dto = row.dto.copy(text = request.text)))
        return Result.success(Unit)
    }

    override suspend fun delete(id: NoteId): Result<Unit> {
        if (!db.notes.rows.value.containsKey(id.value)) return Result.success(Unit)
        db.forgetOrQueueDelete(Keys.note(id.value), Operation.DeleteNote(id.value))
        db.notes.remove(id.value)
        return Result.success(Unit)
    }
}
