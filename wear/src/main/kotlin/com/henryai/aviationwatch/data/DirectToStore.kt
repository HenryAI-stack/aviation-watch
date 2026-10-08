package com.henryai.aviationwatch.data

import com.henryai.aviationwatch.core.Airport
import com.henryai.aviationwatch.core.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The active Direct-To target. [origin] is where the course line starts; it is
 * null until the first GPS fix when Direct-To was started without one (e.g. from search).
 */
data class DirectToTarget(val airport: Airport, val origin: GeoPoint?)

/** Shared by the Nearest, Airport and Direct-To screens (and later tiles). */
object DirectToStore {
    private val state = MutableStateFlow<DirectToTarget?>(null)
    val active: StateFlow<DirectToTarget?> = state.asStateFlow()

    fun activate(airport: Airport, from: GeoPoint?) {
        state.value = DirectToTarget(airport, from)
    }

    /** Sets the course origin if it is still unknown. */
    fun anchor(origin: GeoPoint) {
        state.update { current ->
            if (current != null && current.origin == null) current.copy(origin = origin) else current
        }
    }

    fun cancel() {
        state.value = null
    }
}
