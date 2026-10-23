package ru.prorabprime

import java.io.File
import ru.prorabprime.domain.model.ServerFilePath

/**
 * The folder the share sheet reads from: a private copy of each picture, named by its place in the share. What an
 * earlier share left is removed first, so the folder never holds more than one share. Nothing here knows Android.
 */
internal class ShareStaging(
    private val dir: File,
) {
    /**
     * Copies each of [paths] into the folder with [fetch], which fills the file it is given and says whether it
     * could (it must not throw but for cancellation). Pictures that cannot be fetched are left out; the rest keep
     * their order.
     */
    suspend fun stage(paths: List<ServerFilePath>, fetch: suspend (ServerFilePath, File) -> Boolean): List<File> {
        clear()
        dir.mkdirs()
        return paths.mapIndexedNotNull { index, path ->
            File(dir, "receipt-${index + 1}.jpg").takeIf { target ->
                val fetched = fetch(path, target)
                if (!fetched) target.delete()
                fetched
            }
        }
    }

    fun clear() {
        dir.listFiles()?.forEach { it.delete() }
    }
}
