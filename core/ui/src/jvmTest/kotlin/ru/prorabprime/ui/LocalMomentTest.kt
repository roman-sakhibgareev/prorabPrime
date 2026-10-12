package ru.prorabprime.ui

import com.google.common.truth.Truth.assertThat
import kotlin.time.Instant
import org.junit.Test
import ru.prorabprime.domain.model.LocalDay

class LocalMomentTest {

    private val at = Instant.parse("2026-10-12T22:30:00Z")

    @Test
    fun `an instant is shifted by the offset and may land on the next day`() {
        val moscow = at.toLocalMoment { 3 * 60 }

        assertThat(moscow.day).isEqualTo(LocalDay.of(2026, 10, 13))
        assertThat(moscow.minutes).isEqualTo(90)
        assertThat(moscow.clock()).isEqualTo("01:30")
    }

    @Test
    fun `a zone behind UTC may land on the day before`() {
        val west = Instant.parse("2026-10-12T02:05:00Z").toLocalMoment { -5 * 60 }

        assertThat(west.day).isEqualTo(LocalDay.of(2026, 10, 11))
        assertThat(west.clock()).isEqualTo("21:05")
    }

    @Test
    fun `no offset is UTC itself`() {
        assertThat(at.toLocalMoment { 0 }).isEqualTo(
            LocalMoment(LocalDay.of(2026, 10, 12), 22 * 60 + 30),
        )
    }

    @Test
    fun `the device offset is a plausible number of minutes`() {
        assertThat(localUtcOffsetMinutes(at)).isIn(-14 * 60..14 * 60)
    }
}
