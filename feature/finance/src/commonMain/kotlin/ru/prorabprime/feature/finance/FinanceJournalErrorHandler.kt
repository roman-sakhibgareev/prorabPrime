package ru.prorabprime.feature.finance

import ru.prorabprime.domain.model.AppError
import ru.prorabprime.ui.SnackbarNotifier
import ru.prorabprime.ui.UiText
import ru.prorabprime.ui.toUiText

/** The journal's side of [IFinanceErrorHandler], so the payment form reports to it as it does on an object's screen. */
internal class FinanceJournalErrorHandler(
    private val stateHolder: IFinanceJournalStateHolder,
    private val notifier: SnackbarNotifier,
) : IFinanceErrorHandler {

    /** A reload failure keeps what is on screen; only a first load replaces it. */
    override suspend fun onLoadFailure(error: AppError) {
        if (stateHolder.state.value.status == FinanceJournalStatus.Content) {
            notifier.showError(error.toUiText())
        } else {
            stateHolder.showError(error.toUiText())
        }
    }

    override suspend fun onSaved(message: UiText) = notifier.showSuccess(message)

    override suspend fun onActionFailure(error: AppError) = notifier.showError(error.toUiText())
}
