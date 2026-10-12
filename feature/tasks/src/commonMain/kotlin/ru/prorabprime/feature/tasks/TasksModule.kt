package ru.prorabprime.feature.tasks

import kotlin.time.Clock
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import ru.prorabprime.domain.model.LocalDay

/** The StateHolder is built inside the `viewModel` lambda, so the ViewModel and its ErrorHandler share it. */
val tasksModule = module {
    factory { TasksActions(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { (scope: TasksScope) ->
        val stateHolder: ITasksStateHolder = TasksStateHolder(
            today = LocalDay.ofInstant(Clock.System.now()),
            scopedObjectId = scope.objectId?.value,
        )
        TasksViewModel(
            stateHolder = stateHolder,
            errorHandler = TasksErrorHandler(stateHolder, get()),
            actions = get(),
        )
    }
}
