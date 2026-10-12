package ru.prorabprime.ui

import kotlin.time.Instant

// The browser's offset for that very moment, so a daylight-saving change is honored.
@JsFun("(millis) => -new Date(millis).getTimezoneOffset()")
private external fun browserOffsetMinutes(millis: Double): Int

actual fun localUtcOffsetMinutes(at: Instant): Int = browserOffsetMinutes(at.toEpochMilliseconds().toDouble())
