package com.henryai.aviationwatch.core

import org.junit.Assert.assertEquals
import org.junit.Test

class AtmosphereTest {
    @Test
    fun standardPressureIsSeaLevel() {
        assertEquals(0.0, Atmosphere.altitudeFeet(1013.25), 0.01)
    }

    @Test
    fun isaTableValues() {
        // ISA: 5000 ft ≈ 843.1 hPa, 10000 ft ≈ 696.8 hPa.
        assertEquals(5000.0, Atmosphere.altitudeFeet(843.07), 10.0)
        assertEquals(10000.0, Atmosphere.altitudeFeet(696.82), 10.0)
    }

    @Test
    fun qnhCorrection() {
        // Roughly 27-30 ft per hPa near sea level.
        val diff = Atmosphere.altitudeFeet(1000.0, qnhHpa = 1023.0) -
            Atmosphere.altitudeFeet(1000.0, qnhHpa = 1013.25)
        assertEquals(9.75 * 27.5, diff, 15.0)
    }

    @Test
    fun pressureRoundTrip() {
        val p = Atmosphere.pressureAt(1500.0, qnhHpa = 1020.0)
        assertEquals(1500.0, Atmosphere.altitudeMeters(p, qnhHpa = 1020.0), 0.01)
    }

    @Test
    fun densityAltitude() {
        // ISA conditions: density altitude equals pressure altitude.
        assertEquals(5000.0, Atmosphere.densityAltitudeFt(5000.0, 5.1), 0.1)
        // ISA + 20 °C at sea level ≈ +2400 ft.
        assertEquals(2400.0, Atmosphere.densityAltitudeFt(0.0, 35.0), 0.1)
    }
}
