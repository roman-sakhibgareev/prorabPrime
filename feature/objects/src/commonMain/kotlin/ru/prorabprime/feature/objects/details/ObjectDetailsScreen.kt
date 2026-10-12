package ru.prorabprime.feature.objects.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ru.prorabprime.designsystem.components.BusyScreen
import ru.prorabprime.designsystem.components.DialogHost
import ru.prorabprime.designsystem.components.ErrorMessage
import ru.prorabprime.designsystem.components.LoadingBox
import ru.prorabprime.designsystem.components.TonalButton
import ru.prorabprime.designsystem.components.TopAppBar
import ru.prorabprime.designsystem.theme.ProrabColors
import ru.prorabprime.designsystem.theme.ProrabTheme
import ru.prorabprime.designsystem.theme.Spacing
import ru.prorabprime.domain.model.AttachmentKind
import ru.prorabprime.domain.model.ObjectId
import ru.prorabprime.domain.model.ObjectStatus
import ru.prorabprime.feature.objects.components.StatusChip
import ru.prorabprime.feature.objects.components.label
import ru.prorabprime.feature.objects.photos.PhotoCarousel
import ru.prorabprime.feature.objects.photos.rememberPhotoSources
import ru.prorabprime.feature.objects.resources.Res
import ru.prorabprime.feature.objects.resources.objectdetails_add_receipt
import ru.prorabprime.feature.objects.resources.objectdetails_address
import ru.prorabprime.feature.objects.resources.objectdetails_back
import ru.prorabprime.feature.objects.resources.objectdetails_calendar
import ru.prorabprime.feature.objects.resources.objectdetails_client
import ru.prorabprime.feature.objects.resources.objectdetails_delete
import ru.prorabprime.feature.objects.resources.objectdetails_edit
import ru.prorabprime.feature.objects.resources.objectdetails_finance
import ru.prorabprime.feature.objects.resources.objectdetails_journal
import ru.prorabprime.feature.objects.resources.objectdetails_materials
import ru.prorabprime.feature.objects.resources.objectdetails_notes
import ru.prorabprime.feature.objects.resources.objectdetails_open_chat
import ru.prorabprime.feature.objects.resources.objectdetails_phone
import ru.prorabprime.feature.objects.resources.objectdetails_photos
import ru.prorabprime.feature.objects.resources.objectdetails_pick_photos
import ru.prorabprime.feature.objects.resources.objectdetails_receipts
import ru.prorabprime.feature.objects.resources.objectdetails_status_title
import ru.prorabprime.feature.objects.resources.objectdetails_take_photo
import ru.prorabprime.ui.resolve

/** [onOpenPhoto] gets the photo's id, for the full-screen viewer. */
@Composable
fun ObjectDetailsScreen(
    objectId: String,
    onEdit: () -> Unit,
    onOpenPhoto: (photoId: String) -> Unit,
    onOpenGallery: (receipts: Boolean) -> Unit,
    onOpenFinance: () -> Unit,
    onOpenMaterials: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenCalendar: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ObjectDetailsViewModel = koinViewModel(key = objectId) { parametersOf(ObjectId(objectId)) }
    ObjectDetailsScreen(
        onEdit,
        onOpenPhoto,
        onOpenGallery,
        onOpenFinance,
        onOpenMaterials,
        onOpenJournal,
        onOpenCalendar,
        onClose,
        modifier,
        viewModel,
    )
}

@Composable
private fun ObjectDetailsScreen(
    onEdit: () -> Unit,
    onOpenPhoto: (photoId: String) -> Unit,
    onOpenGallery: (receipts: Boolean) -> Unit,
    onOpenFinance: () -> Unit,
    onOpenMaterials: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenCalendar: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier,
    viewModel: ObjectDetailsViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.isClosed) { if (state.isClosed) onClose() }
    val photoSources = rememberPhotoSources(onPicked = {
        viewModel.onEvent(ObjectDetailsEvent.PhotosPicked(it, AttachmentKind.PHOTO))
    })
    val receiptSources = rememberPhotoSources(onPicked = {
        viewModel.onEvent(ObjectDetailsEvent.PhotosPicked(it, AttachmentKind.RECEIPT))
    })
    fun sourcesFor(kind: AttachmentKind) = if (kind == AttachmentKind.PHOTO) photoSources else receiptSources
    ObjectDetailsContent(
        state = state,
        onEvent = viewModel::onEvent,
        onEdit = onEdit,
        onBack = onClose,
        onOpenPhoto = onOpenPhoto,
        onOpenGallery = onOpenGallery,
        onOpenFinance = onOpenFinance,
        onOpenMaterials = onOpenMaterials,
        onOpenJournal = onOpenJournal,
        onOpenCalendar = onOpenCalendar,
        onTakePhoto = { sourcesFor(it).takePhoto() },
        onPickPhotos = { sourcesFor(it).pickFromGallery() },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ObjectDetailsContent(
    state: ObjectDetailsState,
    onEvent: (ObjectDetailsEvent) -> Unit,
    onEdit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenPhoto: (photoId: String) -> Unit = {},
    onOpenGallery: (receipts: Boolean) -> Unit = {},
    onOpenFinance: () -> Unit = {},
    onOpenMaterials: () -> Unit = {},
    onOpenJournal: () -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    onTakePhoto: (AttachmentKind) -> Unit = {},
    onPickPhotos: (AttachmentKind) -> Unit = {},
) {
    var choosingStatus by remember { mutableStateOf(false) }
    val sections = ObjectSections(
        onOpenFinance,
        onOpenMaterials,
        onOpenJournal,
        onOpenCalendar,
        onEditStatus = { choosingStatus = true },
    )
    // Whether the "camera or gallery" sheet is open is view state, like a menu.
    var choosingSourceFor by remember { mutableStateOf<AttachmentKind?>(null) }
    BusyScreen(state.isDeleting, modifier) {
        Scaffold(
            topBar = { DetailsTopBar(state, onEvent, onEdit, onBack) },
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                if (state.isDeleting) LinearProgressIndicator(Modifier.fillMaxWidth())
                DetailsByStatus(state, onEvent, onOpenPhoto, onOpenGallery, sections, onAdd = {
                    choosingSourceFor = it
                })
            }
        }
    }
    DialogHost(
        dialog = state.dialog,
        onConfirm = { onEvent(ObjectDetailsEvent.DialogConfirmed) },
        onDismiss = { onEvent(ObjectDetailsEvent.DialogDismissed) },
    )
    state.contactEditor?.let { ContactEditorDialog(it, onEvent) }
    if (choosingStatus) {
        StatusSheet(
            current = state.details?.status,
            onPick = {
                choosingStatus = false
                onEvent(ObjectDetailsEvent.StatusPicked(it))
            },
            onDismiss = { choosingStatus = false },
        )
    }
    choosingSourceFor?.let { kind ->
        PhotoSourceSheet(
            onTakePhoto = {
                choosingSourceFor = null
                onTakePhoto(kind)
            },
            onPickPhotos = {
                choosingSourceFor = null
                onPickPhotos(kind)
            },
            onDismiss = { choosingSourceFor = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsTopBar(
    state: ObjectDetailsState,
    onEvent: (ObjectDetailsEvent) -> Unit,
    onEdit: () -> Unit,
    onBack: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(state.details?.title.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.objectdetails_back))
            }
        },
        actions = {
            if (state.status == ObjectDetailsStatus.Content) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, stringResource(Res.string.objectdetails_edit))
                }
                IconButton(onClick = {
                    onEvent(ObjectDetailsEvent.DeleteClicked)
                }, enabled = !state.isDeleting) {
                    Icon(Icons.Default.Delete, stringResource(Res.string.objectdetails_delete))
                }
            }
        },
    )
}

@Composable
private fun DetailsBody(
    details: ObjectDetailsUi,
    state: ObjectDetailsState,
    onEvent: (ObjectDetailsEvent) -> Unit,
    onOpenPhoto: (photoId: String) -> Unit,
    onOpenGallery: (receipts: Boolean) -> Unit,
    sections: ObjectSections,
    onAdd: (AttachmentKind) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        FolderTitle(Res.string.objectdetails_photos) { onOpenGallery(false) }
        PhotoCarousel(
            photos = details.photos,
            uploads = state.uploads.filter { it.kind == AttachmentKind.PHOTO }.toImmutableList(),
            onAddClick = { onAdd(AttachmentKind.PHOTO) },
            onPhotoClick = { onOpenPhoto(it.id) },
            onMakeCover = { onEvent(ObjectDetailsEvent.MakeCoverClicked(it.id)) },
            onDelete = { onEvent(ObjectDetailsEvent.DeletePhotoClicked(it.id)) },
            onRetryUpload = { onEvent(ObjectDetailsEvent.RetryUpload(it.image, it.kind)) },
            onDismissUpload = { onEvent(ObjectDetailsEvent.DismissUpload(it.image)) },
        )
        FolderTitle(Res.string.objectdetails_receipts) { onOpenGallery(true) }
        PhotoCarousel(
            photos = details.receipts,
            uploads = state.uploads.filter { it.kind == AttachmentKind.RECEIPT }.toImmutableList(),
            onAddClick = { onAdd(AttachmentKind.RECEIPT) },
            onPhotoClick = { onOpenPhoto(it.id) },
            onMakeCover = {},
            onDelete = { onEvent(ObjectDetailsEvent.DeletePhotoClicked(it.id)) },
            onRetryUpload = { onEvent(ObjectDetailsEvent.RetryUpload(it.image, it.kind)) },
            onDismissUpload = { onEvent(ObjectDetailsEvent.DismissUpload(it.image)) },
            canMakeCover = false,
            addLabel = Res.string.objectdetails_add_receipt,
        )
        DetailsFields(details, sections.onEditStatus)
        SectionButtons(sections)
        ContactsSection(
            contacts = details.contacts,
            onAdd = { onEvent(ObjectDetailsEvent.AddContactClicked) },
            onEdit = { onEvent(ObjectDetailsEvent.EditContactClicked(it.id)) },
        )
    }
}

@Composable
private fun DetailsByStatus(
    state: ObjectDetailsState,
    onEvent: (ObjectDetailsEvent) -> Unit,
    onOpenPhoto: (photoId: String) -> Unit,
    onOpenGallery: (receipts: Boolean) -> Unit,
    sections: ObjectSections,
    onAdd: (AttachmentKind) -> Unit,
) {
    when (val status = state.status) {
        ObjectDetailsStatus.Content -> state.details?.let { details ->
            DetailsBody(details, state, onEvent, onOpenPhoto, onOpenGallery, sections, onAdd)
        }

        ObjectDetailsStatus.Loading -> LoadingBox()

        is ObjectDetailsStatus.Error -> ErrorMessage(status.message, onRetry = {
            onEvent(ObjectDetailsEvent.Retry)
        })
    }
}

/**
 * Where the card leads besides itself: the object's finance, its materials checklist, its journal and its
 * calendar; and the status sheet.
 */
internal data class ObjectSections(
    val onOpenFinance: () -> Unit,
    val onOpenMaterials: () -> Unit,
    val onOpenJournal: () -> Unit,
    val onOpenCalendar: () -> Unit,
    val onEditStatus: () -> Unit,
)

private val SectionButtonHeight = 56.dp

/** Two rows of two equal buttons, so the grid never ends up ragged. */
@Composable
private fun SectionButtons(sections: ObjectSections) {
    val rows = listOf(
        listOf(
            Res.string.objectdetails_journal to sections.onOpenJournal,
            Res.string.objectdetails_calendar to sections.onOpenCalendar,
        ),
        listOf(
            Res.string.objectdetails_finance to sections.onOpenFinance,
            Res.string.objectdetails_materials to sections.onOpenMaterials,
        ),
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                row.forEach { (label, onClick) ->
                    TonalButton(onClick = onClick, modifier = Modifier.weight(1f).heightIn(min = SectionButtonHeight)) {
                        Text(stringResource(label), textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderTitle(title: StringResource, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}

@Composable
private fun DetailsFields(details: ObjectDetailsUi, onEditStatus: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        StatusChip(details.status, onClick = onEditStatus)
        details.address?.let { Field(stringResource(Res.string.objectdetails_address), it) }
        details.clientName?.let { Field(stringResource(Res.string.objectdetails_client), it) }
        details.clientPhone?.let { phone ->
            // Opens the dialer with the number filled in; nothing is called without the user.
            Field(
                label = stringResource(Res.string.objectdetails_phone),
                value = phone,
                valueColor = ProrabColors.link,
                modifier = Modifier.clickable {
                    uriHandler.openUri(phone.asTelUri())
                },
            )
        }
        details.chatLink?.let { link ->
            TonalButton(onClick = { uriHandler.openUri(link) }) {
                Text(stringResource(Res.string.objectdetails_open_chat))
            }
        }
        details.notes?.let { Field(stringResource(Res.string.objectdetails_notes), it) }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = valueColor)
    }
}

@Preview
@Composable
private fun ObjectDetailsContentPreview() {
    ProrabTheme {
        ObjectDetailsContent(
            state = ObjectDetailsState(
                status = ObjectDetailsStatus.Content,
                details = ObjectDetailsUi(
                    title = "Кухня у Ивановых",
                    address = "ул. Ленина, 1, кв. 5",
                    status = ObjectStatus.IN_PROGRESS,
                    clientName = "Иван",
                    clientPhone = "+7 900 123-45-67",
                    notes = "Ключи у консьержа",
                ),
            ),
            onEvent = {},
            onEdit = {},
            onBack = {},
        )
    }
}
