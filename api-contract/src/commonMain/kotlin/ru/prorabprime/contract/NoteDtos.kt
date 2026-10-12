package ru.prorabprime.contract

import kotlinx.serialization.Serializable

/** A line of an object's log. [createdAt] is an ISO-8601 instant. */
@Serializable
data class NoteDto(
    val id: String,
    val objectId: String,
    val text: String,
    val createdAt: String,
)

/** Body of `POST /api/objects/{id}/notes` and `PUT /api/notes/{id}`; a PUT changes the text only. */
@Serializable
data class NoteRequestDto(
    val text: String,
    /** Chosen by the client on create (a UUID), so a retry of the same create finds the record it made. */
    val id: String? = null,
    /** When it was written on the phone (ISO-8601 instant), kept on create; the server's time if absent. */
    val createdAt: String? = null,
)

object NoteLimits {
    const val TEXT = 2000
}
