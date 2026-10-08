package com.henryai.aviationwatch.ui.airport

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.henryai.aviationwatch.core.Airport
import com.henryai.aviationwatch.core.Frequency
import com.henryai.aviationwatch.core.Runway
import com.henryai.aviationwatch.data.AirportInfo
import com.henryai.aviationwatch.data.AirportRepository
import com.henryai.aviationwatch.data.DirectToStore
import com.henryai.aviationwatch.data.WeatherRepository
import com.henryai.aviationwatch.ui.common.MessageScreen
import com.henryai.aviationwatch.ui.common.ScrollingScreen

private sealed interface InfoState {
    data object Loading : InfoState
    data object NotFound : InfoState
    data class Loaded(val info: AirportInfo) : InfoState
}

/** Airport page: name, elevation, runways, frequencies, Direct-To and weather. */
@Composable
fun AirportInfoScreen(ident: String, onDirectTo: () -> Unit, onWeather: (String) -> Unit) {
    val context = LocalContext.current
    val state by produceState<InfoState>(initialValue = InfoState.Loading, ident) {
        value = AirportRepository.info(context, ident)?.let { InfoState.Loaded(it) } ?: InfoState.NotFound
    }
    when (val s = state) {
        InfoState.Loading -> MessageScreen(ident, "Loading…")
        InfoState.NotFound -> MessageScreen(ident, "Airport not found in the database.")
        is InfoState.Loaded -> AirportInfoContent(
            info = s.info,
            onDirectTo = {
                // Course origin is set from the first GPS fix on the Direct-To page.
                DirectToStore.activate(s.info.airport, from = null)
                onDirectTo()
            },
            onWeather = { onWeather(s.info.airport.ident) },
        )
    }
}

@Composable
private fun AirportInfoContent(info: AirportInfo, onDirectTo: () -> Unit, onWeather: () -> Unit) {
    val airport = info.airport
    ScrollingScreen {
        item {
            ListHeader {
                Text(airport.ident + (airport.iata?.let { " · $it" } ?: ""), color = MaterialTheme.colorScheme.primary)
            }
        }
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(airport.name, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${typeLabel(airport.type)} · Elev ${airport.elevationFt?.let { "$it ft" } ?: "?"}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item {
            Button(onClick = onDirectTo, modifier = Modifier.fillMaxWidth()) { Text("Direct-To") }
        }
        if (WeatherRepository.hasReports(airport.ident)) {
            item {
                Button(
                    onClick = onWeather,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(),
                ) { Text("Weather") }
            }
        }
        if (info.runways.isNotEmpty()) {
            item { ListHeader { Text("Runways") } }
            info.runways.forEach { runway -> item { RunwayRow(runway) } }
        }
        if (info.frequencies.isNotEmpty()) {
            item { ListHeader { Text("Frequencies") } }
            info.frequencies.forEach { frequency -> item { FrequencyRow(frequency) } }
        }
    }
}

@Composable
private fun RunwayRow(runway: Runway) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(runway.designator.ifEmpty { "—" }, style = MaterialTheme.typography.titleMedium)
        val size = listOfNotNull(runway.lengthFt, runway.widthFt).joinToString("×")
        Text(
            listOf(size.ifEmpty { null }?.let { "$it ft" }, runway.surface.ifEmpty { null }, "LGT".takeIf { runway.lighted })
                .filterNotNull()
                .joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun FrequencyRow(frequency: Frequency) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "${frequency.type} ${frequency.display}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        if (frequency.description.isNotEmpty()) {
            Text(frequency.description, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}

private fun typeLabel(type: Airport.Type): String = when (type) {
    Airport.Type.LARGE -> "Large airport"
    Airport.Type.MEDIUM -> "Medium airport"
    Airport.Type.SMALL -> "Small airfield"
    Airport.Type.HELIPORT -> "Heliport"
    Airport.Type.SEAPLANE_BASE -> "Seaplane base"
    Airport.Type.OTHER -> "Airfield"
}
