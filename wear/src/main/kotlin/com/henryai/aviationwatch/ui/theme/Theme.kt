package com.henryai.aviationwatch.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import com.henryai.aviationwatch.core.FlightCategory

/** Avionics-style palette: amber primary on black, with standard flight-category colours. */
object AviationColors {
    val Amber = Color(0xFFFFC107)
    val Cyan = Color(0xFF4DD0E1)
    val Magenta = Color(0xFFE040FB)
    val Vfr = Color(0xFF4CAF50)
    val Mvfr = Color(0xFF2196F3)
    val Ifr = Color(0xFFF44336)
    val Lifr = Color(0xFFE040FB)
}

// Fixed palette rather than dynamic watch-face colours: pilots rely on consistent colour meaning.
private val colorScheme = ColorScheme(
    primary = AviationColors.Amber,
    primaryDim = Color(0xFFC79100),
    primaryContainer = Color(0xFF5C4300),
    onPrimary = Color.Black,
    onPrimaryContainer = Color(0xFFFFE08A),
    secondary = AviationColors.Cyan,
    onSecondary = Color.Black,
)

@Composable
fun AviationWatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme, content = content)
}

/** Standard flight-category colours (VFR green, MVFR blue, IFR red, LIFR magenta). */
fun FlightCategory.color(): Color = when (this) {
    FlightCategory.VFR -> AviationColors.Vfr
    FlightCategory.MVFR -> AviationColors.Mvfr
    FlightCategory.IFR -> AviationColors.Ifr
    FlightCategory.LIFR -> AviationColors.Lifr
}
