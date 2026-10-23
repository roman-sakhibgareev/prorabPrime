package ru.prorabprime.feature.finance

import ru.prorabprime.domain.model.Payment
import ru.prorabprime.ui.DialogModel

/** What the payment form needs of the screen that shows it, so the object's screen and the journal share one form. */
internal interface PaymentFormHost {
    val editor: FinanceEditorUi?

    /** A payment the screen shows, to fill the form with. */
    fun payment(id: String): Payment?

    fun openEditor(editor: FinanceEditorUi)

    /** Applies [transform] to the open form; does nothing when none is open. */
    fun editForm(transform: (FinanceEditorUi) -> FinanceEditorUi)

    fun closeEditor()

    fun askToConfirm(dialog: DialogModel, action: FinanceAction)
}
