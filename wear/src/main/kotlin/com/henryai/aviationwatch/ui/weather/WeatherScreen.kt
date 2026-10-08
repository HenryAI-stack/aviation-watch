package com.henryai.aviationwatch.ui.weather

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.henryai.aviationwatch.core.Airport
import com.henryai.aviationwatch.core.AviationFormat
import com.henryai.aviationwatch.core.Metar
import com.henryai.aviationwatch.core.Navigation
import com.henryai.aviationwatch.core.NearestAirports
import com.henryai.aviationwatch.data.AirportRepository
import com.henryai.aviationwatch.data.DirectToStore
import com.henryai.aviationwatch.data.WeatherRepository
import com.henryai.aviationwatch.location.LocationPermissionGate
import com.henryai.aviationwatch.location.LocationSource
import com.henryai.aviationwatch.ui.common.MessageScreen
import com.henryai.aviationwatch.ui.common.ScrollingScreen
import com.henryai.aviationwatch.ui.theme.color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

private const val NEARBY_STATIONS = 10

private data class StationRow(val airport: Airport, val metar: Metar, val distanceNm: Double)

private sealed interface ListState {
    data object Loading : ListState
    data class Loaded(val rows: List<StationRow>) : ListState
    data class Failed(val message: String) : ListState
}

/** Weather at the Direct-To target and the nearest reporting airports, with flight category. */
@Composable
fun WeatherScreen(onOpenStation: (String) -> Unit) {
    LocationPermissionGate {
        val context = LocalContext.current
        val fix by remember(context) { LocationSource.fixes(context) }
            .collectAsStateWithLifecycle(initialValue = null)
        val target by DirectToStore.active.collectAsStateWithLifecycle()
        val airports by produceState<List<Airport>?>(initialValue = null, context) {
            value = AirportRepository.airports(context)
        }
        var refreshCount by remember { mutableIntStateOf(0) }
        // Fetch once a position is known (and on refresh), not on every GPS update.
        val latestFix by rememberUpdatedState(fix)
        val hasFix = fix != null
        val state by produceState<ListState>(ListState.Loading, airports, hasFix, refreshCount) {
            val all = airports
            val position = latestFix?.position
            if (all.isNullOrEmpty() || position == null) return@produceState
            value = ListState.Loading
            value = try {
                val candidates = withContext(Dispatchers.Default) {
                    val nearby = NearestAirports.find(
                        position,
                        all,
                        limit = NEARBY_STATIONS,
                        types = setOf(Airport.Type.LARGE, Airport.Type.MEDIUM),
                    ).map { it.airport }
                    (listOfNotNull(target?.airport) + nearby).distinctBy { it.ident }
                }
                val metars = WeatherRepository.metars(candidates.map { it.ident }, forceRefresh = refreshCount > 0)
                ListState.Loaded(
                    candidates.mapNotNull { airport ->
                        metars[airport.ident]?.let {
                            StationRow(airport, it, Navigation.distanceNm(position, airport.position))
                        }
                    },
                )
            } catch (e: IOException) {
                ListState.Failed(e.message ?: "Network error")
            }
        }

        val all = airports
        when {
            all == null -> MessageScreen("Weather", "Loading airports…")
            all.isEmpty() -> MessageScreen("Weather", "No airport database in this build. See docs/SETUP.md.")
            fix == null -> MessageScreen("Weather", "Waiting for GPS…")
            else -> when (val s = state) {
                ListState.Loading -> MessageScreen("Weather", "Loading METARs…")
                is ListState.Failed -> MessageScreen(
                    "Weather",
                    "Weather unavailable: ${s.message}",
                    actionLabel = "Retry",
                    onAction = { refreshCount++ },
                )
                is ListState.Loaded -> StationList(s.rows, onOpenStation, onRefresh = { refreshCount++ })
            }
        }
    }
}

@Composable
private fun StationList(rows: List<StationRow>, onOpenStation: (String) -> Unit, onRefresh: () -> Unit) {
    ScrollingScreen {
        item { ListHeader { Text("Weather") } }
        if (rows.isEmpty()) {
            item {
                Text(
                    "No METARs from nearby airports.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        rows.forEach { row ->
            item {
                val category = row.metar.flightCategory
                Button(
                    onClick = { onOpenStation(row.airport.ident) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    secondaryLabel = {
                        Text(
                            row.metar.wind?.let(AviationFormat::wind).orEmpty() +
                                " · ${AviationFormat.distanceNm(row.distanceNm)}nm",
                        )
                    },
                ) {
                    Text(row.airport.ident)
                    Text(" ${category.name}", color = category.color())
                }
            }
        }
        item {
            Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) { Text("Refresh") }
        }
    }
}
