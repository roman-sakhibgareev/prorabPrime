package ru.prorabprime.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AnalyticsEventTest {

    private val every = listOf(
        AnalyticsEvent.ScreenView(AppScreen.ObjectDetails),
        AnalyticsEvent.ObjectCreated(ObjectStatus.IN_PROGRESS),
        AnalyticsEvent.ObjectStatusChanged(ObjectStatus.DONE),
        AnalyticsEvent.ObjectDeleted,
        AnalyticsEvent.PhotoAdded(AttachmentKind.RECEIPT),
        AnalyticsEvent.PaymentAdded,
        AnalyticsEvent.ExtraWorkAdded,
        AnalyticsEvent.TaskCreated(withReminder = true),
    )

    @Test
    fun `names and prop keys are snake case words`() {
        val word = Regex("[a-z][a-z0-9_]*")
        every.forEach { event ->
            assertThat(event.name).matches(word.pattern)
            event.props.keys.forEach { assertThat(it).matches(word.pattern) }
        }
    }

    @Test
    fun `names are unique`() {
        assertThat(every.map { it.name }.toSet()).hasSize(every.size)
    }

    @Test
    fun `props are short words from a fixed set, so no free text can get in`() {
        every.flatMap { it.props.values }.forEach { assertThat(it).matches("[A-Za-z_]{1,30}") }
        assertThat(AnalyticsEvent.ObjectCreated(ObjectStatus.IN_PROGRESS).props)
            .containsExactly("status", "in_progress")
        assertThat(AnalyticsEvent.PhotoAdded(AttachmentKind.RECEIPT).props).containsExactly("kind", "receipt")
        assertThat(AnalyticsEvent.TaskCreated(withReminder = false).props).containsExactly("reminder", "no")
        assertThat(AnalyticsEvent.ScreenView(AppScreen.Gallery).props).containsExactly("screen", "Gallery")
    }
}
