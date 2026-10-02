package ru.prorabprime.domain.usecase

import ru.prorabprime.domain.model.PhotoId
import ru.prorabprime.domain.model.ReceiptInfo
import ru.prorabprime.domain.repository.PhotosRepository

/** Sets the sum and day of a receipt by hand, or (with null) clears them. */
class SetReceiptUseCase(
    private val repository: PhotosRepository,
) {
    suspend operator fun invoke(id: PhotoId, receipt: ReceiptInfo?): Result<Unit> = repository.setReceipt(id, receipt)
}

/** Marks a receipt paid or not paid. */
class SetReceiptPaidUseCase(
    private val repository: PhotosRepository,
) {
    suspend operator fun invoke(id: PhotoId, paid: Boolean): Result<Unit> = repository.setReceiptPaid(id, paid)
}
