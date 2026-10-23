package ru.prorabprime

import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.prorabprime.domain.model.ServerFilePath

class ShareStagingTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val dir get() = File(folder.root, "shared-images")
    private val staging get() = ShareStaging(dir)

    private fun path(name: String) = ServerFilePath("/files/$name")

    private val write: suspend (ServerFilePath, File) -> Boolean = { path, target ->
        target.writeText(path.value)
        true
    }

    @Test
    fun `each picture is copied under its place in the share, in order`() = runTest {
        val files = staging.stage(listOf(path("a"), path("b"), path("c")), write)

        assertThat(files.map { it.name }).containsExactly("receipt-1.jpg", "receipt-2.jpg", "receipt-3.jpg").inOrder()
        assertThat(files.map { it.readText() }).containsExactly("/files/a", "/files/b", "/files/c").inOrder()
    }

    @Test
    fun `one picture that cannot be fetched is left out and leaves no file`() = runTest {
        val files = staging.stage(listOf(path("a"), path("b"), path("c"))) { p, target ->
            p != path("b") && write(p, target)
        }

        assertThat(files.map { it.readText() }).containsExactly("/files/a", "/files/c").inOrder()
        assertThat(dir.list()!!.toList()).containsExactly("receipt-1.jpg", "receipt-3.jpg")
    }

    @Test
    fun `a failed fetch that wrote part of the file leaves nothing behind`() = runTest {
        val files = staging.stage(listOf(path("a"))) { _, target ->
            target.writeText("half")
            false
        }

        assertThat(files).isEmpty()
        assertThat(dir.list()!!.toList()).isEmpty()
    }

    @Test
    fun `an earlier share is removed before the next`() = runTest {
        staging.stage(listOf(path("a"), path("b"), path("c")), write)

        val files = staging.stage(listOf(path("d")), write)

        assertThat(files.map { it.readText() }).containsExactly("/files/d")
        assertThat(dir.list()!!.toList()).containsExactly("receipt-1.jpg")
    }

    @Test
    fun `clearing an empty or missing folder is harmless`() {
        staging.clear()

        assertThat(dir.exists()).isFalse()
    }
}
