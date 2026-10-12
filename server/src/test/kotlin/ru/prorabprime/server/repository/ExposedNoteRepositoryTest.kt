package ru.prorabprime.server.repository

import com.google.common.truth.Truth.assertThat
import com.zaxxer.hikari.HikariDataSource
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.After
import org.junit.Before
import org.junit.Test
import ru.prorabprime.contract.ObjectStatusDto
import ru.prorabprime.server.OTHER_OWNER
import ru.prorabprime.server.TEST_OWNER
import ru.prorabprime.server.db.DbExecutor
import ru.prorabprime.server.db.TestPostgres
import ru.prorabprime.server.model.NoteRecord
import ru.prorabprime.server.model.ObjectFields
import ru.prorabprime.server.model.ObjectRecord

class ExposedNoteRepositoryTest {

    private lateinit var dataSource: HikariDataSource
    private lateinit var objects: ExposedObjectRepository
    private lateinit var notes: ExposedNoteRepository

    private val base = Instant.parse("2026-09-25T10:00:00Z")
    private val objectId = UUID.randomUUID()

    @Before
    fun setUp() = runTest {
        TestPostgres.assumeAvailable()
        dataSource = TestPostgres.freshDataSource()
        val db = DbExecutor(Database.connect(dataSource), Dispatchers.IO)
        objects = ExposedObjectRepository(db)
        notes = ExposedNoteRepository(db)
        objects.insert(
            ObjectRecord(
                id = objectId,
                ownerId = TEST_OWNER,
                fields = ObjectFields(null, "Тверская, 5", ObjectStatusDto.IN_PROGRESS, null, null, null),
                coverPhotoId = null,
                createdAt = base,
                updatedAt = base,
            ),
        )
    }

    @After
    fun tearDown() {
        if (::dataSource.isInitialized) dataSource.close()
    }

    private fun note(minutesLater: Int, text: String = "Стены оштукатурены") =
        NoteRecord(UUID.randomUUID(), objectId, text, base + minutesLater.minutes)

    @Test
    fun `notes read back unchanged and list oldest first`() = runTest {
        val later = note(10, "позже")
        val earlier = note(1, "раньше")
        notes.insert(later)
        notes.insert(earlier)

        assertThat(notes.find(TEST_OWNER, earlier.id)).isEqualTo(earlier)
        assertThat(notes.listByObject(objectId)).containsExactly(earlier, later).inOrder()
    }

    @Test
    fun `updating and deleting report whether there was a note`() = runTest {
        val note = note(1)
        notes.insert(note)

        assertThat(notes.updateText(TEST_OWNER, note.id, "Маяки поставлены")).isTrue()
        assertThat(notes.find(TEST_OWNER, note.id)).isEqualTo(note.copy(text = "Маяки поставлены"))
        assertThat(notes.updateText(TEST_OWNER, UUID.randomUUID(), "x")).isFalse()
        assertThat(notes.delete(TEST_OWNER, note.id)).isTrue()
        assertThat(notes.delete(TEST_OWNER, note.id)).isFalse()
    }

    @Test
    fun `another account's note is not found, not changed and not deleted`() = runTest {
        val note = note(1)
        notes.insert(note)

        assertThat(notes.find(OTHER_OWNER, note.id)).isNull()
        assertThat(notes.updateText(OTHER_OWNER, note.id, "чужое")).isFalse()
        assertThat(notes.delete(OTHER_OWNER, note.id)).isFalse()
        assertThat(notes.find(TEST_OWNER, note.id)).isEqualTo(note)
    }

    @Test
    fun `the object takes its notes with it`() = runTest {
        notes.insert(note(1))

        objects.delete(TEST_OWNER, objectId)

        assertThat(notes.listByObject(objectId)).isEmpty()
    }
}
