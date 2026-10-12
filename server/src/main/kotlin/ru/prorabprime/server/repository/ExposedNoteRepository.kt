package ru.prorabprime.server.repository

import java.util.UUID
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import ru.prorabprime.server.db.DbExecutor
import ru.prorabprime.server.model.NoteRecord
import ru.prorabprime.server.model.OwnerId

class ExposedNoteRepository(
    private val db: DbExecutor,
) : NoteRepository {

    override suspend fun listByObject(objectId: UUID): List<NoteRecord> = db.query {
        NotesTable.selectAll()
            .where { NotesTable.objectId eq objectId }
            .orderBy(NotesTable.createdAt to SortOrder.ASC, NotesTable.id to SortOrder.ASC)
            .map { it.toNoteRecord() }
    }

    /** The one note [id] names, if its object belongs to [owner]. */
    private fun mine(owner: OwnerId, id: UUID): Op<Boolean> =
        (NotesTable.id eq id) and NotesTable.objectId.ownedBy(owner)

    override suspend fun find(owner: OwnerId, id: UUID): NoteRecord? = db.query {
        NotesTable.selectAll().where { mine(owner, id) }.singleOrNull()?.toNoteRecord()
    }

    override suspend fun insert(note: NoteRecord) {
        db.query {
            NotesTable.insert {
                it[id] = note.id
                it[objectId] = note.objectId
                it[text] = note.text
                it[createdAt] = note.createdAt.toJavaInstant()
            }
        }
    }

    override suspend fun updateText(
        owner: OwnerId,
        id: UUID,
        text: String,
    ): Boolean = db.query {
        NotesTable.update({ mine(owner, id) }) { it[NotesTable.text] = text } > 0
    }

    override suspend fun delete(owner: OwnerId, id: UUID): Boolean = db.query {
        NotesTable.deleteWhere { mine(owner, id) } > 0
    }
}

private fun ResultRow.toNoteRecord() = NoteRecord(
    id = this[NotesTable.id],
    objectId = this[NotesTable.objectId],
    text = this[NotesTable.text],
    createdAt = this[NotesTable.createdAt].toKotlinInstant(),
)
