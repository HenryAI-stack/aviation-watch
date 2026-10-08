package com.henryai.aviationwatch.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

/** Avionics-style palette: amber primary on black, with standard flight-category colours. */
object AviationColors {
    val Amber = Color(0xFFFFC107)
    val Cyan = Color(0xFF4DD0E1)
    val Vfr = Color(0xFF4CAF50)
    val Mvfr = Color(0xFF2196F3)
    val Ifr = Color(0xFFF44336)
    val Lifr = Color(0xFFE040FB)
}

private val colors = Colors(
    primary = AviationColors.Amber,
    primaryVariant = Color(0xFFC79100),
    onPrimary = Color.Black,
    secondary = AviationColors.Cyan,
    onSecondary = Color.Black,
)

@Composable
fun AviationWatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(colors = colors, content = content)
}
