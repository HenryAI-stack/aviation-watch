package com.henryai.aviationwatch.ui.search

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.henryai.aviationwatch.core.Airport
import com.henryai.aviationwatch.core.AirportSearch
import com.henryai.aviationwatch.data.AirportRepository
import com.henryai.aviationwatch.ui.common.ScrollingScreen
import com.henryai.aviationwatch.ui.common.rememberTextInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Airport search by ICAO ident, IATA code or name, entered with the system keyboard or voice. */
@Composable
fun SearchScreen(onOpenAirport: (String) -> Unit) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var autoLaunched by rememberSaveable { mutableStateOf(false) }

    val launchInput = rememberTextInput("ICAO, IATA or name") { query = it }
    // Open the keyboard straight away the first time the screen is shown.
    LaunchedEffect(Unit) {
        if (!autoLaunched && query.isEmpty()) {
            autoLaunched = true
            launchInput()
        }
    }

    val airports by produceState<List<Airport>?>(initialValue = null, context) {
        value = AirportRepository.airports(context)
    }
    val results by produceState(initialValue = emptyList<Airport>(), query, airports) {
        val all = airports
        value = if (all.isNullOrEmpty() || query.isBlank()) {
            emptyList()
        } else {
            withContext(Dispatchers.Default) { AirportSearch.search(query, all) }
        }
    }

    val all = airports
    ScrollingScreen {
        item { ListHeader { Text("Airports") } }
        item {
            Button(
                onClick = launchInput,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.filledTonalButtonColors(),
            ) { Text(if (query.isEmpty()) "Search…" else "“$query”") }
        }
        when {
            all == null -> item { Hint("Loading airports…") }
            all.isEmpty() -> item { Hint("No airport database in this build. See docs/SETUP.md.") }
            query.isNotEmpty() && results.isEmpty() -> item { Hint("No match") }
            else -> results.forEach { airport ->
                item {
                    Button(
                        onClick = { onOpenAirport(airport.ident) },
                        modifier = Modifier.fillMaxWidth(),
                        secondaryLabel = {
                            Text(airport.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                    ) { Text(airport.ident + (airport.iata?.let { " · $it" } ?: "")) }
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
}
