package ru.prorabprime.server.model

import java.util.UUID
import kotlin.time.Instant

data class NoteRecord(
    val id: UUID,
    val objectId: UUID,
    val text: String,
    val createdAt: Instant,
)
