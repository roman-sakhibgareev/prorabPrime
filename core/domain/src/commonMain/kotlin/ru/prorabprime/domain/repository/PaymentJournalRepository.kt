package ru.prorabprime.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.prorabprime.domain.model.PaymentJournal

/** The payments of every object together: read from the phone's copy, so it works without a signal too. */
interface PaymentJournalRepository {
    fun observeJournal(): Flow<Result<PaymentJournal>>
}
