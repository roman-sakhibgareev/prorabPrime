package ru.prorabprime.feature.objects.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import org.jetbrains.compose.resources.stringResource
import ru.prorabprime.designsystem.components.InfoCard
import ru.prorabprime.designsystem.components.InfoRow
import ru.prorabprime.designsystem.components.SectionHeader
import ru.prorabprime.designsystem.components.TonalButton
import ru.prorabprime.designsystem.theme.Spacing
import ru.prorabprime.feature.objects.components.StatusChip
import ru.prorabprime.feature.objects.resources.Res
import ru.prorabprime.feature.objects.resources.objectdetails_client
import ru.prorabprime.feature.objects.resources.objectdetails_name
import ru.prorabprime.feature.objects.resources.objectdetails_notes
import ru.prorabprime.feature.objects.resources.objectdetails_open_chat
import ru.prorabprime.feature.objects.resources.objectdetails_phone

/**
 * What the card says about the object, most important first: its status and address as a heading, then the
 * client and the notes, each a titled card. A part with nothing in it is not drawn.
 */
@Composable
internal fun DetailsFields(details: ObjectDetailsUi, onEditStatus: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            StatusChip(details.status, onClick = onEditStatus)
            details.address?.let { Text(it, style = MaterialTheme.typography.titleLarge) }
        }
        if (details.clientName != null || details.clientPhone != null || details.chatLink != null) {
            ClientCard(details)
        }
        details.notes?.let { notes ->
            SectionCard(stringResource(Res.string.objectdetails_notes)) {
                Text(notes, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun ClientCard(details: ObjectDetailsUi) {
    val uriHandler = LocalUriHandler.current
    SectionCard(stringResource(Res.string.objectdetails_client)) {
        details.clientName?.let { InfoRow(stringResource(Res.string.objectdetails_name), it) }
        // Opens the dialer with the number filled in; nothing is called without the user.
        details.clientPhone?.let { phone ->
            InfoRow(
                label = stringResource(Res.string.objectdetails_phone),
                value = phone,
                emphasized = true,
                onClick = { uriHandler.openUri(phone.asTelUri()) },
            )
        }
        details.chatLink?.let { link ->
            TonalButton(onClick = { uriHandler.openUri(link) }) {
                Text(stringResource(Res.string.objectdetails_open_chat))
            }
        }
    }
}

/** A heading with a card under it; [action] is the heading's own control. */
@Composable
internal fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        SectionHeader(title, action = action)
        InfoCard(content = content)
    }
}
