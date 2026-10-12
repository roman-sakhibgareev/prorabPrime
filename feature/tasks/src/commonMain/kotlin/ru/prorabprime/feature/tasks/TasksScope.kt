package ru.prorabprime.feature.tasks

import ru.prorabprime.domain.model.ObjectId

/** What the screen was opened for: the plan of one object, or (null) of all of them. */
internal class TasksScope(
    val objectId: ObjectId?,
)
