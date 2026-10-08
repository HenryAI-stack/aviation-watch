package com.henryai.aviationwatch.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationTest {
    private val loww = GeoPoint(48.1103, 16.5697) // Vienna
    private val lowg = GeoPoint(46.9911, 15.4396) // Graz

    @Test
    fun distanceViennaGraz() {
        // Equirectangular hand check: dLat 1.119°, dLon 1.130° * cos(47.55°) -> ~81.3 NM.
        assertEquals(81.3, Navigation.distanceNm(loww, lowg), 0.5)
    }

    @Test
    fun oneDegreeOfLatitudeIsSixtyNm() {
        assertEquals(60.04, Navigation.distanceNm(GeoPoint(10.0, 20.0), GeoPoint(11.0, 20.0)), 0.01)
    }

    @Test
    fun distanceToSelfIsZero() {
        assertEquals(0.0, Navigation.distanceMeters(loww, loww), 1e-6)
    }

    @Test
    fun bearingCardinalDirections() {
        val origin = GeoPoint(0.0, 0.0)
        assertEquals(0.0, Navigation.initialBearing(origin, GeoPoint(1.0, 0.0)), 1e-6)
        assertEquals(90.0, Navigation.initialBearing(origin, GeoPoint(0.0, 1.0)), 1e-6)
        assertEquals(180.0, Navigation.initialBearing(origin, GeoPoint(-1.0, 0.0)), 1e-6)
        assertEquals(270.0, Navigation.initialBearing(origin, GeoPoint(0.0, -1.0)), 1e-6)
    }

    @Test
    fun crossTrackSign() {
        // Course due north along the prime meridian.
        val start = GeoPoint(0.0, 0.0)
        val end = GeoPoint(1.0, 0.0)
        val right = Navigation.crossTrackMeters(start, end, GeoPoint(0.5, 0.01))
        val left = Navigation.crossTrackMeters(start, end, GeoPoint(0.5, -0.01))
        assertTrue(right > 0)
        assertTrue(left < 0)
        assertEquals(1112.0, right, 5.0) // 0.01° of longitude at the equator
    }

    @Test
    fun ete() {
        assertEquals(3600L, Navigation.eteSeconds(100.0, 100.0))
        assertNull(Navigation.eteSeconds(10.0, 0.0))
    }

    @Test
    fun normalize() {
        assertEquals(350.0, Navigation.normalizeDegrees(-10.0), 1e-9)
        assertEquals(10.0, Navigation.normalizeDegrees(370.0), 1e-9)
    }
}
