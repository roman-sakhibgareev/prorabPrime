package ru.prorabprime.feature.finance

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ru.prorabprime.domain.model.JournalCash
import ru.prorabprime.domain.model.JournalFilter
import ru.prorabprime.domain.model.JournalPeriod
import ru.prorabprime.domain.model.ObjectId
import ru.prorabprime.domain.model.Payment
import ru.prorabprime.domain.model.PaymentSide
import ru.prorabprime.ui.DialogModel
import ru.prorabprime.ui.UiText

@Immutable
internal sealed interface FinanceJournalStatus {
    // Declared most-likely first; every `when` over this mirrors the order.
    data object Content : FinanceJournalStatus

    data object Loading : FinanceJournalStatus

    data class Error(
        val message: UiText,
    ) : FinanceJournalStatus
}

/** One line of the journal, every text already formatted; the payment stays for opening the form. */
@Immutable
internal data class JournalRowUi(
    val payment: Payment,
    val objectTitle: String,
    /** With its sign: `+ 1 000 ₽` for money in, `− 1 000 ₽` for money out. */
    val amount: String,
    val date: String,
    val isIncome: Boolean,
) {
    val id: String get() = payment.id.value
}

@Immutable
internal data class JournalTotalsUi(
    val income: String = "",
    val expense: String = "",
    val balance: String = "",
    val isDeficit: Boolean = false,
)

/** An object the journal can be narrowed to. */
@Immutable
internal data class JournalObjectUi(
    val id: ObjectId,
    val title: String,
)

@Immutable
internal data class FinanceJournalState(
    val status: FinanceJournalStatus = FinanceJournalStatus.Loading,
    val filter: JournalFilter = JournalFilter(),
    /** After the filter, newest first. */
    val rows: ImmutableList<JournalRowUi> = persistentListOf(),
    /** Of [rows]. */
    val totals: JournalTotalsUi = JournalTotalsUi(),
    /** Before the filter: zero means there are no payments at all. */
    val allCount: Int = 0,
    /** The objects that have a payment, for the object filter. */
    val objects: ImmutableList<JournalObjectUi> = persistentListOf(),
    /** Bumped by a retry so the journal is asked for again. */
    val attempt: Int = 0,
    val editor: FinanceEditorUi? = null,
    val dialog: DialogModel? = null,
    val pendingAction: FinanceAction? = null,
)

internal sealed interface FinanceJournalEvent {
    data object Retry : FinanceJournalEvent

    data class PeriodChanged(
        val period: JournalPeriod,
    ) : FinanceJournalEvent

    /** Null for every object. */
    data class ObjectChosen(
        val objectId: ObjectId?,
    ) : FinanceJournalEvent

    /** Null for both. */
    data class SideChosen(
        val side: PaymentSide?,
    ) : FinanceJournalEvent

    /** Null for both. */
    data class CashChosen(
        val cash: JournalCash?,
    ) : FinanceJournalEvent

    data object FiltersReset : FinanceJournalEvent

    data class RowClicked(
        val paymentId: String,
    ) : FinanceJournalEvent

    /** An event of the payment form that a row opened. */
    data class Form(
        val event: PaymentEvent,
    ) : FinanceJournalEvent

    data object DialogConfirmed : FinanceJournalEvent

    data object DialogDismissed : FinanceJournalEvent
}
