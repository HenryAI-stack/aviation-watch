package com.henryai.aviationwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.henryai.aviationwatch.enginetime.EngineLogStore
import com.henryai.aviationwatch.enginetime.EnginetimeSync
import com.henryai.aviationwatch.ui.AviationWatchApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Flights recorded offline are uploaded as soon as there is a connection.
        if (EngineLogStore.get(this).pendingCount.value > 0) EnginetimeSync.schedule(this)
        setContent { AviationWatchApp() }
    }
}
