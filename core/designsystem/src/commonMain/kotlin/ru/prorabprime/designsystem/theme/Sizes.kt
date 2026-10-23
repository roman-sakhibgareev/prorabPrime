package ru.prorabprime.designsystem.theme

import androidx.compose.ui.unit.dp

/** Sizes shared by several components, so that things standing in one row are as tall as each other. */
object Sizes {
    /** Buttons and the search line; also the minimum touch target. */
    val control = 40.dp

    /** Minimum height of a form field with its name inside: Material's own, so the name and the text both fit. */
    val field = 56.dp
}
