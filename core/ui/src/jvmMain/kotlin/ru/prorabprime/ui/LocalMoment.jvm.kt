package ru.prorabprime.ui

import java.util.TimeZone
import kotlin.time.Instant

private const val MILLIS_IN_MINUTE = 60_000

actual fun localUtcOffsetMinutes(at: Instant): Int =
    TimeZone.getDefault().getOffset(at.toEpochMilliseconds()) / MILLIS_IN_MINUTE
