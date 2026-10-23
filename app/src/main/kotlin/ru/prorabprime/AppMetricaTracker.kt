package ru.prorabprime

import android.app.Application
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.AppMetricaConfig
import org.koin.dsl.module
import ru.prorabprime.domain.AnalyticsTracker
import ru.prorabprime.domain.model.AnalyticsEvent

/** Sends events to AppMetrica, which must be activated first ([startAppMetrica]). */
internal class AppMetricaTracker : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) {
        AppMetrica.reportEvent(event.name, event.props)
    }
}

/**
 * Activates AppMetrica with Java, native and ANR crash reports on. Returns whether it is on: without a key it stays
 * off and nothing is reported. Location is not collected.
 */
internal fun startAppMetrica(application: Application, apiKey: String): Boolean {
    if (apiKey.isBlank()) return false
    val config = AppMetricaConfig.newConfigBuilder(apiKey)
        .withCrashReporting(true)
        .withNativeCrashReporting(true)
        .withAnrMonitoring(true)
        .withLocationTracking(false)
        .build()
    AppMetrica.activate(application, config)
    AppMetrica.enableActivityAutoTracking(application)
    return true
}

/** The tracker the domain sees: AppMetrica when it is on, otherwise one that does nothing. */
internal fun analyticsModule(enabled: Boolean) = module {
    single<AnalyticsTracker> { if (enabled) AppMetricaTracker() else AnalyticsTracker.None }
}
