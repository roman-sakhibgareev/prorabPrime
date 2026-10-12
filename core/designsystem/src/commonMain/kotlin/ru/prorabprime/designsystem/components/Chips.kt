package ru.prorabprime.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
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
import ru.prorabprime.designsystem.theme.Sizes
import ru.prorabprime.designsystem.theme.Spacing

/** A choice among a few, as a pill: the chosen one is filled with the main color, so it reads at a glance. */
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
        modifier = modifier.height(Sizes.control),
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.surfaceContainerHigh,
            selectedContainerColor = colors.primary,
            selectedLabelColor = colors.onPrimary,
        ),
        // A pill with no outline: filled or not is the whole difference.
        border = null,
    )
}

/**
 * A small soft-green button that opens something (a sheet, a search): the status chip's look, with an optional
 * icon in front of the label and an arrow after it. [badgeDescription] puts a red dot on the corner and says what
 * it means, for a chip whose sheet has something switched on. With [iconOnly] the label is dropped, and the
 * chip keeps it as its description, for a row too narrow to spell every button out.
 */
@Composable
fun ActionChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    badgeDescription: String? = null,
    iconOnly: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Box(modifier) {
        Surface(
            onClick = onClick,
            modifier = Modifier.height(Sizes.control).then(
                if (iconOnly) Modifier.semantics { contentDescription = label } else Modifier,
            ),
            color = colors.primaryContainer,
            contentColor = colors.onPrimaryContainer,
            shape = MaterialTheme.shapes.large,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = Spacing.m),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                if (!iconOnly || icon == null) Text(label, style = MaterialTheme.typography.labelLarge)
                if (trailingIcon != null) Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
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
