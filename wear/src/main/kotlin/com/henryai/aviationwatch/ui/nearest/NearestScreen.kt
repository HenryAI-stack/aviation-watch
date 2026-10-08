package com.henryai.aviationwatch.ui.nearest

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.Text
import com.henryai.aviationwatch.core.Airport
import com.henryai.aviationwatch.core.AviationFormat
import com.henryai.aviationwatch.core.NearestAirports
import com.henryai.aviationwatch.core.NearestResult
import com.henryai.aviationwatch.data.AirportRepository
import com.henryai.aviationwatch.data.DirectToStore
import com.henryai.aviationwatch.location.Fix
import com.henryai.aviationwatch.location.LocationPermissionGate
import com.henryai.aviationwatch.location.LocationSource
import com.henryai.aviationwatch.ui.common.MessageScreen
import com.henryai.aviationwatch.ui.common.ScrollingScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** "NRST": the closest airports with magnetic bearing and distance. Tap one to go Direct-To. */
@Composable
fun NearestScreen(onDirectTo: () -> Unit) {
    LocationPermissionGate {
        val context = LocalContext.current
        val fix by remember(context) { LocationSource.fixes(context) }
            .collectAsStateWithLifecycle(initialValue = null)
        val airports by produceState<List<Airport>?>(initialValue = null, context) {
            value = AirportRepository.airports(context)
        }
        val nearest by produceState(initialValue = emptyList<NearestResult>(), fix, airports) {
            val position = fix?.position
            val all = airports
            if (position != null && !all.isNullOrEmpty()) {
                value = withContext(Dispatchers.Default) { NearestAirports.find(position, all) }
            }
        }

        val currentFix = fix
        val all = airports
        when {
            all == null -> MessageScreen("Nearest", "Loading airports…")
            all.isEmpty() -> MessageScreen(
                "Nearest",
                "No airport database in this build. See docs/SETUP.md.",
            )
            currentFix == null -> MessageScreen("Nearest", "Waiting for GPS…")
            else -> NearestList(currentFix, nearest) { airport ->
                DirectToStore.activate(from = currentFix.position, airport = airport)
                onDirectTo()
            }
        }
    }
}

@Composable
private fun NearestList(fix: Fix, results: List<NearestResult>, onSelect: (Airport) -> Unit) {
    ScrollingScreen {
        item { ListHeader { Text("Nearest") } }
        results.forEach { result ->
            item {
                Button(
                    onClick = { onSelect(result.airport) },
                    modifier = Modifier.fillMaxWidth(),
                    secondaryLabel = {
                        Text(result.airport.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                ) {
                    Text(
                        "${result.airport.ident}  " +
                            "${AviationFormat.heading(fix.toMagnetic(result.bearingTrue))} " +
                            "${AviationFormat.distanceNm(result.distanceNm)}nm",
                    )
                }
            }
        }
    }
}
