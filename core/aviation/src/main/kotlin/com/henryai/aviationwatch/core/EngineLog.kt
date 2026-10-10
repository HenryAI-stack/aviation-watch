package com.henryai.aviationwatch.core

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Engine-time events, compatible with the Enginetime web app
 * (github.com/HenryAI-stack/enginetime): same type ids and labels.
 */
enum class EngineEventType(val id: String, val label: String) {
    MOTOR_ON("motor_on", "MOTOR AN"),
    START("start", "START"),
    MOTOR_OFF("motor_off", "MOTOR AUS"),
}

data class EngineEvent(val type: EngineEventType, val time: Instant)

/**
 * One flight's engine log. Events must be logged in order MOTOR AN -> START -> MOTOR AUS,
 * each once, exactly as the Enginetime web app enforces.
 */
data class EngineLog(
    val registration: String = "",
    val departure: String = "",
    val destination: String = "",
    val note: String = "",
    val events: List<EngineEvent> = emptyList(),
) {
    fun timeOf(type: EngineEventType): Instant? = events.firstOrNull { it.type == type }?.time

    /** The next event that may be logged, or null when the log is complete. */
    val nextEvent: EngineEventType?
        get() = EngineEventType.entries.firstOrNull { timeOf(it) == null }

    val isComplete: Boolean get() = nextEvent == null

    /** Logs [type] at [time] if it is the next event in sequence; otherwise returns this unchanged. */
    fun log(type: EngineEventType, time: Instant): EngineLog =
        if (type != nextEvent) this else copy(events = events + EngineEvent(type, time))

    /** Removes the most recent event (undo). */
    fun undoLast(): EngineLog = copy(events = events.dropLast(1))

    /** Engine running time: MOTOR AN until MOTOR AUS, or until [now] while running. */
    fun engineTime(now: Instant): Duration? = between(EngineEventType.MOTOR_ON, now)

    /** Time since take-off: START until MOTOR AUS, or until [now]. */
    fun flightTime(now: Instant): Duration? = between(EngineEventType.START, now)

    /** Uppercased registration and 4-letter idents, as the web app cleans them. */
    fun cleaned(): EngineLog = copy(
        registration = registration.uppercase().replace(Regex("[^A-Z0-9-]"), ""),
        departure = cleanIdent(departure),
        destination = cleanIdent(destination),
    )

    /** Note, defaulting to "VFR DEP→DEST" like the web app's route import. */
    fun noteOrDefault(): String =
        note.ifBlank { if (departure.isNotBlank() && destination.isNotBlank()) "VFR $departure→$destination" else "" }

    /** The web app requires registration, departure, destination and note before saving. */
    val hasFlightData: Boolean
        get() = registration.isNotBlank() && departure.isNotBlank() && destination.isNotBlank() &&
            noteOrDefault().isNotBlank()

    private fun between(from: EngineEventType, now: Instant): Duration? {
        val start = timeOf(from) ?: return null
        val end = timeOf(EngineEventType.MOTOR_OFF) ?: now
        return Duration.between(start, end).let { if (it.isNegative) Duration.ZERO else it }
    }

    private fun cleanIdent(s: String) = s.uppercase().replace(Regex("[^A-Z0-9]"), "").take(4)
}

/** Serialises an [EngineLog] in the Enginetime web app's Google Drive file format (version 2). */
object EnginetimeFormat {
    const val FILE_PREFIX = "enginelog_"
    private val ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)
    private val TIME = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")

    /** `enginelog_YYYYMMDD_HHMM_REG.json`, local time of saving, as the web app names files. */
    fun filename(log: EngineLog, savedAt: Instant, zone: ZoneId): String {
        val reg = log.registration.trim().ifEmpty { "UNKNOWN" }.replace(Regex("[\\s/\\\\]"), "")
        return "$FILE_PREFIX${FILE_STAMP.format(savedAt.atZone(zone))}_$reg.json"
    }

    /** JSON identical in structure to the web app's `buildFlightData()` (pretty-printed, 2 spaces). */
    fun toJson(log: EngineLog, savedAt: Instant, zone: ZoneId): String {
        val events = log.events.joinToString(",\n") { event ->
            val local = event.time.atZone(zone)
            val utc = event.time.atZone(ZoneOffset.UTC)
            """    {
      "id": ${event.time.toEpochMilli()},
      "type": ${quote(event.type.id)},
      "label": ${quote(event.type.label)},
      "iso": ${quote(ISO.format(event.time))},
      "lTime": ${quote(TIME.format(local))},
      "lDate": ${quote(DATE.format(local))},
      "uTime": ${quote(TIME.format(utc))},
      "uDate": ${quote(DATE.format(utc))}
    }"""
        }
        val eventsJson = if (log.events.isEmpty()) "[]" else "[\n$events\n  ]"
        return """{
  "version": 2,
  "savedAt": ${quote(ISO.format(savedAt))},
  "registration": ${quote(log.registration.trim())},
  "departure": ${quote(log.departure.trim())},
  "destination": ${quote(log.destination.trim())},
  "note": ${quote(log.noteOrDefault().trim())},
  "events": $eventsJson
}"""
    }

    /** JSON string literal with RFC 8259 escaping. */
    internal fun quote(s: String): String = buildString {
        append('"')
        for (c in s) {
            when {
                c == '"' -> append("\\\"")
                c == '\\' -> append("\\\\")
                c == '\n' -> append("\\n")
                c == '\r' -> append("\\r")
                c == '\t' -> append("\\t")
                c < ' ' -> append("\\u%04x".format(c.code))
                else -> append(c)
            }
        }
        append('"')
    }
}
