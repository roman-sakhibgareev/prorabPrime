package ru.prorabprime.feature.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import ru.prorabprime.designsystem.theme.Spacing
import ru.prorabprime.feature.map.resources.Res
import ru.prorabprime.feature.map.resources.map_zoom_in
import ru.prorabprime.feature.map.resources.map_zoom_out

/** A round white button that stays readable on any tile. */
@Composable
internal fun MapControlButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MapColors.Surface,
        contentColor = MapColors.Ink,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MapColors.Outline),
        modifier = modifier.size(CONTROL_SIZE),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
internal fun ZoomControls(
    viewport: MapViewport,
    modifier: Modifier = Modifier,
    extra: @Composable () -> Unit = {},
) {
    Column(modifier.padding(Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        MapControlButton(onClick = { viewport.zoomBy(1f) }) {
            Icon(Icons.Default.Add, stringResource(Res.string.map_zoom_in))
        }
        val zoomOut = stringResource(Res.string.map_zoom_out)
        MapControlButton(onClick = { viewport.zoomBy(-1f) }) {
            // The core icon set has a plus but no minus.
            Text("−", fontSize = MINUS_SIZE, modifier = Modifier.semantics { contentDescription = zoomOut })
        }
        extra()
    }
}

/** A pin with a white halo, so it stands out from tiles of any color. */
@Composable
internal fun MapPin(
    size: Dp,
    selected: Boolean,
    description: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier.size(size)) {
        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MapColors.Halo, modifier = Modifier.size(size))
        Icon(
            Icons.Default.LocationOn,
            contentDescription = description,
            tint = if (selected) MapColors.SelectedPin else MapColors.Pin,
            modifier = Modifier.size(size - HALO_WIDTH * 2).align(Alignment.Center),
        )
    }
}

private val CONTROL_SIZE = 44.dp
private val HALO_WIDTH = 3.dp
private val MINUS_SIZE = 24.sp
