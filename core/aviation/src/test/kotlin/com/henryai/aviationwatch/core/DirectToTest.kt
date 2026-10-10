package com.henryai.aviationwatch.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DirectToTest {
    // Target due north of the origin along the prime meridian.
    private val target = Airport("TGT", "Target", Airport.Type.SMALL, GeoPoint(1.0, 0.0), 0)
    private val dto = DirectTo(origin = GeoPoint(0.0, 0.0), target = target)

    @Test
    fun onCourseSolution() {
        val s = dto.solve(GeoPoint(0.5, 0.0), groundSpeedKt = 120.0, trackTrue = 0.0)
        assertEquals(0.0, s.desiredTrackTrue, 1e-6)
        assertEquals(0.0, s.bearingTrue, 1e-6)
        assertEquals(30.0, s.distanceNm, 0.1)
        assertEquals(0.0, s.crossTrackNm, 1e-6)
        assertEquals(900.0, s.eteSeconds!!.toDouble(), 5.0) // 30 NM at 120 kt = 15 min
        assertEquals(0.0, s.bearingRelativeToTrack!!, 1e-6)
    }

    @Test
    fun rightOfCourseNeedsLeftTurnAndNeedleLeft() {
        val s = dto.solve(GeoPoint(0.5, 0.02), groundSpeedKt = 100.0, trackTrue = 0.0)
        assertEquals(1.2, s.crossTrackNm, 0.05) // 0.02° lon at equator
        assert(s.bearingRelativeToTrack!! < 0) { "should turn left" }
        assert(Cdi.deflection(s.crossTrackNm) < 0) { "needle should be left" }
    }

    @Test
    fun noTrackNoRelativeBearing() {
        val s = dto.solve(GeoPoint(0.5, 0.0), groundSpeedKt = null, trackTrue = null)
        assertNull(s.bearingRelativeToTrack)
        assertNull(s.eteSeconds)
    }

    @Test
    fun cdiClampsAndScales() {
        assertEquals(-1.0, Cdi.deflection(5.0), 0.0)
        assertEquals(1.0, Cdi.deflection(-5.0), 0.0)
        assertEquals(-0.25, Cdi.deflection(0.5), 1e-9)
        assertEquals(Cdi.TERMINAL_FULL_SCALE_NM, Cdi.fullScaleFor(12.0), 0.0)
        assertEquals(Cdi.ENROUTE_FULL_SCALE_NM, Cdi.fullScaleFor(80.0), 0.0)
    }

    @Test
    fun signedAngleDifference() {
        assertEquals(20.0, Navigation.signedAngleDifference(350.0, 10.0), 1e-9)
        assertEquals(-20.0, Navigation.signedAngleDifference(10.0, 350.0), 1e-9)
        assertEquals(180.0, Navigation.signedAngleDifference(0.0, 180.0), 1e-9)
    }

    @Test
    fun formatting() {
        assertEquals("045°", AviationFormat.heading(45.2))
        assertEquals("360°", AviationFormat.heading(0.3))
        assertEquals("360°", AviationFormat.heading(359.7))
        assertEquals("14:05", AviationFormat.duration(845))
        assertEquals("1:05h", AviationFormat.duration(3900))
        assertEquals("4.3", AviationFormat.distanceNm(4.26))
        assertEquals("43", AviationFormat.distanceNm(42.6))
        assertEquals("290°12G25kt", AviationFormat.wind(Wind(290, 12, 25)))
        assertEquals("VRB03kt", AviationFormat.wind(Wind(null, 3)))
        assertEquals("Calm", AviationFormat.wind(Wind(0, 0)))
        assertEquals("005°08kt", AviationFormat.wind(Wind(5, 8)))
    }
}
