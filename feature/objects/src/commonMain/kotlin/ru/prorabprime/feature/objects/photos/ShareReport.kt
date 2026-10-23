package ru.prorabprime.feature.objects.photos

import kotlinx.collections.immutable.persistentListOf
import ru.prorabprime.domain.ShareResult
import ru.prorabprime.feature.objects.resources.Res
import ru.prorabprime.feature.objects.resources.share_failed
import ru.prorabprime.feature.objects.resources.share_limit
import ru.prorabprime.feature.objects.resources.share_nothing
import ru.prorabprime.feature.objects.resources.share_unsupported
import ru.prorabprime.ui.SnackbarNotifier
import ru.prorabprime.ui.UiText

/** Says why a share did not open the sheet; a share that did needs no word. Returns whether it opened. */
internal suspend fun ShareResult.report(notifier: SnackbarNotifier): Boolean {
    val message = when (this) {
        is ShareResult.Shared -> return true
        ShareResult.NothingToShare -> UiText.Resource(Res.string.share_nothing)
        is ShareResult.TooMany -> UiText.Resource(Res.string.share_limit, persistentListOf(limit))
        ShareResult.Unsupported -> UiText.Resource(Res.string.share_unsupported)
        ShareResult.Failed -> UiText.Resource(Res.string.share_failed)
    }
    notifier.showError(message)
    return false
}
