package com.henryai.aviationwatch.ui.engine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.henryai.aviationwatch.core.Airport
import com.henryai.aviationwatch.core.AviationFormat
import com.henryai.aviationwatch.core.EngineEventType
import com.henryai.aviationwatch.core.EngineLog
import com.henryai.aviationwatch.core.GeoPoint
import com.henryai.aviationwatch.core.NearestAirports
import com.henryai.aviationwatch.data.AirportRepository
import com.henryai.aviationwatch.data.DirectToStore
import com.henryai.aviationwatch.enginetime.EngineLogStore
import com.henryai.aviationwatch.enginetime.EnginetimeAuth
import com.henryai.aviationwatch.location.LocationSource
import com.henryai.aviationwatch.location.hasLocationPermission
import com.henryai.aviationwatch.ui.common.ScrollingScreen
import com.henryai.aviationwatch.ui.common.rememberTextInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val GO_GREEN = Color(0xFF2E7D32)
private val TAKEOFF_BLUE = Color(0xFF1565C0)
private val STOP_RED = Color(0xFFC62828)
private val TIME = DateTimeFormatter.ofPattern("HH:mm:ss")

/**
 * Engine time logger, compatible with the Enginetime web app: MOTOR AN -> START -> MOTOR AUS
 * with running counters. Departure and destination come from the nearest airport (GPS) and
 * can be edited. Finished flights are stored on the watch and synced to Enginetime.
 */
@Composable
fun EngineTimeScreen(onOpenLog: () -> Unit, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { EngineLogStore.get(context) }
    val log by store.current.collectAsStateWithLifecycle()
    val pending by store.pendingCount.collectAsStateWithLifecycle()
    val fix by remember(context) {
        if (hasLocationPermission(context)) LocationSource.fixes(context) else emptyFlow()
    }.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    var lastSaved by rememberSaveable { mutableStateOf<String?>(null) }

    val now by produceState(Instant.now(), log.events.size) {
        while (true) {
            value = Instant.now()
            delay(1000L - System.currentTimeMillis() % 1000L)
        }
    }

    suspend fun nearestIdent(position: GeoPoint?): String? {
        position ?: return null
        val airports = AirportRepository.airports(context)
        return NearestAirports.find(
            position,
            airports,
            limit = 1,
            types = setOf(Airport.Type.LARGE, Airport.Type.MEDIUM, Airport.Type.SMALL),
        ).firstOrNull()?.airport?.ident
    }

    fun saveIfComplete() {
        val finished = store.current.value
        if (finished.isComplete && finished.cleaned().hasFlightData) {
            val summary = summary(finished.cleaned())
            if (store.finish() != null) lastSaved = summary
        }
    }

    val editRegistration = rememberTextInput("Registration, e.g. OE-KAB") { reg ->
        store.update { it.copy(registration = reg) }
        store.defaultRegistration = reg.uppercase()
        saveIfComplete()
    }
    val editDeparture = rememberTextInput("Departure ICAO") { ident ->
        store.update { it.copy(departure = ident) }
        saveIfComplete()
    }
    val editDestination = rememberTextInput("Destination ICAO") { ident ->
        store.update { it.copy(destination = ident) }
        saveIfComplete()
    }

    fun logNext(type: EngineEventType) {
        val time = Instant.now()
        lastSaved = null
        store.update { it.log(type, time) }
        scope.launch {
            val position = fix?.position
            when (type) {
                EngineEventType.MOTOR_ON ->
                    if (store.current.value.departure.isBlank()) {
                        nearestIdent(position)?.let { ident -> store.update { it.copy(departure = ident) } }
                    }
                EngineEventType.START -> Unit
                EngineEventType.MOTOR_OFF -> {
                    // Where the engine stops is where we landed; fall back to the Direct-To target.
                    val ident = nearestIdent(position) ?: DirectToStore.active.value?.airport?.ident
                    if (ident != null && store.current.value.destination.isBlank()) {
                        store.update { it.copy(destination = ident) }
                    }
                }
            }
            saveIfComplete()
        }
    }

    ScrollingScreen {
        item { ListHeader { Text("Engine Time") } }
        lastSaved?.let { saved ->
            item {
                Text(
                    "✓ Saved $saved",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        item {
            Button(
                onClick = editRegistration,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.filledTonalButtonColors(),
                secondaryLabel = { Text("Registration") },
            ) { Text(log.registration.ifBlank { "Set registration" }) }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = editDeparture,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(),
                ) { Text(log.departure.ifBlank { "FROM" }) }
                Button(
                    onClick = editDestination,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(),
                ) { Text(log.destination.ifBlank { "TO" }) }
            }
        }
        item { Counters(log, now) }
        val next = log.nextEvent
        if (next != null) {
            item {
                Button(
                    onClick = {
                        if (log.registration.isBlank()) editRegistration() else logNext(next)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = colorFor(next), contentColor = Color.White),
                    secondaryLabel = { Text(if (log.registration.isBlank()) "Set registration first" else "Tap to log time") },
                ) { Text(next.label) }
            }
        } else {
            // Complete but something is missing (e.g. no GPS for the destination).
            item {
                Button(
                    onClick = if (log.destination.isBlank()) editDestination else editDeparture,
                    modifier = Modifier.fillMaxWidth(),
                    secondaryLabel = { Text("Needed to save the flight") },
                ) { Text(if (log.destination.isBlank()) "Enter destination" else "Enter departure") }
            }
        }
        log.events.forEach { event ->
            item {
                val local = event.time.atZone(ZoneId.systemDefault())
                val utc = event.time.atZone(ZoneOffset.UTC)
                Text(
                    "${event.type.label}  ${TIME.format(local)} (${TIME.format(utc)}Z)",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (log.events.isNotEmpty()) {
            item {
                Button(
                    onClick = { store.update(EngineLog::undoLast) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(),
                ) { Text("Undo ${log.events.last().type.label}") }
            }
        }
        item {
            Text(
                syncStatus(pending, EnginetimeAuth.isConfigured, EnginetimeAuth.signedInEmail(context)),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Button(
                onClick = onOpenLog,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.filledTonalButtonColors(),
            ) { Text("Flight log") }
        }
        item {
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.filledTonalButtonColors(),
            ) { Text("Settings") }
        }
    }
}

@Composable
private fun Counters(log: EngineLog, now: Instant) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        Counter("ENGINE", log.engineTime(now)?.let(AviationFormat::elapsed) ?: "-:--:--")
        Counter("FLIGHT", log.flightTime(now)?.let(AviationFormat::elapsed) ?: "-:--:--")
    }
}

@Composable
private fun Counter(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}

private fun colorFor(type: EngineEventType): Color = when (type) {
    EngineEventType.MOTOR_ON -> GO_GREEN
    EngineEventType.START -> TAKEOFF_BLUE
    EngineEventType.MOTOR_OFF -> STOP_RED
}

private fun summary(log: EngineLog): String =
    "${log.registration} ${log.departure}→${log.destination} · " +
        (log.engineTime(Instant.now())?.let(AviationFormat::elapsed) ?: "")

internal fun syncStatus(pending: Int, configured: Boolean, account: String?): String = when {
    pending == 0 -> "☁ All flights synced to Enginetime"
    !configured -> "$pending stored on watch · cloud sync not configured in this build"
    account == null -> "$pending stored on watch · sign in under Settings to sync"
    else -> "$pending waiting · syncs when online"
}
