package ru.prorabprime.data.sync

import ru.prorabprime.data.local.LocalDb
import ru.prorabprime.data.local.OperationState
import ru.prorabprime.data.local.QueuedOperation
import ru.prorabprime.data.local.isCreate
import ru.prorabprime.domain.model.AppError
import ru.prorabprime.domain.model.PhotoRejection
import ru.prorabprime.domain.model.asAppError

/**
 * Sends the queued changes in the order they were made. Without an answer it stops (the rest stay
 * queued); a change the server refuses for good is marked and skipped, so one bad change never holds
 * up the others. A change to a record whose creation was refused waits behind it: sent now, the server
 * would answer "not found", which counts as done for most changes, and what the user entered would be lost.
 */
internal class PushStep(
    private val db: LocalDb,
    private val runner: OperationRunner,
) {
    /** Null when the whole line was handled; otherwise why it stopped. */
    suspend fun run(): SyncOutcome? {
        val notCreated = mutableSetOf<String>()
        for (queued in db.outbox.snapshot()) {
            if (queued.state == OperationState.PENDING && queued.operation.touched.none { it in notCreated }) {
                send(queued)?.let { return it }
            }
            if (queued.operation.isCreate() && isFailed(queued.seq)) notCreated += queued.operation.touched
        }
        return null
    }

    private fun isFailed(seq: Long): Boolean =
        db.outbox.snapshot().any { it.seq == seq && it.state == OperationState.FAILED }

    /** Null when the line may carry on. */
    private suspend fun send(queued: QueuedOperation): SyncOutcome? {
        val failure = runner.run(queued.operation).exceptionOrNull()
        if (failure == null) {
            db.outbox.complete(queued.seq)
            return null
        }
        return when (val error = failure.asAppError()) {
            AppError.Network -> SyncOutcome.Offline

            AppError.Unauthorized -> SyncOutcome.Unauthorized

            is AppError.Server, AppError.Unknown -> when {
                // The proxy's "server not answering" codes are an outage, not a verdict on this change.
                error is AppError.Server && error.code in NOT_ANSWERING -> SyncOutcome.Offline

                retryLater(queued.seq, queued.attempts, error) -> null

                else -> SyncOutcome.Offline
            }

            else -> {
                db.outbox.fail(queued.seq, reasonOf(error))
                null
            }
        }
    }

    /**
     * A 5xx or an answer nobody understands may pass; it gets a few tries, and then it is treated as a
     * refusal. A 4xx is the server saying no for good. Returns whether the line may carry on.
     */
    private suspend fun retryLater(
        seq: Long,
        attempts: Int,
        error: AppError,
    ): Boolean {
        val permanent = error is AppError.Server && error.code in CLIENT_ERRORS
        return when {
            permanent -> {
                db.outbox.fail(seq, reasonOf(error))
                true
            }

            attempts + 1 >= MAX_ATTEMPTS -> {
                db.outbox.fail(seq, reasonOf(error))
                true
            }

            else -> {
                db.outbox.recordAttempt(seq)
                false
            }
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 5
        val CLIENT_ERRORS = 400..499
        val NOT_ANSWERING = setOf(502, 503, 504)
    }
}

/** A short word for why a change was refused; the screen that lists them puts it in the user's language. */
internal fun reasonOf(error: AppError): String = when (error) {
    is AppError.Validation -> "validation"

    AppError.NotFound -> "not_found"

    is AppError.PhotoRejected -> when (error.reason) {
        PhotoRejection.TOO_LARGE -> "photo_too_large"
        PhotoRejection.QUOTA_EXCEEDED -> "storage_full"
        PhotoRejection.UNSUPPORTED_TYPE -> "photo_unsupported"
    }

    is AppError.Server -> "server_${error.code}"

    AppError.Network, AppError.Unauthorized, AppError.Unknown -> "unknown"
}
