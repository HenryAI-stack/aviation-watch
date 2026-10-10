package com.henryai.aviationwatch.data

import com.henryai.aviationwatch.core.Atmosphere
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** The altimeter setting, shared so the weather page can set QNH from a METAR. */
object AltimeterSettings {
    private const val MIN_HPA = 900.0
    private const val MAX_HPA = 1100.0
    private val state = MutableStateFlow(Atmosphere.STANDARD_PRESSURE_HPA)
    val qnhHpa: StateFlow<Double> = state.asStateFlow()

    fun setQnh(hpa: Double) {
        state.value = hpa.coerceIn(MIN_HPA, MAX_HPA)
    }

    fun adjust(deltaHpa: Double) {
        state.update { (it + deltaHpa).coerceIn(MIN_HPA, MAX_HPA) }
    }
}
