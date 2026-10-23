package ru.prorabprime.domain.usecase

import kotlinx.coroutines.flow.Flow
import ru.prorabprime.domain.repository.SettingsRepository

/** The user's choice to let the app send crash reports and usage statistics, and the way to change it. */
class AnalyticsConsentUseCase(
    private val repository: SettingsRepository,
) {
    val enabled: Flow<Boolean> = repository.analyticsEnabled

    suspend fun set(enabled: Boolean) = repository.saveAnalyticsEnabled(enabled)
}
