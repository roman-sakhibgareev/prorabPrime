package ru.prorabprime.data.repository

import com.google.common.truth.Truth.assertThat
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.prorabprime.data.local.Operation
import ru.prorabprime.domain.model.AppError
import ru.prorabprime.domain.model.NoteDraft
import ru.prorabprime.domain.model.NoteId
import ru.prorabprime.domain.model.ObjectId
import ru.prorabprime.domain.model.asAppError

class NotesRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val phone by lazy { OfflineFixture(folder.root).also { runTest { it.db.load() } } }

    private val objectId get() = ObjectId(phone.server.objectId)

    private suspend fun offlineAndSynced(): OfflineFixture = phone.also {
        it.engine.sync()
        it.server.offline = true
    }

    private suspend fun log() = phone.notes.observeNotes(objectId).first().getOrThrow()

    @Test
    fun `the log comes down from the server with the time each line was written`() = runTest {
        offlineAndSynced()

        val line = log().single()

        assertThat(line.text).isEqualTo("Маяки поставлены")
        assertThat(line.createdAt).isEqualTo(Instant.parse("2026-10-01T09:00:00Z"))
        assertThat(line.isPending).isFalse()
    }

    @Test
    fun `a line written offline is shown at once, stamped with the phone's time, and queued`() = runTest {
        offlineAndSynced()

        phone.notes.add(objectId, NoteDraft("Стены оштукатурены")).getOrThrow()

        val written = log().last()
        assertThat(written.text).isEqualTo("Стены оштукатурены")
        assertThat(written.createdAt).isEqualTo(phone.server.at)
        assertThat(written.isPending).isTrue()
        val queued = phone.db.outbox.snapshot().map { it.operation }.single() as Operation.CreateNote
        assertThat(queued.objectId).isEqualTo(objectId.value)
        assertThat(queued.request.createdAt).isEqualTo(phone.server.at.toString())
        assertThat(queued.request.id).isEqualTo(written.id.value)
    }

    @Test
    fun `the log is read oldest first`() = runTest {
        offlineAndSynced()
        phone.notes.add(objectId, NoteDraft("новая")).getOrThrow()

        assertThat(log().map { it.text }).containsExactly("Маяки поставлены", "новая").inOrder()
    }

    @Test
    fun `editing changes the text and keeps the time`() = runTest {
        offlineAndSynced()
        val before = log().single()

        phone.notes.update(before.id, NoteDraft("Маяки поставлены и проверены")).getOrThrow()

        val after = log().single()
        assertThat(after.text).isEqualTo("Маяки поставлены и проверены")
        assertThat(after.createdAt).isEqualTo(before.createdAt)
        assertThat(after.isPending).isTrue()
    }

    @Test
    fun `a line that never left the phone is forgotten when deleted, and a sent one is queued for deletion`() =
        runTest {
            offlineAndSynced()
            phone.notes.add(objectId, NoteDraft("Передумал")).getOrThrow()
            val forgotten = log().last().id

            phone.notes.delete(forgotten).getOrThrow()
            phone.notes.delete(NoteId("n1")).getOrThrow()

            assertThat(log()).isEmpty()
            assertThat(phone.db.outbox.snapshot().map { it.operation }).containsExactly(Operation.DeleteNote("n1"))
        }

    @Test
    fun `an unknown object has no log, and a line cannot be added to it`() = runTest {
        offlineAndSynced()
        val missing = ObjectId("no-such-object")

        val added = phone.notes.add(missing, NoteDraft("x"))
        val edited = phone.notes.update(NoteId("no-such-note"), NoteDraft("x"))

        assertThat(added.exceptionOrNull()?.asAppError()).isEqualTo(AppError.NotFound)
        assertThat(edited.exceptionOrNull()?.asAppError()).isEqualTo(AppError.NotFound)
    }
}
