package ru.prorabprime.designsystem.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar as MaterialTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * The bar of every screen: the brand's green with white text and icons, by day and by night, so the
 * screen's title is always the strongest line on it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    /** For a bar laid over a picture, where the theme's own colors would not read. */
    containerColor: Color? = null,
    contentColor: Color? = null,
) {
    val container = containerColor ?: topBarColor()
    val content = contentColor ?: Color.White
    MaterialTopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = container,
            scrolledContainerColor = container,
            navigationIconContentColor = content,
            titleContentColor = content,
            actionIconContentColor = content,
        ),
    )
}

/** The bar's own color, for what has to continue it, such as the strip beside a centered column. */
@Composable
fun topBarColor(): Color = MaterialTheme.colorScheme.let {
    if (it.background.luminance() < DARK_LUMINANCE) it.primaryContainer else it.primary
}

private const val DARK_LUMINANCE = 0.5f
