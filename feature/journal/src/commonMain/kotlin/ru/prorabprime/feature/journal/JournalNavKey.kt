package ru.prorabprime.feature.journal

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class JournalNavKey(
    val objectId: String,
) : NavKey
