package com.henryai.aviationwatch.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class EngineLogTest {
    private val on = Instant.parse("2026-10-10T08:15:30.123Z")
    private val start = Instant.parse("2026-10-10T08:24:00Z")
    private val off = Instant.parse("2026-10-10T09:05:10Z")
    private val vienna = ZoneId.of("Europe/Vienna")

    private val flight = EngineLog(registration = "oe-kab ", departure = "loan", destination = "LOWW")
        .cleaned()
        .log(EngineEventType.MOTOR_ON, on)
        .log(EngineEventType.START, start)
        .log(EngineEventType.MOTOR_OFF, off)

    @Test
    fun enforcesSequenceAndOnce() {
        val empty = EngineLog()
        assertEquals(EngineEventType.MOTOR_ON, empty.nextEvent)
        // START before MOTOR AN is ignored, as in the web app.
        assertEquals(empty, empty.log(EngineEventType.START, start))
        val running = empty.log(EngineEventType.MOTOR_ON, on)
        assertEquals(running, running.log(EngineEventType.MOTOR_ON, start))
        assertEquals(EngineEventType.START, running.nextEvent)
        assertTrue(flight.isComplete)
        assertNull(flight.nextEvent)
        assertEquals(EngineEventType.MOTOR_OFF, flight.undoLast().nextEvent)
    }

    @Test
    fun durations() {
        assertEquals(2979L, flight.engineTime(Instant.MAX)!!.seconds)
        assertEquals(Duration.ofSeconds(2470), flight.flightTime(Instant.MAX))
        val running = EngineLog().log(EngineEventType.MOTOR_ON, on)
        assertEquals(60L, running.engineTime(on.plusSeconds(60))!!.seconds)
        assertNull(running.flightTime(on.plusSeconds(60)))
        assertEquals("0:49:39", AviationFormat.elapsed(flight.engineTime(Instant.MAX)!!))
    }

    @Test
    fun cleaningAndDefaultNote() {
        assertEquals("OE-KAB", flight.registration)
        assertEquals("LOAN", flight.departure)
        assertEquals("VFR LOAN→LOWW", flight.noteOrDefault())
        assertTrue(flight.hasFlightData)
        assertFalse(EngineLog(registration = "OE-KAB", departure = "LOAN").hasFlightData)
        assertEquals("Platzrunden", flight.copy(note = "Platzrunden").noteOrDefault())
    }

    @Test
    fun filenameMatchesWebApp() {
        // Saved 09:05 UTC = 11:05 in Vienna (CEST).
        assertEquals("enginelog_20261010_1105_OE-KAB.json", EnginetimeFormat.filename(flight, off, vienna))
        assertEquals("enginelog_20261010_1105_UNKNOWN.json", EnginetimeFormat.filename(EngineLog(), off, vienna))
    }

    @Test
    fun jsonMatchesWebAppFormat() {
        val json = EnginetimeFormat.toJson(flight.copy(events = flight.events.take(1)), off, vienna)
        val expected = """{
  "version": 2,
  "savedAt": "2026-10-10T09:05:10.000Z",
  "registration": "OE-KAB",
  "departure": "LOAN",
  "destination": "LOWW",
  "note": "VFR LOAN→LOWW",
  "events": [
    {
      "id": 1791620130123,
      "type": "motor_on",
      "label": "MOTOR AN",
      "iso": "2026-10-10T08:15:30.123Z",
      "lTime": "10:15:30",
      "lDate": "10.10.2026",
      "uTime": "08:15:30",
      "uDate": "10.10.2026"
    }
  ]
}"""
        assertEquals(expected, json)
        assertTrue(EnginetimeFormat.toJson(EngineLog(), off, vienna).contains("\"events\": []"))
    }

    @Test
    fun jsonEscaping() {
        assertEquals("\"a\\\"b\\\\c\\nd\\u0001\"", EnginetimeFormat.quote("a\"b\\c\nd\u0001"))
    }
}
