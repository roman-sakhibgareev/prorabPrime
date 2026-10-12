package ru.prorabprime.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.FilterChip as MaterialFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.prorabprime.designsystem.icons.ProrabIcons
import ru.prorabprime.designsystem.theme.ProrabColors
import ru.prorabprime.designsystem.theme.ProrabTheme

/** A choice among a few: the chosen one is filled with the main color, so it reads at a glance. */
@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    MaterialFilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = colors.primary,
            selectedLabelColor = colors.onPrimary,
        ),
    )
}

/**
 * A small soft-green button that opens something (a sheet, a search): the status chip's look, with an optional
 * icon in front of the label and an arrow after it. [badgeDescription] puts a red dot on the corner and says what
 * it means, for a chip whose sheet has something switched on.
 */
@Composable
fun ActionChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    badgeDescription: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    Box(modifier) {
        Surface(
            onClick = onClick,
            color = colors.primaryContainer,
            contentColor = colors.onPrimaryContainer,
            shape = MaterialTheme.shapes.small,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(label, style = MaterialTheme.typography.labelMedium)
                if (trailingIcon != null) Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
        if (badgeDescription != null) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-3).dp)
                    .size(10.dp)
                    .background(ProrabColors.badge, CircleShape)
                    .border(2.dp, colors.background, CircleShape)
                    .semantics { contentDescription = badgeDescription },
            )
        }
    }
}

@Preview
@Composable
private fun ActionChipPreview() {
    ProrabTheme {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionChip(label = "Фильтры", onClick = {}, icon = ProrabIcons.Filter, badgeDescription = "Включены")
            ActionChip(label = "По дате изменений", onClick = {}, trailingIcon = Icons.Default.ArrowDropDown)
        }
    }
}
