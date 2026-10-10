package com.henryai.aviationwatch.ui.engine

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.henryai.aviationwatch.enginetime.EngineLogStore
import com.henryai.aviationwatch.enginetime.EnginetimeAuth
import com.henryai.aviationwatch.enginetime.EnginetimeSync
import com.henryai.aviationwatch.ui.common.ScrollingScreen
import com.henryai.aviationwatch.ui.common.rememberTextInput

/** Default registration and the Enginetime (Google) account used for sync. */
@Composable
fun SettingsScreen(onSignIn: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { EngineLogStore.get(context) }
    var registration by remember { mutableStateOf(store.defaultRegistration) }
    // Bumped to re-read the account after signing out.
    var accountVersion by remember { mutableIntStateOf(0) }
    val account = remember(accountVersion) { EnginetimeAuth.signedInEmail(context) }
    val editRegistration = rememberTextInput("Registration, e.g. OE-KAB") { value ->
        val reg = value.uppercase()
        store.defaultRegistration = reg
        registration = reg
        // Also use it for the flight in progress if none is set yet.
        store.update { if (it.registration.isBlank()) it.copy(registration = reg) else it }
    }

    ScrollingScreen {
        item { ListHeader { Text("Settings") } }
        item {
            Button(
                onClick = editRegistration,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.filledTonalButtonColors(),
                secondaryLabel = { Text("Default registration") },
            ) { Text(registration.ifBlank { "Not set" }) }
        }
        item { ListHeader { Text("Enginetime sync") } }
        when {
            !EnginetimeAuth.isConfigured -> item {
                Text(
                    "Cloud sync is not configured in this build. Flights are kept on the watch. " +
                        "See docs/ENGINETIME.md.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }
            account == null -> item {
                Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) { Text("Sign in with Google") }
            }
            else -> {
                item {
                    Text(account, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
                item {
                    Button(onClick = { EnginetimeSync.schedule(context) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Sync now")
                    }
                }
                item {
                    Button(
                        onClick = {
                            EnginetimeAuth.signOut(context)
                            accountVersion++
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors(),
                    ) { Text("Sign out") }
                }
            }
        }
    }
}
