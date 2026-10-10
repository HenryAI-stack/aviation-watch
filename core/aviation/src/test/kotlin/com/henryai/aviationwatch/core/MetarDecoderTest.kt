package com.henryai.aviationwatch.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MetarDecoderTest {
    @Test
    fun europeanMetar() {
        val m = MetarDecoder.decode("METAR LOWW 081720Z 29012G25KT 250V320 9999 -SHRA FEW020CB BKN035 12/07 Q1018 NOSIG")!!
        assertEquals("LOWW", m.station)
        assertEquals(8, m.day)
        assertEquals(17, m.hour)
        assertEquals(20, m.minute)
        assertEquals(Wind(290, 12, 25, 250, 320), m.wind)
        assertEquals(10_000.0, m.visibilityM!!, 0.0)
        assertEquals(listOf("-SHRA"), m.weather)
        assertEquals(listOf(CloudLayer(CloudLayer.Cover.FEW, 2000, "CB"), CloudLayer(CloudLayer.Cover.BKN, 3500)), m.clouds)
        assertEquals(3500, m.ceilingFt)
        assertEquals(12, m.temperatureC)
        assertEquals(7, m.dewpointC)
        assertEquals(1018.0, m.qnhHpa!!, 0.0)
        assertEquals(FlightCategory.VFR, m.flightCategory)
    }

    @Test
    fun cavokAndNegativeTemperatures() {
        val m = MetarDecoder.decode("LOWI 080950Z VRB02KT CAVOK M03/M08 Q1031")!!
        assertTrue(m.cavok)
        assertNull(m.wind!!.directionTrue)
        assertEquals(-3, m.temperatureC)
        assertEquals(-8, m.dewpointC)
        assertTrue(m.clouds.isEmpty())
        assertEquals(FlightCategory.VFR, m.flightCategory)
    }

    @Test
    fun usMetarWithFractionalVisibilityAndAltimeter() {
        val m = MetarDecoder.decode("KSFO 081756Z 28015KT 1 1/2SM BR OVC006 14/13 A2992 RMK AO2 SLP132")!!
        assertEquals(1.5 * Metar.METERS_PER_SM, m.visibilityM!!, 0.1)
        assertEquals(listOf("BR"), m.weather)
        assertEquals(600, m.ceilingFt)
        assertEquals(1013.2, m.qnhHpa!!, 0.1)
        assertEquals(FlightCategory.IFR, m.flightCategory)
    }

    @Test
    fun lowConditionsAndMetersPerSecond() {
        val m = MetarDecoder.decode("UUEE 080600Z 18005MPS 0400 R24L/0550N FG VV002 02/02 Q1009")!!
        assertEquals(10, m.wind!!.speedKt) // 5 m/s ≈ 9.7 kt
        assertEquals(400.0, m.visibilityM!!, 0.0)
        assertEquals(listOf("FG"), m.weather)
        assertEquals(200, m.ceilingFt)
        assertEquals(FlightCategory.LIFR, m.flightCategory)
    }

    @Test
    fun ignoresTrendAndRemarks() {
        val m = MetarDecoder.decode("EDDM 081720Z 07008KT 9999 SCT040 15/05 Q1020 TEMPO 3000 TSRA BKN010CB")!!
        assertEquals(listOf(CloudLayer(CloudLayer.Cover.SCT, 4000)), m.clouds)
        assertTrue(m.weather.isEmpty())
        assertNull(m.ceilingFt)
    }

    @Test
    fun calmAndAutoReport() {
        val m = MetarDecoder.decode("LOAN 081720Z AUTO 00000KT 9999 NCD 10/04 Q1019")!!
        assertTrue(m.wind!!.isCalm)
        assertTrue(m.clouds.isEmpty())
        assertFalse(m.cavok)
    }

    @Test
    fun rejectsGarbage() {
        assertNull(MetarDecoder.decode(""))
        assertNull(MetarDecoder.decode("No data"))
    }

    @Test
    fun observationTimeAcrossMonthBoundary() {
        val m = MetarDecoder.decode("LOWW 312350Z 29010KT 9999 FEW030 10/05 Q1015")!!
        // Report from 31 Oct 23:50Z, read on 1 Nov 00:10Z.
        assertEquals(Instant.parse("2026-10-31T23:50:00Z"), m.observedAt(Instant.parse("2026-11-01T00:10:00Z")))
        // Same-day report.
        val n = MetarDecoder.decode("LOWW 081720Z 29010KT 9999 FEW030 10/05 Q1015")!!
        assertEquals(Instant.parse("2026-10-08T17:20:00Z"), n.observedAt(Instant.parse("2026-10-08T17:35:00Z")))
    }
}
