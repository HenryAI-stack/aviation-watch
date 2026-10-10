package com.henryai.aviationwatch.ui.engine

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.henryai.aviationwatch.core.AviationFormat
import com.henryai.aviationwatch.enginetime.EngineLogStore
import com.henryai.aviationwatch.enginetime.EnginetimeAuth
import com.henryai.aviationwatch.enginetime.EnginetimeSync
import com.henryai.aviationwatch.ui.common.ScrollingScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val DATE = DateTimeFormatter.ofPattern("dd.MM. HH:mm")

private data class LoggedFlight(val title: String, val detail: String, val synced: Boolean)

/** Flights stored on the watch: waiting for upload (⏳) or already in Enginetime (☁). */
@Composable
fun FlightLogScreen() {
    val context = LocalContext.current
    val store = remember(context) { EngineLogStore.get(context) }
    val pending by store.pendingCount.collectAsStateWithLifecycle()
    val flights by produceState(emptyList<LoggedFlight>(), pending) {
        value = withContext(Dispatchers.IO) {
            store.pendingFiles().mapNotNull { read(it, synced = false) } +
                store.sentFiles().mapNotNull { read(it, synced = true) }
        }
    }
    ScrollingScreen {
        item { ListHeader { Text("Flight Log") } }
        item {
            Text(
                syncStatus(pending, EnginetimeAuth.isConfigured, EnginetimeAuth.signedInEmail(context)),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
        if (pending > 0) {
            item {
                Button(onClick = { EnginetimeSync.schedule(context) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Sync now")
                }
            }
        }
        if (flights.isEmpty()) {
            item { Text("No flights yet", style = MaterialTheme.typography.bodyMedium) }
        }
        flights.forEach { flight ->
            item {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        (if (flight.synced) "☁ " else "⏳ ") + flight.title,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    Text(flight.detail, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

/** Reads the summary of a stored Enginetime file. */
private fun read(file: File, synced: Boolean): LoggedFlight? = try {
    val json = JSONObject(file.readText())
    val events = json.optJSONArray("events")
    val times = buildMap {
        for (i in 0 until (events?.length() ?: 0)) {
            val e = events!!.getJSONObject(i)
            put(e.optString("type"), Instant.parse(e.optString("iso")))
        }
    }
    val on = times["motor_on"]
    val off = times["motor_off"]
    val engine = if (on != null && off != null) AviationFormat.elapsed(Duration.between(on, off)) else "--"
    LoggedFlight(
        title = "${json.optString("registration")} ${json.optString("departure")}→${json.optString("destination")}",
        detail = "${on?.let { DATE.format(it.atZone(ZoneId.systemDefault())) } ?: ""} · engine $engine",
        synced = synced,
    )
} catch (e: IOException) {
    null
} catch (e: JSONException) {
    null
} catch (e: DateTimeParseException) {
    null
}
