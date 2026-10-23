package ru.prorabprime.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import ru.prorabprime.domain.ImageSharer
import ru.prorabprime.domain.ShareResult
import ru.prorabprime.domain.model.ServerFilePath
import ru.prorabprime.testing.FakeImageSharer

class ShareImagesUseCaseTest {

    private val sharer = FakeImageSharer()
    private val share = ShareImagesUseCase(sharer)

    private fun paths(count: Int) = List(count) { ServerFilePath("/files/$it.jpg") }

    @Test
    fun `one picture is shared on its own`() = runTest {
        val result = share(paths(1))

        assertThat(result).isEqualTo(ShareResult.Shared(1))
        assertThat(sharer.shared).containsExactly(paths(1))
    }

    @Test
    fun `several pictures are shared together and in order`() = runTest {
        val result = share(paths(4))

        assertThat(result).isEqualTo(ShareResult.Shared(4))
        assertThat(sharer.shared).containsExactly(paths(4))
    }

    @Test
    fun `an empty selection and paths with no file share nothing`() = runTest {
        assertThat(share(emptyList())).isEqualTo(ShareResult.NothingToShare)
        assertThat(share(listOf(ServerFilePath(""), ServerFilePath("  ")))).isEqualTo(ShareResult.NothingToShare)
        assertThat(sharer.shared).isEmpty()
    }

    @Test
    fun `a picture chosen twice is sent once`() = runTest {
        val path = ServerFilePath("/files/a.jpg")

        share(listOf(path, ServerFilePath(""), path))

        assertThat(sharer.shared).containsExactly(listOf(path))
    }

    @Test
    fun `the limit is allowed and one more is refused`() = runTest {
        assertThat(share(paths(MAX_SHARED_IMAGES))).isEqualTo(ShareResult.Shared(MAX_SHARED_IMAGES))

        assertThat(share(paths(MAX_SHARED_IMAGES + 1))).isEqualTo(ShareResult.TooMany(MAX_SHARED_IMAGES))
        assertThat(sharer.shared).hasSize(1)
    }

    @Test
    fun `the platform's answer is passed on`() = runTest {
        sharer.result = { ShareResult.Failed }

        assertThat(share(paths(2))).isEqualTo(ShareResult.Failed)
    }

    @Test
    fun `the browser's sharer says it cannot`() = runTest {
        assertThat(ShareImagesUseCase(ImageSharer.Unsupported)(paths(2))).isEqualTo(ShareResult.Unsupported)
    }
}
