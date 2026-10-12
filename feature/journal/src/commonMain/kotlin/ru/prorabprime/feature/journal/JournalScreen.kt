package ru.prorabprime.feature.journal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ru.prorabprime.designsystem.components.DialogHost
import ru.prorabprime.designsystem.components.ErrorMessage
import ru.prorabprime.designsystem.components.LoadingBox
import ru.prorabprime.designsystem.components.PendingMark
import ru.prorabprime.designsystem.components.TextButton
import ru.prorabprime.designsystem.components.TopAppBar
import ru.prorabprime.designsystem.theme.ProrabTheme
import ru.prorabprime.designsystem.theme.Spacing
import ru.prorabprime.domain.model.LocalDay
import ru.prorabprime.domain.model.ObjectField
import ru.prorabprime.domain.model.ObjectId
import ru.prorabprime.feature.journal.resources.Res
import ru.prorabprime.feature.journal.resources.journal_back
import ru.prorabprime.feature.journal.resources.journal_cancel
import ru.prorabprime.feature.journal.resources.journal_delete
import ru.prorabprime.feature.journal.resources.journal_draft_label
import ru.prorabprime.feature.journal.resources.journal_edit
import ru.prorabprime.feature.journal.resources.journal_empty
import ru.prorabprime.feature.journal.resources.journal_empty_hint
import ru.prorabprime.feature.journal.resources.journal_save
import ru.prorabprime.feature.journal.resources.journal_send
import ru.prorabprime.feature.journal.resources.journal_title
import ru.prorabprime.feature.journal.resources.journal_today
import ru.prorabprime.feature.journal.resources.journal_yesterday

@Composable
fun JournalScreen(
    objectId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: JournalViewModel = koinViewModel(key = "journal-$objectId") { parametersOf(ObjectId(objectId)) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    JournalContent(state, viewModel::onEvent, onBack, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JournalContent(
    state: JournalState,
    onEvent: (JournalEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.journal_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.journal_back))
                    }
                },
            )
        },
        bottomBar = { Composer(state, onEvent) },
    ) { padding ->
        when (val status = state.status) {
            JournalStatus.Content -> state.journal?.let { Log(it, onEvent, Modifier.padding(padding)) }

            JournalStatus.Loading -> LoadingBox(Modifier.padding(padding))

            is JournalStatus.Error -> ErrorMessage(
                status.message,
                onRetry = { onEvent(JournalEvent.Retry) },
                modifier = Modifier.padding(padding),
            )
        }
    }
    state.editor?.let { NoteEditor(it, onEvent) }
    DialogHost(
        dialog = state.dialog,
        onConfirm = { onEvent(JournalEvent.DialogConfirmed) },
        onDismiss = { onEvent(JournalEvent.DialogDismissed) },
    )
}

/** The log reads top to bottom, oldest first, and opens at its newest line. */
@Composable
private fun Log(
    journal: JournalUi,
    onEvent: (JournalEvent) -> Unit,
    modifier: Modifier,
) {
    if (journal.isEmpty) {
        EmptyLog(modifier)
        return
    }
    val listState = rememberLazyListState()
    // One list item per heading and per line.
    val lastIndex = journal.days.sumOf { it.notes.size + 1 } - 1
    LaunchedEffect(journal.count) { listState.scrollToItem(lastIndex) }
    LazyColumn(state = listState, modifier = modifier.fillMaxSize()) {
        journal.days.forEach { day ->
            item(key = "day-${day.day.epochDay}") { DayHeader(day.heading) }
            items(day.notes, key = { it.id }) { note -> NoteRow(note, onEvent) }
        }
    }
}

@Composable
private fun EmptyLog(modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(Res.string.journal_empty), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(Res.string.journal_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DayHeader(heading: DayHeading) {
    val text = when (heading) {
        DayHeading.Today -> stringResource(Res.string.journal_today)
        DayHeading.Yesterday -> stringResource(Res.string.journal_yesterday)
        is DayHeading.Date -> heading.text
    }
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(
            horizontal = Spacing.m,
        ).padding(top = Spacing.m, bottom = Spacing.xs),
    )
}

/** A line of the log: the time it was written, then the text. A tap opens it for changing. */
@Composable
private fun NoteRow(note: NoteUi, onEvent: (JournalEvent) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEvent(JournalEvent.NoteClicked(note.id)) }
            .padding(horizontal = Spacing.m, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Text(
            note.time,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(min = TIME_WIDTH).padding(top = 2.dp),
        )
        Text(note.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (note.isPending) PendingMark(Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun Composer(state: JournalState, onEvent: (JournalEvent) -> Unit) {
    Surface(tonalElevation = 3.dp, modifier = Modifier.imePadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m, vertical = Spacing.s),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value = state.draft,
                onValueChange = { onEvent(JournalEvent.DraftChanged(it)) },
                label = { Text(stringResource(Res.string.journal_draft_label)) },
                isError = state.draftError != null,
                supportingText = state.draftError?.let { { Text(stringResource(it.message)) } },
                maxLines = DRAFT_MAX_LINES,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { onEvent(JournalEvent.SendClicked) },
                enabled = !state.isSending && state.draft.isNotBlank(),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, stringResource(Res.string.journal_send))
            }
        }
    }
}

@Composable
private fun NoteEditor(editor: NoteEditorUi, onEvent: (JournalEvent) -> Unit) {
    val problem = editor.errors[ObjectField.NOTE_TEXT]
    AlertDialog(
        onDismissRequest = { if (!editor.isSaving) onEvent(JournalEvent.EditorDismissed) },
        title = { Text(stringResource(Res.string.journal_edit)) },
        text = {
            OutlinedTextField(
                value = editor.text,
                onValueChange = { onEvent(JournalEvent.EditorTextChanged(it)) },
                isError = problem != null,
                supportingText = problem?.let { { Text(stringResource(it.message)) } },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onEvent(JournalEvent.EditorSaveClicked) }, loading = editor.isSaving) {
                Text(stringResource(Res.string.journal_save))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    onEvent(JournalEvent.DeleteClicked(editor.noteId))
                }, enabled = !editor.isSaving) {
                    Text(stringResource(Res.string.journal_delete), color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = { onEvent(JournalEvent.EditorDismissed) }, enabled = !editor.isSaving) {
                    Text(stringResource(Res.string.journal_cancel))
                }
            }
        },
    )
}

private val TIME_WIDTH = 44.dp
private const val DRAFT_MAX_LINES = 4

@Suppress("MagicNumber")
@Preview
@Composable
private fun JournalContentPreview() {
    ProrabTheme {
        JournalContent(
            state = JournalState(
                status = JournalStatus.Content,
                journal = JournalUi(
                    persistentListOf(
                        JournalDayUi(
                            LocalDay.of(2026, 10, 11),
                            DayHeading.Yesterday,
                            persistentListOf(NoteUi("1", "Стены оштукатурены, маяки поставлены", "17:40")),
                        ),
                        JournalDayUi(
                            LocalDay.of(2026, 10, 12),
                            DayHeading.Today,
                            persistentListOf(
                                NoteUi("2", "Не забыть заказать штукатурку", "09:15"),
                                NoteUi("3", "Встреча с заказчиком завтра в 16:00", "10:02", isPending = true),
                            ),
                        ),
                    ),
                ),
                draft = "Привезли плитку",
            ),
            onEvent = {},
            onBack = {},
        )
    }
}
