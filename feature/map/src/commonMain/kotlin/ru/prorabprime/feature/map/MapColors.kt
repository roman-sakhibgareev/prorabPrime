package ru.prorabprime.feature.map

import androidx.compose.ui.graphics.Color

// The tiles are always light and mostly green, whatever the app theme is, so everything drawn on
// them has its own fixed colors: white with a dark outline, never the theme's.
internal object MapColors {
    val Pin = Color(0xFF0B5A2B)
    val SelectedPin = Color(0xFFD32F2F)
    val Halo = Color.White
    val Ink = Color(0xFF14201A)
    val Surface = Color.White
    val Outline = Color(0xFF14201A).copy(alpha = 0.35f)
}
