package ru.prorabprime.domain.usecase

import kotlinx.coroutines.flow.first
import ru.prorabprime.domain.model.ObjectDraft
import ru.prorabprime.domain.model.ObjectId
import ru.prorabprime.domain.model.ObjectStatus
import ru.prorabprime.domain.repository.ObjectsRepository

/** Changes only the status of an object, keeping every other field and the pin on the map as they are. */
class ChangeObjectStatusUseCase(
    private val repository: ObjectsRepository,
) {
    suspend operator fun invoke(id: ObjectId, status: ObjectStatus): Result<Unit> {
        val current = repository.observeObject(id).first().getOrElse { return Result.failure(it) }
        if (current.status == status) return Result.success(Unit)
        return repository.update(
            id,
            ObjectDraft(
                title = current.title,
                address = current.address,
                status = status,
                clientName = current.clientName,
                clientPhone = current.clientPhone,
                notes = current.notes,
                chatLink = current.chatLink,
                point = current.point,
            ),
        )
    }
}
