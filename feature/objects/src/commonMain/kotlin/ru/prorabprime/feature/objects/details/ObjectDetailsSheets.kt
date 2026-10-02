package ru.prorabprime.feature.objects.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import ru.prorabprime.designsystem.theme.Spacing
import ru.prorabprime.domain.model.ObjectStatus
import ru.prorabprime.feature.objects.components.label
import ru.prorabprime.feature.objects.resources.Res
import ru.prorabprime.feature.objects.resources.objectdetails_pick_photos
import ru.prorabprime.feature.objects.resources.objectdetails_status_title
import ru.prorabprime.feature.objects.resources.objectdetails_take_photo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StatusSheet(
    current: ObjectStatus?,
    onPick: (ObjectStatus) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            stringResource(Res.string.objectdetails_status_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = Spacing.m),
        )
        ObjectStatus.entries.forEach { status ->
            ListItem(
                headlineContent = { Text(stringResource(status.label)) },
                trailingContent = if (status == current) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else {
                    null
                },
                modifier = Modifier.clickable { onPick(status) },
            )
        }
        Spacer(Modifier.height(Spacing.l))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhotoSourceSheet(
    onTakePhoto: () -> Unit,
    onPickPhotos: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        ListItem(
            headlineContent = { Text(stringResource(Res.string.objectdetails_take_photo)) },
            leadingContent = { Icon(Icons.Default.Add, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onTakePhoto),
        )
        ListItem(
            headlineContent = { Text(stringResource(Res.string.objectdetails_pick_photos)) },
            leadingContent = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onPickPhotos).padding(bottom = Spacing.l),
        )
    }
}
