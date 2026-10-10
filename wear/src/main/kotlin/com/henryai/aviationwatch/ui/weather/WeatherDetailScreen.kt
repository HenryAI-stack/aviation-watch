package com.henryai.aviationwatch.ui.weather

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.henryai.aviationwatch.core.Atmosphere
import com.henryai.aviationwatch.core.AviationFormat
import com.henryai.aviationwatch.core.CloudLayer
import com.henryai.aviationwatch.core.Metar
import com.henryai.aviationwatch.core.RunwayWind
import com.henryai.aviationwatch.core.Units
import com.henryai.aviationwatch.data.AirportInfo
import com.henryai.aviationwatch.data.AirportRepository
import com.henryai.aviationwatch.data.AltimeterSettings
import com.henryai.aviationwatch.data.WeatherRepository
import com.henryai.aviationwatch.location.magneticVariation
import com.henryai.aviationwatch.ui.common.MessageScreen
import com.henryai.aviationwatch.ui.common.ScrollingScreen
import com.henryai.aviationwatch.ui.theme.color
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private sealed interface DetailState {
    data object Loading : DetailState
    data class Loaded(val info: AirportInfo?, val metar: Metar?, val taf: List<String>) : DetailState
    data class Failed(val message: String) : DetailState
}

/** Decoded METAR, runway wind, density altitude and TAF for one station. */
@Composable
fun WeatherDetailScreen(ident: String) {
    val context = LocalContext.current
    var refreshCount by remember { mutableIntStateOf(0) }
    val state by produceState<DetailState>(DetailState.Loading, ident, refreshCount) {
        value = DetailState.Loading
        value = try {
            val force = refreshCount > 0
            DetailState.Loaded(
                info = AirportRepository.info(context, ident),
                metar = WeatherRepository.metars(listOf(ident), force)[ident],
                taf = WeatherRepository.taf(ident, force),
            )
        } catch (e: IOException) {
            DetailState.Failed(e.message ?: "Network error")
        }
    }
    when (val s = state) {
        DetailState.Loading -> MessageScreen(ident, "Loading weather…")
        is DetailState.Failed -> MessageScreen(ident, "Weather unavailable: ${s.message}", "Retry") { refreshCount++ }
        is DetailState.Loaded ->
            if (s.metar == null && s.taf.isEmpty()) {
                MessageScreen(ident, "No current METAR or TAF for this airport.", "Refresh") { refreshCount++ }
            } else {
                WeatherDetail(ident, s, onRefresh = { refreshCount++ })
            }
    }
}

@Composable
private fun WeatherDetail(ident: String, state: DetailState.Loaded, onRefresh: () -> Unit) {
    val metar = state.metar
    var qnhApplied by remember(metar) { mutableStateOf(false) }
    ScrollingScreen {
        item {
            ListHeader {
                Text(ident)
                metar?.let { Text(" ${it.flightCategory.name}", color = it.flightCategory.color()) }
            }
        }
        if (metar != null) {
            item { Caption(observationAge(metar)) }
            metar.wind?.let { wind ->
                item { Field("Wind", AviationFormat.wind(wind) + variableRange(wind.variableFrom, wind.variableTo)) }
            }
            item { Field("Visibility", visibility(metar)) }
            if (metar.weather.isNotEmpty()) item { Field("Weather", metar.weather.joinToString(" ")) }
            item { Field("Clouds", clouds(metar)) }
            item {
                Field(
                    "Temp / Dew",
                    "${metar.temperatureC?.let { "$it°" } ?: "--"} / ${metar.dewpointC?.let { "$it°" } ?: "--"}",
                )
            }
            metar.qnhHpa?.let { qnh ->
                item {
                    Field("QNH", String.format(Locale.ROOT, "%.0f hPa · %.2f\"", qnh, Units.hpaToInHg(qnh)))
                }
            }
            state.info?.let { info -> runwayWind(info, metar)?.let { item { Field("Best runway", it) } } }
            densityAltitude(state.info, metar)?.let { item { Field("Density alt", it) } }
            metar.qnhHpa?.let { qnh ->
                item {
                    Button(
                        onClick = {
                            AltimeterSettings.setQnh(qnh)
                            qnhApplied = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors(),
                    ) { Text(if (qnhApplied) "Altimeter set ✓" else "Set altimeter QNH") }
                }
            }
            item { Raw(metar.raw) }
        }
        if (state.taf.isNotEmpty()) {
            item { ListHeader { Text("TAF") } }
            state.taf.forEach { line -> item { Raw(line) } }
        }
        item {
            Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) { Text("Refresh") }
        }
    }
}

@Composable
private fun Field(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Caption(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
}

@Composable
private fun Raw(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

private fun observationAge(metar: Metar): String {
    val now = Instant.now()
    val observed = metar.observedAt(now) ?: return "Observation time unknown"
    val minutes = Duration.between(observed, now).toMinutes().coerceAtLeast(0)
    val time = String.format(Locale.ROOT, "%02d%02dZ", metar.hour, metar.minute)
    return if (minutes < 120) "$time · $minutes min ago" else "$time · ${minutes / 60} h ago (old)"
}

private fun variableRange(from: Int?, to: Int?): String =
    if (from != null && to != null) String.format(Locale.ROOT, "\n%03d°–%03d°", from, to) else ""

private fun visibility(metar: Metar): String {
    if (metar.cavok) return "CAVOK"
    val meters = metar.visibilityM ?: return "--"
    return when {
        meters >= 10_000 -> "10 km+"
        meters >= 5_000 -> String.format(Locale.ROOT, "%.0f km", meters / 1000)
        else -> "${meters.roundToInt()} m"
    }
}

private fun clouds(metar: Metar): String = when {
    metar.cavok -> "No significant cloud"
    metar.clouds.isEmpty() -> "None reported"
    else -> metar.clouds.joinToString("\n") { layer ->
        val base = layer.baseFt?.let { "$it ft" } ?: "---"
        "${layer.cover.name} $base${layer.type?.let { " $it" } ?: ""}" +
            if (layer.cover == CloudLayer.Cover.VV) " (VV)" else ""
    }
}

private fun runwayWind(info: AirportInfo, metar: Metar): String? {
    val wind = metar.wind ?: return null
    val variation = magneticVariation(info.airport.position)
    val best = RunwayWind.best(info.runways, wind, variation) ?: return null
    val head = best.components.headwindKt.roundToInt()
    val cross = best.components.crosswindKt.roundToInt()
    val headText = if (head >= 0) "$head kt head" else "${-head} kt TAIL"
    val crossText = if (cross == 0) "no X-wind" else "${abs(cross)} kt X ${if (cross > 0) "R" else "L"}"
    val gust = wind.gustKt?.let { " (G$it)" } ?: ""
    return "RWY ${best.end.name}\n$headText · $crossText$gust"
}

private fun densityAltitude(info: AirportInfo?, metar: Metar): String? {
    val elevation = info?.airport?.elevationFt ?: return null
    val temperature = metar.temperatureC ?: return null
    val qnh = metar.qnhHpa ?: return null
    val pressureAltitude = Atmosphere.pressureAltitudeFt(elevation.toDouble(), qnh)
    val densityAltitude = Atmosphere.densityAltitudeFt(pressureAltitude, temperature.toDouble())
    return "${(densityAltitude / 10).roundToInt() * 10} ft"
}
