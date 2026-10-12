package ru.prorabprime.feature.tasks

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** The plan of every object, or, with an [objectId], the calendar of that one object. */
@Serializable
data class TasksNavKey(
    val objectId: String? = null,
) : NavKey
