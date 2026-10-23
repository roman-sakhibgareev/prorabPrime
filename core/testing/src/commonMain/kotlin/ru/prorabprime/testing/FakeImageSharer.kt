package ru.prorabprime.testing

import ru.prorabprime.domain.ImageSharer
import ru.prorabprime.domain.ShareResult
import ru.prorabprime.domain.model.ServerFilePath

class FakeImageSharer : ImageSharer {

    /** What a share answers; by default the sheet opens with every picture asked for. */
    var result: (List<ServerFilePath>) -> ShareResult = { ShareResult.Shared(it.size) }

    val shared = mutableListOf<List<ServerFilePath>>()

    override suspend fun share(images: List<ServerFilePath>): ShareResult {
        shared += images
        return result(images)
    }
}
