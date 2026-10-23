package ru.prorabprime.domain

import ru.prorabprime.domain.model.ServerFilePath

/** Hands pictures to the system's share sheet, so the user can send them to any messenger. Platform-specific. */
interface ImageSharer {

    /** Offers the full-size pictures at [images]; they are already cleaned and counted by `ShareImagesUseCase`. */
    suspend fun share(images: List<ServerFilePath>): ShareResult

    /** What the browser and the tests that do not care use: it cannot share. */
    data object Unsupported : ImageSharer {
        override suspend fun share(images: List<ServerFilePath>): ShareResult = ShareResult.Unsupported
    }
}

sealed interface ShareResult {
    /** The share sheet is open with [count] pictures; what the user does there is not known. */
    data class Shared(
        val count: Int,
    ) : ShareResult

    data object NothingToShare : ShareResult

    /** More than [limit] pictures were asked for; none was shared. */
    data class TooMany(
        val limit: Int,
    ) : ShareResult

    /** This platform has no way to share pictures. */
    data object Unsupported : ShareResult

    /** No picture could be made ready, for example none was cached and there is no signal. */
    data object Failed : ShareResult
}
