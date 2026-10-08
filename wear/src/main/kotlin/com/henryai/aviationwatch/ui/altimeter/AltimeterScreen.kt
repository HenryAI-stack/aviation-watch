package com.henryai.aviationwatch.ui.altimeter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import androidx.wear.compose.material3.TextButtonDefaults
import com.henryai.aviationwatch.core.Atmosphere
import com.henryai.aviationwatch.core.Units
import com.henryai.aviationwatch.data.AltimeterSettings
import com.henryai.aviationwatch.sensors.PressureSensor
import com.henryai.aviationwatch.ui.common.ScrollingScreen
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Barometric altimeter. Indicated altitude uses the pilot-set QNH; pressure
 * altitude always uses 1013.25 hPa. Note: in a pressurised cabin the watch
 * measures cabin altitude, not aircraft altitude. QNH can also be set from a METAR
 * on the weather page.
 */
@Composable
fun AltimeterScreen() {
    val context = LocalContext.current
    val available = remember(context) { PressureSensor.isAvailable(context) }
    val pressure by remember(context) { PressureSensor.readings(context) }
        .collectAsStateWithLifecycle(initialValue = null)
    val qnh by AltimeterSettings.qnhHpa.collectAsStateWithLifecycle()

    ScrollingScreen {
        item { ListHeader { Text("Altimeter") } }
        item {
            val p = pressure
            Text(
                text = when {
                    !available -> "No barometer"
                    p == null -> "---- ft"
                    else -> "${Atmosphere.altitudeFeet(p.toDouble(), qnh).roundToInt()} ft"
                },
                style = MaterialTheme.typography.numeralSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { AltimeterSettings.adjust(-1.0) },
                    colors = TextButtonDefaults.filledTonalTextButtonColors(),
                ) { Text("−") }
                Text(
                    text = String.format(Locale.ROOT, "QNH\n%.0f\n%.2f\"", qnh, Units.hpaToInHg(qnh)),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                )
                TextButton(
                    onClick = { AltimeterSettings.adjust(1.0) },
                    colors = TextButtonDefaults.filledTonalTextButtonColors(),
                ) { Text("+") }
            }
        }
        item {
            val p = pressure
            Text(
                text = if (p == null) {
                    "PA ---- ft"
                } else {
                    String.format(
                        Locale.ROOT,
                        "PA %d ft\n%.1f hPa",
                        Atmosphere.altitudeFeet(p.toDouble()).roundToInt(),
                        p,
                    )
                },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
