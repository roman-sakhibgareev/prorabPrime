package ru.prorabprime

import android.app.Application
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.AppMetricaConfig
import org.koin.dsl.module
import ru.prorabprime.domain.AnalyticsTracker
import ru.prorabprime.domain.model.AnalyticsEvent

/** Sends events to AppMetrica while the user allows it; AppMetrica must be activated first ([startAppMetrica]). */
internal class AppMetricaTracker : AnalyticsTracker {

    @Volatile
    var enabled: Boolean = false

    override fun track(event: AnalyticsEvent) {
        if (enabled) AppMetrica.reportEvent(event.name, event.props)
    }
}

/**
 * Activates AppMetrica with Java, native and ANR crash reports on. Returns whether it is on: without a key it stays
 * off and nothing is reported. Sending starts switched off, until the user's choice has been read ([allowSending]),
 * so an opt-out holds from the first launch. Location is not collected.
 */
internal fun startAppMetrica(application: Application, apiKey: String): Boolean {
    if (apiKey.isBlank()) return false
    val config = AppMetricaConfig.newConfigBuilder(apiKey)
        .withCrashReporting(true)
        .withNativeCrashReporting(true)
        .withAnrMonitoring(true)
        .withLocationTracking(false)
        .withDataSendingEnabled(false)
        .build()
    AppMetrica.activate(application, config)
    AppMetrica.enableActivityAutoTracking(application)
    return true
}

/** Lets AppMetrica send crash reports and events, or stops it; the user's choice in the settings. */
internal fun allowSending(tracker: AppMetricaTracker, allowed: Boolean) {
    tracker.enabled = allowed
    AppMetrica.setDataSendingEnabled(allowed)
}

/** The tracker the domain sees: AppMetrica's when it is on, otherwise one that does nothing. */
internal fun analyticsModule(tracker: AppMetricaTracker?) = module {
    single<AnalyticsTracker> { tracker ?: AnalyticsTracker.None }
}
