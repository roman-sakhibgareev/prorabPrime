package ru.prorabprime.domain.usecase

import ru.prorabprime.domain.AnalyticsTracker
import ru.prorabprime.domain.model.AnalyticsEvent
import ru.prorabprime.domain.model.ObjectId
import ru.prorabprime.domain.repository.ObjectsRepository
import ru.prorabprime.domain.tracked

class DeleteObjectUseCase(
    private val repository: ObjectsRepository,
    private val tracker: AnalyticsTracker = AnalyticsTracker.None,
) {
    suspend operator fun invoke(id: ObjectId): Result<Unit> =
        repository.delete(id).tracked(tracker) { AnalyticsEvent.ObjectDeleted }
}
