package ru.prorabprime.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import ru.prorabprime.data.local.Keys
import ru.prorabprime.data.local.LocalDb
import ru.prorabprime.data.local.PaymentRow
import ru.prorabprime.data.mapper.toDomain
import ru.prorabprime.domain.model.JournalEntry
import ru.prorabprime.domain.model.ObjectId
import ru.prorabprime.domain.model.PaymentJournal
import ru.prorabprime.domain.repository.PaymentJournalRepository

/** Every payment of every object, gathered from the phone's copy. */
internal class PaymentJournalRepositoryImpl(
    private val db: LocalDb,
) : PaymentJournalRepository {

    override fun observeJournal(): Flow<Result<PaymentJournal>> = db.changes.transform {
        // Nothing to say until the copy is loaded: an empty journal now would read as "no payments".
        if (db.objects.rows.value.isNotEmpty() || db.metaRow.syncTried) emit(Result.success(journal()))
    }

    private fun journal(): PaymentJournal {
        val objects = db.objects.rows.value
        val dirty = db.outbox.dirtyKeys()
        val newestFirst = db.payments.rows.value.values
            .sortedWith(compareByDescending<PaymentRow> { it.dto.paidOn }.thenByDescending { it.order })
        return PaymentJournal(
            newestFirst.mapNotNull { row ->
                val obj = objects[row.objectId] ?: return@mapNotNull null
                JournalEntry(
                    payment = row.dto.toDomain().copy(isPending = Keys.payment(row.dto.id) in dirty),
                    objectId = ObjectId(row.objectId),
                    objectTitle = obj.title ?: obj.address,
                )
            },
        )
    }
}
