package ru.prorabprime.server.service

import com.google.common.truth.Truth.assertThat
import java.util.UUID
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.test.runTest
import org.junit.Test
import ru.prorabprime.contract.FieldProblemDto
import ru.prorabprime.contract.NoteLimits
import ru.prorabprime.contract.NoteRequestDto
import ru.prorabprime.contract.ObjectFieldDto
import ru.prorabprime.contract.ObjectRequestDto
import ru.prorabprime.contract.ObjectStatusDto
import ru.prorabprime.server.TEST_OWNER
import ru.prorabprime.server.error.ServiceError
import ru.prorabprime.server.error.ServiceException
import ru.prorabprime.server.fakes.FIXED_NOW
import ru.prorabprime.server.fakes.FakeContactRepository
import ru.prorabprime.server.fakes.FakeNoteRepository
import ru.prorabprime.server.fakes.FakeObjectRepository
import ru.prorabprime.server.fakes.FakePhotoRepository
import ru.prorabprime.server.fakes.FixedClock

class NoteServiceTest {

    private val objects = FakeObjectRepository(FakePhotoRepository())
    private val repository = FakeNoteRepository()
    private val clock = FixedClock()
    private val service = NoteService(objects, repository, clock)

    private suspend fun anObject(): UUID = ObjectService(objects, FakePhotoRepository(), FakeContactRepository(), clock)
        .create(TEST_OWNER, ObjectRequestDto(address = "Тверская, 5", status = ObjectStatusDto.IN_PROGRESS))
        .getOrThrow().id

    private fun Result<*>.error() = (exceptionOrNull() as? ServiceException)?.error

    @Test
    fun `a note is trimmed and stamped with the server's time when the phone sent none`() = runTest {
        val id = anObject()

        val note = service.create(TEST_OWNER, id, NoteRequestDto("  Стены оштукатурены ")).getOrThrow()

        assertThat(note.text).isEqualTo("Стены оштукатурены")
        assertThat(note.createdAt).isEqualTo(FIXED_NOW)
        assertThat(note.objectId).isEqualTo(id)
    }

    @Test
    fun `the time the phone wrote it is kept, but never one from the future`() = runTest {
        val id = anObject()
        val earlier = FIXED_NOW - 3.hours

        val past = service.create(TEST_OWNER, id, NoteRequestDto("Раньше", createdAt = earlier.toString())).getOrThrow()
        val future = service
            .create(TEST_OWNER, id, NoteRequestDto("Позже", createdAt = (FIXED_NOW + 5.hours).toString()))
            .getOrThrow()

        assertThat(past.createdAt).isEqualTo(earlier)
        assertThat(future.createdAt).isEqualTo(FIXED_NOW)
        assertThat(service.create(TEST_OWNER, id, NoteRequestDto("x", createdAt = "вчера")).error())
            .isInstanceOf(ServiceError.Validation::class.java)
    }

    @Test
    fun `a note needs text that fits the column`() = runTest {
        val id = anObject()

        val empty = service.create(TEST_OWNER, id, NoteRequestDto("  ")).error() as ServiceError.Validation
        val long = service.create(TEST_OWNER, id, NoteRequestDto("я".repeat(NoteLimits.TEXT + 1)))
            .error() as ServiceError.Validation

        assertThat(empty.fieldErrors.map { it.field to it.problem })
            .containsExactly(ObjectFieldDto.NOTE_TEXT to FieldProblemDto.REQUIRED)
        assertThat(long.fieldErrors.map { it.field to it.problem })
            .containsExactly(ObjectFieldDto.NOTE_TEXT to FieldProblemDto.TOO_LONG)
        assertThat(repository.records).isEmpty()
    }

    @Test
    fun `the log is listed oldest first`() = runTest {
        val id = anObject()
        service.create(TEST_OWNER, id, NoteRequestDto("второе", createdAt = (FIXED_NOW - 1.hours).toString()))
        service.create(TEST_OWNER, id, NoteRequestDto("первое", createdAt = (FIXED_NOW - 2.hours).toString()))

        val log = service.list(TEST_OWNER, id).getOrThrow()

        assertThat(log.map { it.text }).containsExactly("первое", "второе").inOrder()
    }

    @Test
    fun `a create sent again under the same id finds the note it made`() = runTest {
        val id = anObject()
        val noteId = UUID.randomUUID().toString()

        val first = service.create(TEST_OWNER, id, NoteRequestDto("Раз", id = noteId)).getOrThrow()
        val again = service.create(TEST_OWNER, id, NoteRequestDto("Раз", id = noteId)).getOrThrow()

        assertThat(again).isEqualTo(first)
        assertThat(repository.records).hasSize(1)
    }

    @Test
    fun `a note changes its text only, and is deleted`() = runTest {
        val id = anObject()
        val note = service.create(TEST_OWNER, id, NoteRequestDto("Было")).getOrThrow()

        service.update(TEST_OWNER, note.id, NoteRequestDto("Стало")).getOrThrow()
        assertThat(repository.records.getValue(note.id).text).isEqualTo("Стало")
        assertThat(repository.records.getValue(note.id).createdAt).isEqualTo(note.createdAt)

        service.delete(TEST_OWNER, note.id).getOrThrow()
        assertThat(repository.records).isEmpty()
    }

    @Test
    fun `an unknown object or note is not found`() = runTest {
        val missing = UUID.randomUUID()

        assertThat(service.list(TEST_OWNER, missing).error()).isInstanceOf(ServiceError.NotFound::class.java)
        assertThat(service.create(TEST_OWNER, missing, NoteRequestDto("x")).error())
            .isInstanceOf(ServiceError.NotFound::class.java)
        assertThat(service.update(TEST_OWNER, missing, NoteRequestDto("x")).error())
            .isInstanceOf(ServiceError.NotFound::class.java)
        assertThat(service.delete(TEST_OWNER, missing).error()).isInstanceOf(ServiceError.NotFound::class.java)
    }
}
