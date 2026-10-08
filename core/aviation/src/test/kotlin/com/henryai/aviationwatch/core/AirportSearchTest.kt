package com.henryai.aviationwatch.core

import org.junit.Assert.assertEquals
import org.junit.Test

class AirportSearchTest {
    private fun airport(ident: String, name: String, type: Airport.Type, iata: String? = null) =
        Airport(ident, name, type, GeoPoint(0.0, 0.0), 0, iata)

    private val airports = listOf(
        airport("LOWW", "Vienna International Airport", Airport.Type.LARGE, "VIE"),
        airport("LOWG", "Graz Airport", Airport.Type.MEDIUM, "GRZ"),
        airport("LOAN", "Wiener Neustadt Ost", Airport.Type.SMALL),
        airport("LOWL", "Linz Airport", Airport.Type.MEDIUM, "LNZ"),
        airport("LOAV", "Vöslau", Airport.Type.SMALL),
    )

    @Test
    fun exactIdentFirstThenPrefixBySize() {
        assertEquals("LOWW", AirportSearch.search("loww", airports).first().ident)
        assertEquals(listOf("LOWW", "LOWG", "LOWL"), AirportSearch.search("LOW", airports).map { it.ident })
    }

    @Test
    fun iataAndName() {
        assertEquals("LOWW", AirportSearch.search("vie", airports).first().ident)
        assertEquals(listOf("LOAN"), AirportSearch.search("wien", airports).map { it.ident })
        assertEquals(listOf("LOWW"), AirportSearch.search("vienna", airports).map { it.ident })
    }

    @Test
    fun shortOrEmptyQueries() {
        assertEquals(emptyList<Airport>(), AirportSearch.search("  ", airports))
        // Two letters only match ident prefixes, not names.
        assertEquals(listOf("LOAN", "LOAV"), AirportSearch.search("LOA", airports).map { it.ident })
        assertEquals(emptyList<Airport>(), AirportSearch.search("NZ", airports))
    }
}
