package ru.prorabprime.server.service

import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Instant
import ru.prorabprime.contract.FieldErrorDto
import ru.prorabprime.contract.FieldProblemDto
import ru.prorabprime.contract.NoteLimits
import ru.prorabprime.contract.NoteRequestDto
import ru.prorabprime.contract.ObjectFieldDto
import ru.prorabprime.server.error.ServiceError
import ru.prorabprime.server.error.asFailure
import ru.prorabprime.server.model.NoteRecord
import ru.prorabprime.server.model.OwnerId
import ru.prorabprime.server.repository.NoteRepository
import ru.prorabprime.server.repository.ObjectRepository

/** Trims the text and checks it against the column. */
fun validateNote(request: NoteRequestDto): Result<String> {
    val text = request.text.trim()
    val errors = buildList {
        if (text.isEmpty()) add(FieldErrorDto(ObjectFieldDto.NOTE_TEXT, FieldProblemDto.REQUIRED))
        if (text.length > NoteLimits.TEXT) add(FieldErrorDto(ObjectFieldDto.NOTE_TEXT, FieldProblemDto.TOO_LONG))
    }
    return if (errors.isEmpty()) Result.success(text) else ServiceError.Validation("Invalid note", errors).asFailure()
}

/** The log of an object. A change to it is a change to the object. */
class NoteService(
    private val objects: ObjectRepository,
    private val notes: NoteRepository,
    private val clock: Clock,
    private val newId: () -> UUID = UUID::randomUUID,
) {
    suspend fun list(owner: OwnerId, objectId: UUID): Result<List<NoteRecord>> {
        if (objects.find(owner, objectId) == null) return objectNotFound(objectId)
        return Result.success(notes.listByObject(objectId))
    }

    suspend fun create(
        owner: OwnerId,
        objectId: UUID,
        request: NoteRequestDto,
    ): Result<NoteRecord> {
        val text = validateNote(request).getOrElse { return Result.failure(it) }
        if (objects.find(owner, objectId) == null) return objectNotFound(objectId)
        val clientId = parseClientId(request.id).getOrElse { return Result.failure(it) }
        val now = clock.now()
        val writtenAt = parseWrittenAt(request.createdAt, now).getOrElse { return Result.failure(it) }
        alreadyCreated(clientId?.let { notes.find(owner, it) }) { it.objectId == objectId }?.let { return it }
        val record = NoteRecord(clientId ?: newId(), objectId, text, writtenAt)
        notes.insert(record)
        objects.touch(owner, objectId, now)
        return Result.success(record)
    }

    suspend fun update(
        owner: OwnerId,
        id: UUID,
        request: NoteRequestDto,
    ): Result<Unit> {
        val text = validateNote(request).getOrElse { return Result.failure(it) }
        val existing = notes.find(owner, id) ?: return notFound(id)
        notes.updateText(owner, id, text)
        objects.touch(owner, existing.objectId, clock.now())
        return Result.success(Unit)
    }

    suspend fun delete(owner: OwnerId, id: UUID): Result<Unit> {
        val existing = notes.find(owner, id) ?: return notFound(id)
        notes.delete(owner, id)
        objects.touch(owner, existing.objectId, clock.now())
        return Result.success(Unit)
    }

    /** The phone's own time for the line, but never one from the future. */
    private fun parseWrittenAt(raw: String?, now: Instant): Result<Instant> {
        if (raw.isNullOrBlank()) return Result.success(now)
        val parsed = runCatching { Instant.parse(raw.trim()) }.getOrNull()
            ?: return ServiceError.Validation("createdAt is not an instant").asFailure()
        return Result.success(minOf(parsed, now))
    }

    private fun <T> objectNotFound(id: UUID): Result<T> = ServiceError.NotFound("No object $id").asFailure()

    private fun notFound(id: UUID): Result<Unit> = ServiceError.NotFound("No note $id").asFailure()
}
