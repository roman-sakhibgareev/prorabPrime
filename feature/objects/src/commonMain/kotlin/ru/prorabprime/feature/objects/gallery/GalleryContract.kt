package ru.prorabprime.feature.objects.gallery

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import ru.prorabprime.domain.model.ServerFilePath
import ru.prorabprime.ui.DialogModel
import ru.prorabprime.ui.UiText

@Immutable
internal sealed interface GalleryStatus {
    // Declared most-likely first; every `when` over this mirrors the order.
    data object Content : GalleryStatus

    data object Loading : GalleryStatus

    data class Error(
        val message: UiText,
    ) : GalleryStatus
}

/** One tile of the grid. */
@Immutable
internal data class GalleryPhotoUi(
    val id: String,
    val thumb: ServerFilePath,
    /** The full-size picture, which is what a share sends. */
    val path: ServerFilePath,
    val isCover: Boolean,
    val isPending: Boolean,
    val quarterTurns: Int,
    /** `790 ₽` for a receipt with a sum; null otherwise. */
    val amount: String?,
)

@Immutable
internal data class GalleryState(
    val status: GalleryStatus = GalleryStatus.Loading,
    val receipts: Boolean = false,
    val photos: ImmutableList<GalleryPhotoUi> = persistentListOf(),
    /** Taps pick tiles instead of opening them. */
    val selecting: Boolean = false,
    val selected: ImmutableSet<String> = persistentSetOf(),
    val dialog: DialogModel? = null,
    /** A send is getting its pictures ready; a second one would open a second sheet. */
    val isSharing: Boolean = false,
)

internal sealed interface GalleryEvent {
    /** The "select" button, or the cross that leaves selecting. */
    data object SelectingToggled : GalleryEvent

    /** A tap on a tile while selecting, or a long press on any tile. */
    data class PhotoToggled(
        val photoId: String,
    ) : GalleryEvent

    /** Picks every photo, or none when every photo is already picked. */
    data object SelectAllToggled : GalleryEvent

    data object DeleteClicked : GalleryEvent

    /** Sends the picked pictures through the share sheet. */
    data object ShareClicked : GalleryEvent

    data object DialogConfirmed : GalleryEvent

    data object DialogDismissed : GalleryEvent
}

internal data class GalleryArgs(
    val objectId: String,
    val receipts: Boolean,
)
