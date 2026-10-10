package com.henryai.aviationwatch.ui.engine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.henryai.aviationwatch.enginetime.EnginetimeAuth
import com.henryai.aviationwatch.enginetime.EnginetimeAuth.PollResult
import com.henryai.aviationwatch.enginetime.EnginetimeSync
import com.henryai.aviationwatch.ui.common.MessageScreen
import kotlinx.coroutines.delay
import java.io.IOException

private sealed interface SignInState {
    data object Requesting : SignInState
    data class ShowCode(val url: String, val code: String) : SignInState
    data class Done(val account: String) : SignInState
    data class Failed(val message: String) : SignInState
}

/**
 * Device sign-in: the watch shows a code, the user enters it on the phone at
 * google.com/device and approves Enginetime's Drive access. Needed once.
 */
@Composable
fun SignInScreen() {
    val context = LocalContext.current
    val state by produceState<SignInState>(SignInState.Requesting) {
        if (!EnginetimeAuth.isConfigured) {
            value = SignInState.Failed("Cloud sync is not configured in this build.")
            return@produceState
        }
        value = try {
            val code = EnginetimeAuth.requestDeviceCode()
            value = SignInState.ShowCode(code.verificationUrl, code.userCode)
            var interval = code.intervalSeconds.coerceAtLeast(1)
            val deadline = System.currentTimeMillis() + code.expiresInSeconds * 1000L
            var result: SignInState = SignInState.Failed("The code expired. Please try again.")
            while (System.currentTimeMillis() < deadline) {
                delay(interval * 1000L)
                when (val poll = EnginetimeAuth.poll(context, code)) {
                    PollResult.Pending -> continue
                    PollResult.SlowDown -> interval += 5
                    is PollResult.SignedIn -> {
                        // Upload anything recorded before signing in.
                        EnginetimeSync.schedule(context)
                        result = SignInState.Done(poll.email ?: "Google account")
                        break
                    }
                    PollResult.Denied -> {
                        result = SignInState.Failed("Access was denied on the phone.")
                        break
                    }
                    PollResult.Expired -> break
                    is PollResult.Failed -> {
                        result = SignInState.Failed(poll.message)
                        break
                    }
                }
            }
            result
        } catch (e: IOException) {
            SignInState.Failed(e.message ?: "Network error")
        }
    }

    when (val s = state) {
        SignInState.Requesting -> MessageScreen("Sign in", "Contacting Google…")
        is SignInState.ShowCode -> CodeScreen(s.url, s.code)
        is SignInState.Done -> MessageScreen("Signed in", "${s.account}\n\nEngine times now sync to Enginetime.")
        is SignInState.Failed -> MessageScreen("Sign in", s.message)
    }
}

@Composable
private fun CodeScreen(url: String, code: String) {
    // Keep the code visible while the user types it on the phone.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("On your phone open", style = MaterialTheme.typography.bodySmall)
            Text(
                url.removePrefix("https://").removePrefix("http://"),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text("and enter", style = MaterialTheme.typography.bodySmall)
            Text(
                code,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
