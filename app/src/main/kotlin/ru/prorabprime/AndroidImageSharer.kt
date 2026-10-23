package ru.prorabprime

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Size
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.dsl.module
import ru.prorabprime.data.network.toRequestUrl
import ru.prorabprime.domain.ImageSharer
import ru.prorabprime.domain.ShareResult
import ru.prorabprime.domain.model.ServerFilePath

/**
 * Opens the system's share sheet with the pictures, over a [FileProvider] that serves only the share folder (no
 * storage permission). Pictures come from Coil's disk cache when they were seen, and from the server through the same
 * loader when not; one still on the phone is copied from where it waits.
 */
internal class AndroidImageSharer(
    private val context: Context,
    private val loader: ImageLoader,
) : ImageSharer {

    private val authority = "${context.packageName}.shareprovider"
    private val staging = ShareStaging(File(context.cacheDir, SHARE_DIR))

    override suspend fun share(images: List<ServerFilePath>): ShareResult = withContext(Dispatchers.IO) {
        val files = staging.stage(images, ::fetch)
        if (files.isEmpty()) return@withContext ShareResult.Failed
        val uris = files.map { FileProvider.getUriForFile(context, authority, it) }
        try {
            context.startActivity(chooser(uris))
            ShareResult.Shared(files.size)
        } catch (_: ActivityNotFoundException) {
            ShareResult.Failed
        }
    }

    private fun chooser(uris: List<Uri>): Intent {
        val send = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris.single())
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
        send.type = "image/*"
        // The clip is what carries the read grant through the chooser to the app picked there.
        send.clipData = ClipData.newRawUri(null, uris.first()).also { clip ->
            uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
        }
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private suspend fun fetch(path: ServerFilePath, target: File): Boolean = try {
        if (path.value.startsWith(LOCAL_SCHEME)) {
            copyLocal(path.value.removePrefix(LOCAL_SCHEME), target)
        } else {
            copyCached(path, target)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        false
    }

    private fun copyLocal(source: String, target: File): Boolean {
        val file = File(source)
        if (!file.isFile) return false
        file.copyTo(target, overwrite = true)
        return true
    }

    private suspend fun copyCached(path: ServerFilePath, target: File): Boolean {
        val key = path.toRequestUrl()
        return copyFromDiskCache(key, target) || run {
            // Not seen yet: the loader fetches it with the app's own client and keeps it in the disk cache.
            loader.execute(
                ImageRequest.Builder(context)
                    .data(path)
                    .diskCacheKey(key)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .size(Size(1, 1))
                    .build(),
            )
            copyFromDiskCache(key, target)
        }
    }

    private fun copyFromDiskCache(key: String, target: File): Boolean {
        val snapshot = loader.diskCache?.openSnapshot(key) ?: return false
        snapshot.use { it.data.toFile().copyTo(target, overwrite = true) }
        return true
    }

    private companion object {
        const val SHARE_DIR = "shared-images"
        const val LOCAL_SCHEME = "file://"
    }
}

/** The Android half of [ImageSharer]: the share sheet. */
internal fun imageSharerModule(context: Context, loader: ImageLoader) = module {
    single<ImageSharer> { AndroidImageSharer(context, loader) }
}
