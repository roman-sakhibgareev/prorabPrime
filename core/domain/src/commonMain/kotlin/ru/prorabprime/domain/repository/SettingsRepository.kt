package ru.prorabprime.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.prorabprime.domain.model.ObjectSort
import ru.prorabprime.domain.model.ServerSettings

/** Settings persisted on the device. */
interface SettingsRepository {
    val serverSettings: Flow<ServerSettings>

    val objectSort: Flow<ObjectSort>

    /** Whether crash reports and usage statistics may be sent; on until the user turns it off (ADR-0029). */
    val analyticsEnabled: Flow<Boolean>

    suspend fun saveServerSettings(settings: ServerSettings)

    suspend fun saveObjectSort(sort: ObjectSort)

    suspend fun saveAnalyticsEnabled(enabled: Boolean)
}
