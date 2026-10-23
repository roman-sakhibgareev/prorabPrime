package ru.prorabprime.designsystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** The few icons the core set lacks, drawn from the Material path data. */
object ProrabIcons {

    /** A clock: waiting to be sent. */
    val Schedule: ImageVector by lazy {
        icon(
            "Schedule",
            "M11.99,2C6.47,2 2,6.48 2,12s4.47,10 9.99,10C17.52,22 22,17.52 22,12S17.52,2 11.99,2zM12,20c-4.42,0 " +
                "-8,-3.58 -8,-8s3.58,-8 8,-8 8,3.58 8,8 -3.58,8 -8,8zM12.5,7H11v6l5.25,3.15 0.75,-1.23 -4.5,-2.67z",
        )
    }

    val Camera: ImageVector by lazy {
        icon(
            "Camera",
            "M12,12m-3.2,0a3.2,3.2 0 1,1 6.4,0a3.2,3.2 0 1,1 -6.4,0M9,2L7.17,4H4c-1.1,0 -2,0.9 -2,2v12c0,1.1 " +
                "0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V6c0,-1.1 -0.9,-2 -2,-2h-3.17L15,2H9zM12,17c-2.76,0 -5,-2.24 " +
                "-5,-5s2.24,-5 5,-5 5,2.24 5,5 -2.24,5 -5,5z",
        )
    }

    val CalendarMonth: ImageVector by lazy {
        icon(
            "CalendarMonth",
            "M20,3h-1V1h-2v2H7V1H5v2H4C2.9,3 2,3.9 2,5v16c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V5C22,3.9 21.1,3 20," +
                "3zM20,21H4V8h16V21zM7,10h2v2H7V10zM11,10h2v2h-2V10zM15,10h2v2h-2V10zM7,14h2v2H7V14zM11,14h2v2h-2V14z" +
                "M15,14h2v2h-2V14z",
        )
    }

    val Receipt: ImageVector by lazy {
        icon(
            "Receipt",
            "M18,17H6v-2h12V17zM18,13H6v-2h12V13zM18,9H6V7h12V9zM3,22l1.5,-1.5L6,22l1.5,-1.5L9,22l1.5,-1.5L12," +
                "22l1.5,-1.5L15,22l1.5,-1.5L18,22l1.5,-1.5L21,22V2l-1.5,1.5L18,2l-1.5,1.5L15,2l-1.5,1.5L12,2l-1.5," +
                "1.5L9,2L7.5,3.5L6,2L4.5,3.5L3,2V22z",
        )
    }

    /** Lines of falling length: sorting. */
    val Sort: ImageVector by lazy {
        icon("Sort", "M3,18h6v-2H3V18zM3,6v2h18V6H3zM3,13h12v-2H3V13z")
    }

    /** Three lines of falling length: a filter. */
    val Filter: ImageVector by lazy {
        icon("Filter", "M10,18h4v-2h-4V18zM3,6v2h18V6H3zM6,13h12v-2H6V13z")
    }

    /** Lines of text on a page: a note. */
    val Note: ImageVector by lazy {
        icon("Note", "M3,18h12v-2H3V18zM3,6v2h18V6H3zM3,13h18v-2H3V13z")
    }

    /** A location pin: an address on the map. */
    val Pin: ImageVector by lazy {
        icon(
            "Pin",
            "M12,2C8.13,2 5,5.13 5,9c0,5.25 7,13 7,13s7,-7.75 7,-13c0,-3.87 -3.13,-7 -7,-7zM12,11.5c-1.38,0 " +
                "-2.5,-1.12 -2.5,-2.5s1.12,-2.5 2.5,-2.5 2.5,1.12 2.5,2.5 -1.12,2.5 -2.5,2.5z",
        )
    }

    val Wallet: ImageVector by lazy {
        icon(
            "Wallet",
            "M21,18v1c0,1.1 -0.9,2 -2,2H5c-1.11,0 -2,-0.9 -2,-2V5c0,-1.1 0.89,-2 2,-2h14c1.1,0 2,0.9 2,2v1h-9c" +
                "-1.11,0 -2,0.9 -2,2v8c0,1.1 0.89,2 2,2h9zM12,16h10V8H12v8zM16,13.5c-0.83,0 -1.5,-0.67 -1.5,-1.5s" +
                "0.67,-1.5 1.5,-1.5 1.5,0.67 1.5,1.5 -0.67,1.5 -1.5,1.5z",
        )
    }

    /** Banknotes: payments. */
    val Payments: ImageVector by lazy {
        icon(
            "Payments",
            "M19,14V6c0,-1.1 -0.9,-2 -2,-2H3C1.9,4 1,4.9 1,6v8c0,1.1 0.9,2 2,2h14C18.1,16 19,15.1 19," +
                "14zM10,15c-1.66,0 -3,-1.34 -3,-3s1.34,-3 3,-3 3,1.34 3,3 -1.34,3 -3,3z" +
                "M23,9v11c0,1.1 -0.9,2 -2,2H4v-2h17V9H23z",
        )
    }

    val CheckCircle: ImageVector by lazy {
        icon(
            "CheckCircle",
            "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM10,17l-5,-5 1.41,-1.41L10," +
                "14.17l7.59,-7.59L19,8l-9,9z",
        )
    }

    val Alarm: ImageVector by lazy {
        icon(
            "Alarm",
            "M22,5.72l-4.6,-3.86 -1.29,1.53 4.6,3.86L22,5.72zM7.88,3.39L6.6,1.86 2,5.71l1.29,1.53 4.59,-3.85z" +
                "M12.5,8H11v6l4.75,2.85 0.75,-1.23 -4,-2.37V8zM12,4c-4.97,0 -9,4.03 -9,9s4.02,9 9,9c4.97,0 9,-4.03 " +
                "9,-9s-4.03,-9 -9,-9zM12,20c-3.87,0 -7,-3.13 -7,-7s3.13,-7 7,-7 7,3.13 7,7 -3.13,7 -7,7z",
        )
    }

    private fun icon(name: String, pathData: String): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black)).build()
}
