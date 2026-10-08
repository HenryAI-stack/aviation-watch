package com.henryai.aviationwatch.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RunwayWindTest {
    @Test
    fun components() {
        val straight = RunwayWind.components(windFromTrue = 290.0, windSpeedKt = 10.0, runwayHeadingTrue = 290.0)
        assertEquals(10.0, straight.headwindKt, 1e-9)
        assertEquals(0.0, straight.crosswindKt, 1e-9)
        val fromRight = RunwayWind.components(windFromTrue = 320.0, windSpeedKt = 20.0, runwayHeadingTrue = 290.0)
        assertEquals(17.3, fromRight.headwindKt, 0.1)
        assertEquals(10.0, fromRight.crosswindKt, 0.1)
        val tail = RunwayWind.components(windFromTrue = 110.0, windSpeedKt = 10.0, runwayHeadingTrue = 290.0)
        assertEquals(-10.0, tail.headwindKt, 1e-9)
    }

    @Test
    fun endsFromDesignators() {
        assertEquals(
            listOf(RunwayEnd("08L", 80), RunwayEnd("26R", 260)),
            RunwayWind.ends(Runway("08L/26R", 13123, 197, "CON", true)),
        )
        assertEquals(listOf(RunwayEnd("36", 360)), RunwayWind.ends(Runway("36/H1", null, null, "", false)))
    }

    @Test
    fun bestRunwayUsesVariation() {
        val runways = listOf(Runway("11/29", 11811, 148, "ASP", true), Runway("16/34", 11483, 148, "ASP", true))
        val best = RunwayWind.best(runways, Wind(300, 15), magneticVariation = 5.0)!!
        assertEquals("29", best.end.name)
        // Runway 29 is 295° true with 5° E variation: wind 5° from the right.
        assertEquals(1.3, best.components.crosswindKt, 0.1)
        assertNull(RunwayWind.best(runways, Wind(null, 3), 5.0))
        assertNull(RunwayWind.best(runways, Wind(0, 0), 5.0))
    }

    @Test
    fun pressureAltitude() {
        assertEquals(0.0, Atmosphere.pressureAltitudeFt(0.0, 1013.25), 0.5)
        // Low pressure raises pressure altitude by roughly 27-28 ft/hPa near sea level.
        assertEquals(1000.0 + 10.25 * 27.7, Atmosphere.pressureAltitudeFt(1000.0, 1003.0), 15.0)
    }
}
