package ru.prorabprime.designsystem.components

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull
import ru.prorabprime.ui.AppMessage
import ru.prorabprime.ui.MessageKind

/** What a Snackbar says and how it went, so the host can color it. */
class AppSnackbarVisuals(
    override val message: String,
    val kind: MessageKind,
) : SnackbarVisuals {
    override val actionLabel: String? = null
    override val withDismissAction: Boolean = false

    // Material's Short is 4s; the timeout in [show] ends it sooner.
    override val duration: SnackbarDuration = SnackbarDuration.Indefinite
}

private const val SHOWN_MILLIS = 2500L

/** Shows the message for [SHOWN_MILLIS], or until the user swipes it down. */
suspend fun SnackbarHostState.show(message: AppMessage, text: String) {
    withTimeoutOrNull(SHOWN_MILLIS) { showSnackbar(AppSnackbarVisuals(text, message.kind)) }
}

/** In the theme's green when it went well, red when it did not; a swipe down dismisses it. */
@Composable
fun AppSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(state, modifier) { data -> AppSnackbar(data) }
}

@Composable
private fun AppSnackbar(data: SnackbarData) {
    val success = (data.visuals as? AppSnackbarVisuals)?.kind == MessageKind.Success
    val colors = MaterialTheme.colorScheme
    Snackbar(
        snackbarData = data,
        modifier = Modifier.pointerInput(data) {
            var dragged = 0f
            detectVerticalDragGestures(
                onDragStart = { dragged = 0f },
                onVerticalDrag = { _, delta -> dragged += delta },
                onDragEnd = { if (dragged > SWIPE_DISMISS_PX) data.dismiss() },
            )
        },
        containerColor = if (success) colors.primary else colors.error,
        contentColor = if (success) colors.onPrimary else colors.onError,
        shape = MaterialTheme.shapes.small,
    )
}

private const val SWIPE_DISMISS_PX = 40f
