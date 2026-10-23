package ru.prorabprime.shared.navigation

import androidx.navigation3.runtime.NavKey
import ru.prorabprime.domain.AnalyticsTracker
import ru.prorabprime.domain.model.AnalyticsEvent
import ru.prorabprime.domain.model.AppScreen
import ru.prorabprime.feature.expenses.ExpensesNavKey
import ru.prorabprime.feature.finance.FinanceNavKey
import ru.prorabprime.feature.journal.JournalNavKey
import ru.prorabprime.feature.map.MapNavKey
import ru.prorabprime.feature.map.PlacePickerNavKey
import ru.prorabprime.feature.materials.MaterialsNavKey
import ru.prorabprime.feature.objects.details.ObjectDetailsNavKey
import ru.prorabprime.feature.objects.edit.ObjectEditNavKey
import ru.prorabprime.feature.objects.gallery.GalleryNavKey
import ru.prorabprime.feature.objects.list.ObjectsListNavKey
import ru.prorabprime.feature.objects.viewer.PhotoViewerNavKey
import ru.prorabprime.feature.settings.SettingsNavKey
import ru.prorabprime.feature.tasks.TasksNavKey
import ru.prorabprime.feature.trash.TrashNavKey

/**
 * The screen a key stands for, by a fixed word. Taken from a table rather than the class name, which R8 renames in a
 * release build, and free of the key's arguments, which are ids of the user's objects.
 */
internal fun NavKey.screen(): AppScreen = SCREENS[this::class] ?: AppScreen.Other

/** A screen coming to the front, going forward or back. */
internal fun reportScreen(key: NavKey, tracker: AnalyticsTracker) {
    tracker.track(AnalyticsEvent.ScreenView(key.screen()))
}

internal val SCREENS = mapOf(
    ObjectsListNavKey::class to AppScreen.ObjectsList,
    ObjectDetailsNavKey::class to AppScreen.ObjectDetails,
    ObjectEditNavKey::class to AppScreen.ObjectEdit,
    PhotoViewerNavKey::class to AppScreen.PhotoViewer,
    GalleryNavKey::class to AppScreen.Gallery,
    SettingsNavKey::class to AppScreen.Settings,
    FinanceNavKey::class to AppScreen.Finance,
    MaterialsNavKey::class to AppScreen.Materials,
    JournalNavKey::class to AppScreen.Journal,
    MapNavKey::class to AppScreen.Map,
    PlacePickerNavKey::class to AppScreen.PlacePicker,
    TasksNavKey::class to AppScreen.Tasks,
    ExpensesNavKey::class to AppScreen.Expenses,
    TrashNavKey::class to AppScreen.Trash,
)
