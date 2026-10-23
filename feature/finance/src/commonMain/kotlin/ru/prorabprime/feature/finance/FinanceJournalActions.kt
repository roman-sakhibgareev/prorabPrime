package ru.prorabprime.feature.finance

import ru.prorabprime.domain.usecase.DeletePaymentUseCase
import ru.prorabprime.domain.usecase.ObservePaymentJournalUseCase
import ru.prorabprime.domain.usecase.SavePaymentUseCase

/** The use cases the journal calls, in one collaborator to keep the ViewModel's constructor short. */
internal class FinanceJournalActions(
    val observeJournal: ObservePaymentJournalUseCase,
    val savePayment: SavePaymentUseCase,
    val deletePayment: DeletePaymentUseCase,
)
