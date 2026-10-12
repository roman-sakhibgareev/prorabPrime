package ru.prorabprime.feature.journal

import com.google.common.truth.Truth.assertThat
import kotlin.time.Instant
import org.junit.Test
import ru.prorabprime.domain.model.LocalDay
import ru.prorabprime.domain.model.Note
import ru.prorabprime.domain.model.NoteId

class JournalUiMapperTest {

    // Moscow: three hours ahead of UTC, so 22:30Z on the 12th is 01:30 on the 13th.
    private val now = Instant.parse("2026-10-13T08:00:00Z")
    private val mapper = JournalUiMapper(now = { now }, offsetMinutes = { 3 * 60 })

    private fun note(
        id: String,
        at: String,
        text: String = id,
        pending: Boolean = false,
    ) = Note(NoteId(id), text, Instant.parse(at), pending)

    @Test
    fun `lines are grouped by the day they were written on the wall clock, oldest day first`() {
        val journal = mapper.map(
            listOf(
                note("a", "2026-10-10T09:00:00Z"),
                note("b", "2026-10-12T22:30:00Z"),
                note("c", "2026-10-13T05:15:00Z"),
            ),
        )

        assertThat(journal.days.map { it.day }).containsExactly(
            LocalDay.of(2026, 10, 10),
            LocalDay.of(2026, 10, 13),
        ).inOrder()
        assertThat(journal.days[1].notes.map { it.id }).containsExactly("b", "c").inOrder()
        assertThat(journal.days[1].notes.map { it.time }).containsExactly("01:30", "08:15").inOrder()
        assertThat(journal.count).isEqualTo(3)
    }

    @Test
    fun `a day is headed today, yesterday or by its date`() {
        val journal = mapper.map(
            listOf(
                note("old", "2026-10-09T09:00:00Z"),
                note("yesterday", "2026-10-12T09:00:00Z"),
                note("today", "2026-10-13T05:00:00Z"),
            ),
        )

        assertThat(journal.days.map { it.heading }).containsExactly(
            DayHeading.Date("09.10.2026"),
            DayHeading.Yesterday,
            DayHeading.Today,
        ).inOrder()
    }

    @Test
    fun `a line waiting for the server is marked, and an empty log is empty`() {
        val journal = mapper.map(listOf(note("p", "2026-10-13T05:00:00Z", pending = true)))

        assertThat(journal.days.single().notes.single().isPending).isTrue()
        assertThat(mapper.map(emptyList()).isEmpty).isTrue()
    }
}
