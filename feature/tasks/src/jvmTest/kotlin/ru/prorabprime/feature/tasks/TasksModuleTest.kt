package ru.prorabprime.feature.tasks

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import ru.prorabprime.domain.model.ObjectId
import ru.prorabprime.domain.repository.ObjectsRepository
import ru.prorabprime.domain.repository.TasksRepository
import ru.prorabprime.domain.usecase.DeleteTaskUseCase
import ru.prorabprime.domain.usecase.ObserveDayTasksUseCase
import ru.prorabprime.domain.usecase.ObserveNextTaskUseCase
import ru.prorabprime.domain.usecase.ObserveObjectsUseCase
import ru.prorabprime.domain.usecase.ObserveOverdueTasksUseCase
import ru.prorabprime.domain.usecase.ObserveTasksRangeUseCase
import ru.prorabprime.domain.usecase.SaveTaskUseCase
import ru.prorabprime.testing.FakeObjectsRepository
import ru.prorabprime.testing.FakeSnackbarNotifier
import ru.prorabprime.testing.FakeTasksRepository
import ru.prorabprime.testing.MainDispatcherRule
import ru.prorabprime.ui.SnackbarNotifier

/** Koin resolves at runtime; this is what catches a definition that drifted from its constructor. */
class TasksModuleTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakes = module {
        single<TasksRepository> { FakeTasksRepository() }
        single<ObjectsRepository> { FakeObjectsRepository() }
        factory { ObserveObjectsUseCase(get()) }
        factory { ObserveDayTasksUseCase(get()) }
        factory { ObserveOverdueTasksUseCase(get()) }
        factory { ObserveTasksRangeUseCase(get()) }
        factory { ObserveNextTaskUseCase(get()) }
        factory { SaveTaskUseCase(get()) }
        factory { DeleteTaskUseCase(get()) }
        single<SnackbarNotifier> { FakeSnackbarNotifier() }
    }

    @Test
    fun `resolves the tasks ViewModel for all objects and for one`() {
        val koin = koinApplication { modules(fakes, tasksModule) }.koin

        assertThat(koin.get<TasksViewModel> { parametersOf(TasksScope(null)) }).isNotNull()
        assertThat(koin.get<TasksViewModel> { parametersOf(TasksScope(ObjectId("o1"))) }).isNotNull()
    }
}
