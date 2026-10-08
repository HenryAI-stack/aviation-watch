package com.henryai.aviationwatch.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AirportCsvTest {
    @Test
    fun splitsQuotedFields() {
        assertEquals(
            listOf("LOAN", "Wiener Neustadt, \"Ost\"", "SMALL", "47.8", "16.2", "896"),
            AirportCsv.splitLine("LOAN,\"Wiener Neustadt, \"\"Ost\"\"\",SMALL,47.8,16.2,896"),
        )
    }

    @Test
    fun parsesRowsAndSkipsHeaderAndGarbage() {
        val csv = """
            ident,name,type,lat,lon,elevation_ft
            LOWW,Vienna International Airport,LARGE,48.11030,16.56970,600
            BAD,Broken row,SMALL,not-a-number,16.0,100
            XXXX,No elevation,SEAPLANE_BASE,10.0,20.0,
            OUT,Out of range,SMALL,95.0,0.0,0
        """.trimIndent()
        val airports = AirportCsv.parse(csv.lineSequence())
        assertEquals(listOf("LOWW", "XXXX"), airports.map { it.ident })
        assertEquals(Airport.Type.LARGE, airports[0].type)
        assertEquals(600, airports[0].elevationFt)
        assertEquals(Airport.Type.SEAPLANE_BASE, airports[1].type)
        assertNull(airports[1].elevationFt)
    }
}
