package com.henryai.aviationwatch.ui.clock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val zuluFormat = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneOffset.UTC)
private val zuluDateFormat = DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneOffset.UTC)
private val localFormat = DateTimeFormatter.ofPattern("HH:mm")

/** Zulu (UTC) clock with local time underneath — the pilot's primary time reference. */
@Composable
fun UtcClockScreen() {
    val now by produceState(initialValue = Instant.now()) {
        while (true) {
            // Tick on the second boundary so the display does not drift.
            delay(1000L - System.currentTimeMillis() % 1000L)
            value = Instant.now()
        }
    }
    Scaffold {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("UTC", color = MaterialTheme.colors.secondary)
            Text(
                text = zuluFormat.format(now) + "Z",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary,
            )
            Text(zuluDateFormat.format(now), style = MaterialTheme.typography.caption1)
            Text(
                text = "LCL " + localFormat.format(now.atZone(ZoneId.systemDefault())),
                style = MaterialTheme.typography.title3,
            )
        }
    }
}
