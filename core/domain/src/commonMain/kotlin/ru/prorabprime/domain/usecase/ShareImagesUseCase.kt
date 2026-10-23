package ru.prorabprime.domain.usecase

import ru.prorabprime.domain.ImageSharer
import ru.prorabprime.domain.ShareResult
import ru.prorabprime.domain.model.ServerFilePath

/** The most pictures one share carries; messengers and the system's share sheet struggle with more. */
const val MAX_SHARED_IMAGES = 10

/** Shares the full-size pictures at [paths] through the platform's share sheet. */
class ShareImagesUseCase(
    private val sharer: ImageSharer,
) {
    /** Paths with no file and repeated paths are dropped first, so selecting the same picture twice sends it once. */
    suspend operator fun invoke(paths: List<ServerFilePath>): ShareResult {
        val images = paths.filter { it.value.isNotBlank() }.distinct()
        return when {
            images.isEmpty() -> ShareResult.NothingToShare
            images.size > MAX_SHARED_IMAGES -> ShareResult.TooMany(MAX_SHARED_IMAGES)
            else -> sharer.share(images)
        }
    }
}
