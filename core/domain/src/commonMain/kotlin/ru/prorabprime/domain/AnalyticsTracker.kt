package ru.prorabprime.domain

import ru.prorabprime.domain.model.AnalyticsEvent

/** Where the app says what happened (ADR-0029). Cheap, never blocks, never throws. */
interface AnalyticsTracker {

    fun track(event: AnalyticsEvent)

    /** What the browser and the tests that do not care use: it reports nothing. */
    data object None : AnalyticsTracker {
        override fun track(event: AnalyticsEvent) = Unit
    }
}

/** Reports [event] if this succeeded, and returns this as it was; a failed write is not something that happened. */
fun <T> Result<T>.tracked(tracker: AnalyticsTracker, event: () -> AnalyticsEvent): Result<T> = also {
    if (isSuccess) tracker.track(event())
}
