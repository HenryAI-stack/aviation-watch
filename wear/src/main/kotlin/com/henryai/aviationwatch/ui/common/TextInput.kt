package com.henryai.aviationwatch.ui.common

import android.app.RemoteInput
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.wear.input.RemoteInputIntentHelper

private const val INPUT_KEY = "text_input"

/**
 * Returns a function that opens the system keyboard / voice input with [label] and
 * delivers non-blank, trimmed text to [onResult].
 */
@Composable
fun rememberTextInput(label: String, onResult: (String) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val text = result.data
            ?.let { RemoteInput.getResultsFromIntent(it) }
            ?.getCharSequence(INPUT_KEY)
            ?.toString()
            ?.trim()
        if (!text.isNullOrEmpty()) onResult(text)
    }
    return remember(launcher, label) {
        {
            val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
            val remoteInput = RemoteInput.Builder(INPUT_KEY).setLabel(label).build()
            RemoteInputIntentHelper.putRemoteInputsExtra(intent, listOf(remoteInput))
            launcher.launch(intent)
        }
    }
}
