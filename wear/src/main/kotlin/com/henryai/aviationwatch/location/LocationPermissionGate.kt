package com.henryai.aviationwatch.location

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.henryai.aviationwatch.ui.common.MessageScreen

/** Shows [content] once location permission is granted, otherwise asks for it. */
@Composable
fun LocationPermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasLocationPermission(context)) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted = hasLocationPermission(context) }

    if (granted) {
        content()
    } else {
        MessageScreen(
            title = "GPS",
            message = "Precise location is needed for Nearest and Direct-To. " +
                "It is only used on the watch.",
            actionLabel = "Allow",
            onAction = {
                launcher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            },
        )
    }
}
