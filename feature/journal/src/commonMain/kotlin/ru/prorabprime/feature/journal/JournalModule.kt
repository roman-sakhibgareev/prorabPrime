package ru.prorabprime.feature.journal

import kotlin.time.Clock
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import ru.prorabprime.domain.model.ObjectId

/**
 * The StateHolder is built inside the `viewModel` lambda, so the ViewModel and its ErrorHandler
 * share one state object. The object id arrives as a Koin parameter.
 */
val journalModule = module {
    factory { JournalActions(get(), get(), get()) }
    viewModel { (objectId: ObjectId) ->
        val stateHolder: IJournalStateHolder = JournalStateHolder()
        JournalViewModel(
            objectId = objectId,
            stateHolder = stateHolder,
            errorHandler = JournalErrorHandler(stateHolder, get()),
            actions = get(),
            mapper = JournalUiMapper(now = Clock.System::now),
        )
    }
}
