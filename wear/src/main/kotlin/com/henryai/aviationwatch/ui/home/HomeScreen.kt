package com.henryai.aviationwatch.ui.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import com.henryai.aviationwatch.ui.Destination
import com.henryai.aviationwatch.ui.common.ScrollingScreen
import com.henryai.aviationwatch.ui.theme.AviationWatchTheme

@Composable
fun HomeScreen(onOpen: (Destination) -> Unit) {
    ScrollingScreen {
        item { ListHeader { Text("Aviation") } }
        Destination.entries.forEach { destination ->
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onOpen(destination) },
                    label = { Text(destination.title) },
                    secondaryLabel = { Text(destination.subtitle) },
                    colors = if (destination.implemented) {
                        ChipDefaults.primaryChipColors()
                    } else {
                        ChipDefaults.secondaryChipColors()
                    },
                )
            }
        }
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    AviationWatchTheme { HomeScreen(onOpen = {}) }
}
