package ru.prorabprime.domain.model

/**
 * Every event the app reports, and the only way to make one: the catalog in `docs/ANALYTICS.md` is this list. A
 * prop is a word or a yes/no from a fixed set, never something the user typed and never an id (ADR-0029).
 */
sealed class AnalyticsEvent(
    val name: String,
    val props: Map<String, String> = emptyMap(),
) {
    data class ScreenView(
        val screen: AppScreen,
    ) : AnalyticsEvent("screen_view", mapOf("screen" to screen.name))

    data class ObjectCreated(
        val status: ObjectStatus,
    ) : AnalyticsEvent("object_created", mapOf("status" to status.word()))

    data class ObjectStatusChanged(
        val to: ObjectStatus,
    ) : AnalyticsEvent("object_status_changed", mapOf("to" to to.word()))

    data object ObjectDeleted : AnalyticsEvent("object_deleted")

    data class PhotoAdded(
        val kind: AttachmentKind,
    ) : AnalyticsEvent("photo_added", mapOf("kind" to kind.name.lowercase()))

    data object PaymentAdded : AnalyticsEvent("payment_added")

    data object ExtraWorkAdded : AnalyticsEvent("extra_work_added")

    data class TaskCreated(
        val withReminder: Boolean,
    ) : AnalyticsEvent("task_created", mapOf("reminder" to if (withReminder) "yes" else "no"))
}

private fun ObjectStatus.word() = name.lowercase()

/** A screen by a fixed word; never a key's arguments, which are ids of the user's objects. */
enum class AppScreen {
    ObjectsList,
    ObjectDetails,
    ObjectEdit,
    PhotoViewer,
    Gallery,
    Settings,
    Finance,
    Materials,
    Journal,
    Map,
    PlacePicker,
    Tasks,
    Expenses,
    Trash,
    Other,
}
