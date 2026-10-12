package ru.prorabprime.feature.journal

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import ru.prorabprime.domain.model.FieldProblem
import ru.prorabprime.ui.DialogModel
import ru.prorabprime.ui.UiText

class JournalStateHolderTest {

    private val holder = JournalStateHolder()
    private val state get() = holder.state.value

    private val dialog = DialogModel.Confirmation(
        title = UiText.Raw("t"),
        message = UiText.Raw("m"),
        confirmLabel = UiText.Raw("ok"),
        destructive = true,
    )

    @Test
    fun `it starts loading with nothing to show`() {
        assertThat(state.status).isEqualTo(JournalStatus.Loading)
        assertThat(state.journal).isNull()
    }

    @Test
    fun `showing the log is content, and an error or loading replaces only the status`() {
        holder.showJournal(JournalUi())
        assertThat(state.status).isEqualTo(JournalStatus.Content)

        holder.showError(UiText.Raw("нет сети"))
        assertThat(state.status).isEqualTo(JournalStatus.Error(UiText.Raw("нет сети")))
        assertThat(state.journal).isEqualTo(JournalUi())

        holder.showLoading()
        assertThat(state.status).isEqualTo(JournalStatus.Loading)
    }

    @Test
    fun `typing clears the problem, and sending empties the field`() {
        holder.setDraftError(FieldProblem.REQUIRED)
        holder.setDraft("Привезли плитку")
        assertThat(state.draftError).isNull()

        holder.setSending(true)
        holder.draftSent()

        assertThat(state.draft).isEmpty()
        assertThat(state.isSending).isFalse()
    }

    @Test
    fun `the form is edited only while it is open`() {
        holder.editForm { it.copy(text = "ignored") }
        assertThat(state.editor).isNull()

        holder.openEditor(NoteEditorUi("n1", "Было"))
        holder.editForm { it.copy(text = "Стало") }
        assertThat(state.editor?.text).isEqualTo("Стало")

        holder.closeEditor()
        assertThat(state.editor).isNull()
    }

    @Test
    fun `a dialog remembers what it is about until it is closed`() {
        holder.setDialog(dialog, "n1")
        assertThat(state.dialog).isEqualTo(dialog)
        assertThat(state.pendingDeleteId).isEqualTo("n1")

        holder.setDialog(null)
        assertThat(state.dialog).isNull()
        assertThat(state.pendingDeleteId).isNull()
    }
}
