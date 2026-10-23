package ru.prorabprime.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import ru.prorabprime.testing.FakeSettingsRepository

class AnalyticsConsentUseCaseTest {

    private val repository = FakeSettingsRepository()
    private val consent = AnalyticsConsentUseCase(repository)

    @Test
    fun `reporting is allowed until the user says no`() = runTest {
        assertThat(consent.enabled.first()).isTrue()
    }

    @Test
    fun `the choice is saved and read back`() = runTest {
        consent.set(false)

        assertThat(consent.enabled.first()).isFalse()
        assertThat(repository.analyticsEnabled.value).isFalse()
    }
}
