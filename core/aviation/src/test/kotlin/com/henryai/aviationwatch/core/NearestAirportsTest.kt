package com.henryai.aviationwatch.core

import org.junit.Assert.assertEquals
import org.junit.Test

class NearestAirportsTest {
    private val airports = listOf(
        Airport("LOWW", "Vienna", Airport.Type.LARGE, GeoPoint(48.1103, 16.5697), 600),
        Airport("LOAN", "Wiener Neustadt Ost", Airport.Type.SMALL, GeoPoint(47.8433, 16.2600), 896),
        Airport("LOWG", "Graz", Airport.Type.MEDIUM, GeoPoint(46.9911, 15.4396), 1115),
        Airport("LOXX", "Some Heliport", Airport.Type.HELIPORT, GeoPoint(48.20, 16.37), 560),
    )

    @Test
    fun sortsByDistanceAndFiltersHeliports() {
        val viennaCenter = GeoPoint(48.2082, 16.3738)
        val result = NearestAirports.find(viennaCenter, airports, limit = 2)
        assertEquals(listOf("LOWW", "LOAN"), result.map { it.airport.ident })
    }

    @Test
    fun bearingIsComputed() {
        val result = NearestAirports.find(GeoPoint(47.0, 15.0), airports, limit = 1)
        assertEquals("LOWG", result.single().airport.ident)
        assertEquals(90.0, result.single().bearingTrue, 10.0)
    }
}
