package ru.prorabprime.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// The brand's green and white (the client's own profile and logo): white surfaces and text on
// green in the dark theme, green on white in the light one. Every role is set, so no Material
// baseline purple shows through in containers or surfaces.
private val BrandGreen = Color(0xFF0B7A3B)
private val BrandGreenDeep = Color(0xFF065C2B)
private val BrandGreenSoft = Color(0xFF4CC27F)

private val LightColors = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5F2E0),
    onPrimaryContainer = Color(0xFF033A1A),
    secondary = Color(0xFF3F6B50),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1F1E7),
    onSecondaryContainer = Color(0xFF0E2A1A),
    tertiary = Color(0xFF5B6B3A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE6EDCB),
    onTertiaryContainer = Color(0xFF1A2205),
    background = Color.White,
    onBackground = Color(0xFF111A14),
    surface = Color.White,
    onSurface = Color(0xFF111A14),
    surfaceVariant = Color(0xFFE3EEE7),
    onSurfaceVariant = Color(0xFF3A4A41),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF6FAF7),
    surfaceContainer = Color(0xFFEFF5F1),
    surfaceContainerHigh = Color(0xFFE8F0EA),
    surfaceContainerHighest = Color(0xFFE1EBE4),
    outline = Color(0xFF5F7267),
    outlineVariant = Color(0xFFBFD0C5),
)

private val DarkColors = darkColorScheme(
    primary = BrandGreenSoft,
    onPrimary = Color(0xFF00391A),
    primaryContainer = BrandGreen,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFB7CCBF),
    onSecondary = Color(0xFF213329),
    secondaryContainer = BrandGreenDeep,
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFFCDD9A6),
    onTertiary = Color(0xFF2D3510),
    tertiaryContainer = Color(0xFF444F25),
    onTertiaryContainer = Color(0xFFE9F2C5),
    background = Color(0xFF0C130F),
    onBackground = Color.White,
    surface = Color(0xFF0C130F),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF34443B),
    onSurfaceVariant = Color(0xFFC9D8CF),
    surfaceContainerLowest = Color(0xFF080D0A),
    surfaceContainerLow = Color(0xFF131C16),
    surfaceContainer = Color(0xFF18221C),
    surfaceContainerHigh = Color(0xFF212C25),
    surfaceContainerHighest = Color(0xFF2B372F),
    outline = Color(0xFF8FA398),
    outlineVariant = Color(0xFF34443B),
)

/** Colors that have no Material role. */
object ProrabColors {
    private val LinkLight = Color(0xFF1A5FC8)
    private val LinkDark = Color(0xFF8AB4F8)
    private val BadgeLight = Color(0xFFD92D20)
    private val BadgeDark = Color(0xFFFF6B5E)

    /** Blue for anything that opens something (a phone number, a link): the color people read as a link. */
    val link: Color
        @Composable get() = if (isDark()) LinkDark else LinkLight

    /** Red for a dot that says "something is switched on here", such as an active filter. */
    val badge: Color
        @Composable get() = if (isDark()) BadgeDark else BadgeLight

    @Composable
    private fun isDark() = MaterialTheme.colorScheme.background.luminance() < DARK_LUMINANCE

    private const val DARK_LUMINANCE = 0.5f
}

/** One corner radius for everything that has corners, so a card, a button and a field match. */
object Corners {
    val m = 8.dp
    val l = 12.dp
}

private val BaseTypography = Typography()

private fun TextStyle.sized(
    size: Int,
    line: Int,
    weight: FontWeight,
): TextStyle = copy(fontSize = size.sp, lineHeight = line.sp, fontWeight = weight)

/**
 * The type scale, as size/line height in sp: screen title 24/28, section 20/24, card title 16/20, text 16/20,
 * secondary text 14/20, label 14/16 and 12/16, footnote 10/12. Headings and labels a weight heavier than the
 * default, so a screen's structure is easy to see.
 */
private val ProrabTypography = Typography(
    headlineSmall = BaseTypography.headlineSmall.sized(24, 28, FontWeight.SemiBold),
    titleLarge = BaseTypography.titleLarge.sized(20, 24, FontWeight.SemiBold),
    titleMedium = BaseTypography.titleMedium.sized(16, 20, FontWeight.SemiBold),
    titleSmall = BaseTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = BaseTypography.bodyLarge.sized(16, 20, FontWeight.Normal),
    bodyMedium = BaseTypography.bodyMedium.sized(14, 20, FontWeight.Normal),
    labelLarge = BaseTypography.labelLarge.sized(14, 16, FontWeight.SemiBold),
    labelMedium = BaseTypography.labelMedium.sized(12, 16, FontWeight.Medium),
    labelSmall = BaseTypography.labelSmall.sized(10, 12, FontWeight.Normal),
)

private val ProrabShapes = Shapes(
    extraSmall = RoundedCornerShape(Corners.m),
    small = RoundedCornerShape(Corners.m),
    medium = RoundedCornerShape(Corners.m),
    large = RoundedCornerShape(Corners.l),
    extraLarge = RoundedCornerShape(Corners.l),
)

@Composable
fun ProrabTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = ProrabShapes,
        typography = ProrabTypography,
        content = content,
    )
}
