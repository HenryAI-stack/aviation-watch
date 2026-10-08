package com.henryai.aviationwatch.core

import org.junit.Assert.assertEquals
import org.junit.Test

class AirportDetailsCsvTest {
    private val runways = """
        ident,designator,length_ft,width_ft,surface,lighted
        LOAN,09/27,3609,98,GRASS,0
        LOWW,11/29,11811,148,ASP,1
        LOWW,16/34,11483,148,ASP,1
        LOWZ,08/26,2165,,ASP,0
    """.trimIndent()

    private val frequencies = """
        ident,type,description,mhz
        LOAN,INFO,Wiener Neustadt Info,124.25
        LOWW,ATIS,"ATIS, arrival",121.73
        LOWW,TWR,WIEN TOWER,119.4
    """.trimIndent()

    @Test
    fun runwaysForOneAirport() {
        val result = AirportDetailsCsv.runwaysFor("LOWW", runways.lineSequence())
        assertEquals(listOf("11/29", "16/34"), result.map { it.designator })
        assertEquals(11811, result[0].lengthFt)
        assertEquals(true, result[0].lighted)
        assertEquals(null, AirportDetailsCsv.runwaysFor("LOWZ", runways.lineSequence()).single().widthFt)
        assertEquals(emptyList<Runway>(), AirportDetailsCsv.runwaysFor("XXXX", runways.lineSequence()))
    }

    @Test
    fun frequenciesWithQuotedDescription() {
        val result = AirportDetailsCsv.frequenciesFor("LOWW", frequencies.lineSequence())
        assertEquals(listOf("ATIS", "TWR"), result.map { it.type })
        assertEquals("ATIS, arrival", result[0].description)
        assertEquals("119.400", result[1].display)
    }

    @Test
    fun iataColumnIsOptional() {
        val csv = "ident,name,type,lat,lon,elevation_ft,iata\nLOWW,Vienna,LARGE,48.1,16.5,600,VIE\nLOAN,Ost,SMALL,47.8,16.2,896,\n"
        val airports = AirportCsv.parse(csv.lineSequence())
        assertEquals("VIE", airports[0].iata)
        assertEquals(null, airports[1].iata)
    }
}
