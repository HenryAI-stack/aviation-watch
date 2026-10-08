package com.henryai.aviationwatch.ui.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.Text
import com.henryai.aviationwatch.ui.Destination
import com.henryai.aviationwatch.ui.common.ScrollingScreen
import com.henryai.aviationwatch.ui.theme.AviationWatchTheme

@Composable
fun HomeScreen(onOpen: (Destination) -> Unit) {
    ScrollingScreen {
        item { ListHeader { Text("Aviation") } }
        Destination.entries.forEach { destination ->
            item {
                Button(
                    onClick = { onOpen(destination) },
                    modifier = Modifier.fillMaxWidth(),
                    secondaryLabel = { Text(destination.subtitle) },
                    colors = if (destination.implemented) {
                        ButtonDefaults.buttonColors()
                    } else {
                        ButtonDefaults.filledTonalButtonColors()
                    },
                ) { Text(destination.title) }
            }
        }
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    AviationWatchTheme { HomeScreen(onOpen = {}) }
}
