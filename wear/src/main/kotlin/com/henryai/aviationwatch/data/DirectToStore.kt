package com.henryai.aviationwatch.data

import com.henryai.aviationwatch.core.Airport
import com.henryai.aviationwatch.core.DirectTo
import com.henryai.aviationwatch.core.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The active Direct-To, shared by the Nearest and Direct-To screens (and later tiles). */
object DirectToStore {
    private val state = MutableStateFlow<DirectTo?>(null)
    val active: StateFlow<DirectTo?> = state.asStateFlow()

    fun activate(from: GeoPoint, airport: Airport) {
        state.value = DirectTo(origin = from, target = airport)
    }

    fun cancel() {
        state.value = null
    }
}
