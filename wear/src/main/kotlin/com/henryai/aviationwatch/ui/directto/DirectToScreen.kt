package com.henryai.aviationwatch.ui.directto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.henryai.aviationwatch.core.AviationFormat
import com.henryai.aviationwatch.core.Cdi
import com.henryai.aviationwatch.core.DirectTo
import com.henryai.aviationwatch.core.NavSolution
import com.henryai.aviationwatch.data.DirectToStore
import com.henryai.aviationwatch.location.Fix
import com.henryai.aviationwatch.location.LocationPermissionGate
import com.henryai.aviationwatch.location.LocationSource
import com.henryai.aviationwatch.ui.common.MessageScreen
import com.henryai.aviationwatch.ui.common.ScrollingScreen
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Direct-To page: bearing pointer, CDI, and BRG/DIS/DTK/XTK/GS/TRK/ETE. Directions are magnetic. */
@Composable
fun DirectToScreen(onOpenNearest: () -> Unit) {
    val active by DirectToStore.active.collectAsStateWithLifecycle()
    val directTo = active
    if (directTo == null) {
        MessageScreen(
            title = "Direct-To",
            message = "No active Direct-To. Pick an airport from Nearest.",
            actionLabel = "Nearest",
            onAction = onOpenNearest,
        )
        return
    }
    LocationPermissionGate {
        val context = LocalContext.current
        val fix by remember(context) { LocationSource.fixes(context) }
            .collectAsStateWithLifecycle(initialValue = null)
        KeepScreenOn()
        val currentFix = fix
        if (currentFix == null) {
            MessageScreen("→ ${directTo.target.ident}", "Waiting for GPS…")
        } else {
            DirectToContent(directTo, currentFix)
        }
    }
}

@Composable
private fun DirectToContent(directTo: DirectTo, fix: Fix) {
    val nav = directTo.solve(fix.position, fix.groundSpeedKt, fix.trackTrue)
    val fullScale = Cdi.fullScaleFor(nav.distanceNm)
    ScrollingScreen {
        item {
            ListHeader {
                Text(
                    "→ ${directTo.target.ident}",
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BearingPointer(nav.bearingRelativeToTrack, Modifier.size(56.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        AviationFormat.distanceNm(nav.distanceNm),
                        style = MaterialTheme.typography.numeralSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text("NM", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        item {
            CdiIndicator(
                deflection = Cdi.deflection(nav.crossTrackNm, fullScale).toFloat(),
                modifier = Modifier.fillMaxWidth().height(28.dp),
            )
        }
        item { DataRow("BRG", magnetic(fix, nav.bearingTrue), "DTK", magnetic(fix, nav.desiredTrackTrue)) }
        item { DataRow("XTK", crossTrack(nav), "ETE", nav.eteSeconds?.let(AviationFormat::duration) ?: "--:--") }
        item {
            DataRow(
                "GS",
                nav.groundSpeedKt?.let { "${it.roundToInt()}kt" } ?: "---",
                "TRK",
                nav.trackTrue?.let { magnetic(fix, it) } ?: "---°",
            )
        }
        item {
            Text(
                "${directTo.target.name}${directTo.target.elevationFt?.let { " · ${it}ft" } ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        item {
            Button(
                onClick = DirectToStore::cancel,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.filledTonalButtonColors(),
            ) { Text("Cancel Direct-To") }
        }
    }
}

@Composable
private fun DataRow(label1: String, value1: String, label2: String, value2: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        DataCell(label1, value1)
        DataCell(label2, value2)
    }
}

@Composable
private fun DataCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

private fun magnetic(fix: Fix, trueDegrees: Double): String = AviationFormat.heading(fix.toMagnetic(trueDegrees))

private fun crossTrack(nav: NavSolution): String {
    val xtk = nav.crossTrackNm
    val side = when {
        abs(xtk) < 0.05 -> ""
        xtk > 0 -> "R"
        else -> "L"
    }
    return String.format(Locale.ROOT, "%.1f%s", abs(xtk), side)
}

/** Keeps the display on while the Direct-To page is visible, like a panel instrument. */
@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}
