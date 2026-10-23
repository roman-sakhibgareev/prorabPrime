package ru.prorabprime.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import ru.prorabprime.domain.model.PaymentJournal
import ru.prorabprime.domain.repository.PaymentJournalRepository

/** In-memory [PaymentJournalRepository]: says nothing until [result] is set, then emits it as set. */
class FakePaymentJournalRepository : PaymentJournalRepository {

    val result = MutableStateFlow<Result<PaymentJournal>?>(null)

    override fun observeJournal(): Flow<Result<PaymentJournal>> = result.filterNotNull()
}
