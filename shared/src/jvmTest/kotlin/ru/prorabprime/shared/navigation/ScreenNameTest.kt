package ru.prorabprime.shared.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import ru.prorabprime.domain.model.AnalyticsEvent
import ru.prorabprime.domain.model.AppScreen
import ru.prorabprime.feature.finance.FinanceJournalNavKey
import ru.prorabprime.feature.objects.details.ObjectDetailsNavKey
import ru.prorabprime.feature.objects.list.ObjectsListNavKey
import ru.prorabprime.feature.settings.SettingsNavKey
import ru.prorabprime.testing.FakeAnalyticsTracker

class ScreenNameTest {

    @Test
    fun `a screen is named by a fixed word`() {
        assertThat(ObjectsListNavKey.screen()).isEqualTo(AppScreen.ObjectsList)
        assertThat(SettingsNavKey.screen()).isEqualTo(AppScreen.Settings)
        assertThat(FinanceJournalNavKey.screen()).isEqualTo(AppScreen.FinanceJournal)
    }

    @Test
    fun `every screen but Other is reachable from a key`() {
        assertThat(SCREENS.size).isEqualTo(AppScreen.entries.size - 1)
    }

    @Test
    fun `a screen coming to the front is reported by its fixed name, never by the key's arguments`() {
        val tracker = FakeAnalyticsTracker()

        reportScreen(ObjectsListNavKey, tracker)
        reportScreen(ObjectDetailsNavKey("secret-object-id"), tracker)

        assertThat(tracker.events).containsExactly(
            AnalyticsEvent.ScreenView(AppScreen.ObjectsList),
            AnalyticsEvent.ScreenView(AppScreen.ObjectDetails),
        ).inOrder()
        assertThat(tracker.events.flatMap { it.props.values }).doesNotContain("secret-object-id")
    }
}
