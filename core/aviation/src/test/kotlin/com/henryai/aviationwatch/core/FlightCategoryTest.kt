package com.henryai.aviationwatch.core

import org.junit.Assert.assertEquals
import org.junit.Test

class FlightCategoryTest {
    @Test
    fun categories() {
        assertEquals(FlightCategory.VFR, FlightCategory.from(null, 10.0))
        assertEquals(FlightCategory.VFR, FlightCategory.from(3500, 6.0))
        assertEquals(FlightCategory.MVFR, FlightCategory.from(3000, 10.0))
        assertEquals(FlightCategory.MVFR, FlightCategory.from(5000, 5.0))
        assertEquals(FlightCategory.IFR, FlightCategory.from(800, 10.0))
        assertEquals(FlightCategory.IFR, FlightCategory.from(null, 2.0))
        assertEquals(FlightCategory.LIFR, FlightCategory.from(200, 10.0))
        assertEquals(FlightCategory.LIFR, FlightCategory.from(5000, 0.5))
    }

    @Test
    fun worseOfCeilingAndVisibilityWins() {
        assertEquals(FlightCategory.LIFR, FlightCategory.from(2000, 0.25))
    }
}
