package ru.prorabprime.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import ru.prorabprime.domain.model.AppError
import ru.prorabprime.domain.model.GeoPoint
import ru.prorabprime.domain.model.ObjectId
import ru.prorabprime.domain.model.ObjectStatus
import ru.prorabprime.domain.model.asAppError
import ru.prorabprime.testing.FakeObjectsRepository
import ru.prorabprime.testing.anObjectDetails

class ChangeObjectStatusUseCaseTest {

    private val repository = FakeObjectsRepository()
    private val changeStatus = ChangeObjectStatusUseCase(repository)
    private val id = ObjectId("a")

    @Test
    fun `changes only the status and keeps the pin`() = runTest {
        repository.details.value = mapOf(
            id to anObjectDetails(id = "a", title = "Квартира", status = ObjectStatus.PLANNED)
                .copy(clientName = "Ара", latitude = 56.8, longitude = 60.6),
        )

        val result = changeStatus(id, ObjectStatus.DONE)

        assertThat(result.isSuccess).isTrue()
        val (updatedId, draft) = repository.updated.single()
        assertThat(updatedId).isEqualTo(id)
        assertThat(draft.status).isEqualTo(ObjectStatus.DONE)
        assertThat(draft.title).isEqualTo("Квартира")
        assertThat(draft.clientName).isEqualTo("Ара")
        assertThat(draft.point).isEqualTo(GeoPoint(56.8, 60.6))
    }

    @Test
    fun `the same status writes nothing`() = runTest {
        repository.details.value = mapOf(id to anObjectDetails(id = "a", status = ObjectStatus.IN_PROGRESS))

        assertThat(changeStatus(id, ObjectStatus.IN_PROGRESS).isSuccess).isTrue()
        assertThat(repository.updated).isEmpty()
    }

    @Test
    fun `a missing object fails as NotFound`() = runTest {
        val result = changeStatus(ObjectId("gone"), ObjectStatus.DONE)

        assertThat(result.exceptionOrNull()?.asAppError()).isEqualTo(AppError.NotFound)
    }
}
