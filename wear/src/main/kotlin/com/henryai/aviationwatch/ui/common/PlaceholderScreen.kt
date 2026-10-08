package com.henryai.aviationwatch.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.henryai.aviationwatch.ui.Destination

/** Stand-in for features that are on the roadmap but not built yet. */
@Composable
fun PlaceholderScreen(destination: Destination) {
    ScrollingScreen {
        item { ListHeader { Text(destination.title) } }
        item {
            Text(
                text = "${destination.subtitle}\n\nPlanned for phase ${destination.phase}.\nSee docs/FEATURES.md",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.body2,
            )
        }
    }
}
