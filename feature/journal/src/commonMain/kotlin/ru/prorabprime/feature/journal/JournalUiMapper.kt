package ru.prorabprime.feature.journal

import kotlin.time.Instant
import kotlinx.collections.immutable.toImmutableList
import ru.prorabprime.domain.model.LocalDay
import ru.prorabprime.domain.model.Note
import ru.prorabprime.ui.localUtcOffsetMinutes
import ru.prorabprime.ui.toLocalMoment

/**
 * Groups the log by the day each line was written on the device's wall clock, oldest day first.
 * [now] and [offsetMinutes] are given so a test can fix the day and the zone.
 */
internal class JournalUiMapper(
    private val now: () -> Instant,
    private val offsetMinutes: (Instant) -> Int = ::localUtcOffsetMinutes,
) {
    fun map(notes: List<Note>): JournalUi {
        val today = now().toLocalMoment(offsetMinutes).day
        val days = notes
            .map { it to it.createdAt.toLocalMoment(offsetMinutes) }
            .groupBy { (_, moment) -> moment.day }
            .map { (day, lines) ->
                JournalDayUi(
                    day = day,
                    heading = heading(day, today),
                    notes = lines.map { (note, moment) ->
                        NoteUi(note.id.value, note.text, moment.clock(), note.isPending)
                    }.toImmutableList(),
                )
            }
        return JournalUi(days.toImmutableList())
    }

    private fun heading(day: LocalDay, today: LocalDay): DayHeading = when (day) {
        today -> DayHeading.Today
        today.plusDays(-1) -> DayHeading.Yesterday
        else -> DayHeading.Date(day.format())
    }
}
