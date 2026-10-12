package ru.prorabprime.feature.journal

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import ru.prorabprime.domain.model.ObjectId
import ru.prorabprime.domain.repository.NotesRepository
import ru.prorabprime.domain.usecase.DeleteNoteUseCase
import ru.prorabprime.domain.usecase.ObserveNotesUseCase
import ru.prorabprime.domain.usecase.SaveNoteUseCase
import ru.prorabprime.testing.FakeNotesRepository
import ru.prorabprime.testing.FakeSnackbarNotifier
import ru.prorabprime.testing.MainDispatcherRule
import ru.prorabprime.ui.SnackbarNotifier

/** Koin resolves at runtime; this is what catches a definition that drifted from its constructor. */
class JournalModuleTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakes = module {
        single<NotesRepository> { FakeNotesRepository() }
        factory { ObserveNotesUseCase(get()) }
        factory { SaveNoteUseCase(get()) }
        factory { DeleteNoteUseCase(get()) }
        single<SnackbarNotifier> { FakeSnackbarNotifier() }
    }

    @Test
    fun `resolves the journal ViewModel with its argument`() {
        val koin = koinApplication { modules(fakes, journalModule) }.koin

        assertThat(koin.get<JournalViewModel> { parametersOf(ObjectId("o1")) }).isNotNull()
    }
}
