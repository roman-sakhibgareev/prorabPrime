package ru.prorabprime.ui

import kotlin.time.Instant
import ru.prorabprime.domain.model.LocalDay

/** The device's offset from UTC, in minutes, at [at]: what turns a stored instant into the wall clock. */
expect fun localUtcOffsetMinutes(at: Instant): Int

/** A wall-clock moment: the [day] and the [minutes] after midnight. */
data class LocalMoment(
    val day: LocalDay,
    val minutes: Int,
) {
    /** `HH:mm`. */
    fun clock(): String = "${(minutes / MINUTES_IN_HOUR).pad2()}:${(minutes % MINUTES_IN_HOUR).pad2()}"
}

/** This instant on the wall clock of the device, or of the zone [offsetMinutes] gives. */
fun Instant.toLocalMoment(offsetMinutes: (Instant) -> Int = ::localUtcOffsetMinutes): LocalMoment {
    val shifted = toEpochMilliseconds() + offsetMinutes(this) * MILLIS_IN_MINUTE
    val day = LocalDay.ofUtcMillis(shifted)
    return LocalMoment(day, ((shifted - LocalDay.toUtcMillis(day)) / MILLIS_IN_MINUTE).toInt())
}

private fun Int.pad2() = toString().padStart(2, '0')

private const val MINUTES_IN_HOUR = 60
private const val MILLIS_IN_MINUTE = 60_000L
