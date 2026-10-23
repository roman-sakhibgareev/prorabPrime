package ru.prorabprime.domain.usecase

import kotlinx.coroutines.flow.Flow
import ru.prorabprime.domain.model.PaymentJournal
import ru.prorabprime.domain.repository.PaymentJournalRepository

class ObservePaymentJournalUseCase(
    private val repository: PaymentJournalRepository,
) {
    operator fun invoke(): Flow<Result<PaymentJournal>> = repository.observeJournal()
}
