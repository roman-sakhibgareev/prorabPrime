package ru.prorabprime.domain.usecase

import ru.prorabprime.domain.AnalyticsTracker
import ru.prorabprime.domain.ImageCompressor
import ru.prorabprime.domain.model.AnalyticsEvent
import ru.prorabprime.domain.model.AttachmentKind
import ru.prorabprime.domain.model.LocalImageRef
import ru.prorabprime.domain.model.ObjectId
import ru.prorabprime.domain.model.Photo
import ru.prorabprime.domain.repository.PhotosRepository
import ru.prorabprime.domain.tracked

/** Compresses a picture from the device, then uploads it. Nothing is sent if compression fails. */
class UploadPhotoUseCase(
    private val compressor: ImageCompressor,
    private val repository: PhotosRepository,
    private val tracker: AnalyticsTracker = AnalyticsTracker.None,
) {
    suspend operator fun invoke(
        objectId: ObjectId,
        image: LocalImageRef,
        kind: AttachmentKind = AttachmentKind.PHOTO,
        note: String? = null,
    ): Result<Photo> {
        val compressed = compressor.compress(image).getOrElse { return Result.failure(it) }
        return repository.upload(objectId, compressed, kind, note).tracked(tracker) { AnalyticsEvent.PhotoAdded(kind) }
    }
}
