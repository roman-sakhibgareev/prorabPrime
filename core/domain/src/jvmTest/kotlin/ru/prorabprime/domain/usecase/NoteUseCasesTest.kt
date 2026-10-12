package ru.prorabprime.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import ru.prorabprime.domain.model.AppError
import ru.prorabprime.domain.model.FieldProblem
import ru.prorabprime.domain.model.NoteDraft
import ru.prorabprime.domain.model.NoteId
import ru.prorabprime.domain.model.ObjectField
import ru.prorabprime.domain.model.ObjectId
import ru.prorabprime.domain.model.asAppError
import ru.prorabprime.testing.FakeNotesRepository

class NoteUseCasesTest {

    private val repository = FakeNotesRepository()
    private val save = SaveNoteUseCase(repository)
    private val objectId = ObjectId("o1")

    private fun Result<*>.problems() = (exceptionOrNull()?.asAppError() as? AppError.Validation)?.fieldErrors

    @Test
    fun `a note is trimmed before it is written`() = runTest {
        assertThat(save.create(objectId, NoteDraft("  Маяки поставлены ")).isSuccess).isTrue()
        assertThat(save.update(NoteId("n"), NoteDraft(" Готово ")).isSuccess).isTrue()

        assertThat(repository.added).containsExactly(objectId to NoteDraft("Маяки поставлены"))
        assertThat(repository.updated).containsExactly(NoteId("n") to NoteDraft("Готово"))
    }

    @Test
    fun `a note needs text that fits, and a bad one never reaches the repository`() = runTest {
        assertThat(save.create(objectId, NoteDraft("  ")).problems())
            .isEqualTo(persistentMapOf(ObjectField.NOTE_TEXT to FieldProblem.REQUIRED))
        assertThat(save.update(NoteId("n"), NoteDraft("я".repeat(NoteDraft.MAX_TEXT + 1))).problems())
            .isEqualTo(persistentMapOf(ObjectField.NOTE_TEXT to FieldProblem.TOO_LONG))

        assertThat(repository.added).isEmpty()
        assertThat(repository.updated).isEmpty()
    }

    @Test
    fun `a note is deleted by its id`() = runTest {
        assertThat(DeleteNoteUseCase(repository)(NoteId("n")).isSuccess).isTrue()

        assertThat(repository.deleted).containsExactly(NoteId("n"))
    }
}
