package ru.prorabprime.domain.model

import kotlin.time.Instant
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap

/** A line of an object's log, written at [createdAt]. */
data class Note(
    val id: NoteId,
    val text: String,
    val createdAt: Instant,
    /** Made or changed on the phone and not yet accepted by the server. */
    val isPending: Boolean = false,
)

/** A note as the form submits it. */
data class NoteDraft(
    val text: String = "",
) {
    fun normalized(): NoteDraft = copy(text = text.trim())

    fun validate(): ImmutableMap<ObjectField, FieldProblem> {
        val problems = buildMap {
            if (text.isBlank()) put(ObjectField.NOTE_TEXT, FieldProblem.REQUIRED)
            if (text.length > MAX_TEXT) put(ObjectField.NOTE_TEXT, FieldProblem.TOO_LONG)
        }
        return if (problems.isEmpty()) persistentMapOf() else problems.toImmutableMap()
    }

    companion object {
        // Mirrors :api-contract's NoteLimits, which the domain cannot see.
        const val MAX_TEXT = 2000
    }
}
