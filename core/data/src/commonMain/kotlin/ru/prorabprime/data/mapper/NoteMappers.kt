package ru.prorabprime.data.mapper

import kotlin.time.Instant
import ru.prorabprime.contract.NoteDto
import ru.prorabprime.contract.NoteRequestDto
import ru.prorabprime.domain.model.Note
import ru.prorabprime.domain.model.NoteDraft
import ru.prorabprime.domain.model.NoteId

internal fun NoteDto.toDomain() = Note(
    id = NoteId(id),
    text = text,
    // The server sends what it stored; a time it cannot have written reads as the epoch.
    createdAt = runCatching { Instant.parse(createdAt) }.getOrDefault(Instant.fromEpochSeconds(0)),
)

internal fun NoteDraft.toRequestDto() = NoteRequestDto(text = text)
