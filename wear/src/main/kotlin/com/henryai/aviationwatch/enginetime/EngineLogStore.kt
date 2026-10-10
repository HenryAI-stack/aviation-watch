package com.henryai.aviationwatch.enginetime

import android.content.Context
import com.henryai.aviationwatch.core.EngineEventType
import com.henryai.aviationwatch.core.EngineEvent
import com.henryai.aviationwatch.core.EngineLog
import com.henryai.aviationwatch.core.EnginetimeFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.time.Instant
import java.time.ZoneId

/**
 * Durable engine-time storage on the watch.
 *
 * - The flight in progress lives in SharedPreferences (written synchronously on every
 *   change), so it survives the app being closed, killed or the watch restarting.
 * - A finished flight is written as an Enginetime JSON file into `pending/` and only moved
 *   to `sent/` after Google Drive confirmed the upload. Nothing is deleted before that.
 */
class EngineLogStore private constructor(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val pendingDir = File(context.filesDir, "enginetime/pending").apply { mkdirs() }
    private val sentDir = File(context.filesDir, "enginetime/sent").apply { mkdirs() }

    private val currentState = MutableStateFlow(readCurrent())
    val current: StateFlow<EngineLog> = currentState.asStateFlow()

    private val pendingState = MutableStateFlow(pendingFiles().size)

    /** Number of finished flights waiting for upload. */
    val pendingCount: StateFlow<Int> = pendingState.asStateFlow()

    /** Registration pre-filled for each new flight. */
    var defaultRegistration: String
        get() = prefs.getString(KEY_DEFAULT_REG, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_DEFAULT_REG, value).commit()
        }

    @Synchronized
    fun update(transform: (EngineLog) -> EngineLog) {
        val updated = transform(currentState.value)
        writeCurrent(updated)
        currentState.value = updated
    }

    /**
     * Stores the completed flight as an Enginetime file for upload and starts a new log.
     * Returns the file name, or null if the flight is not complete yet.
     */
    @Synchronized
    fun finish(now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): String? {
        val log = currentState.value.cleaned()
        if (!log.isComplete || !log.hasFlightData) return null
        val name = EnginetimeFormat.filename(log, now, zone)
        val target = File(pendingDir, name)
        val temp = File(pendingDir, "$name.tmp")
        temp.writeText(EnginetimeFormat.toJson(log, now, zone))
        temp.renameTo(target)
        update { EngineLog(registration = log.registration) }
        refreshPending()
        EnginetimeSync.schedule(context)
        return name
    }

    fun pendingFiles(): List<File> = jsonFiles(pendingDir)

    fun sentFiles(): List<File> = jsonFiles(sentDir)

    /** Called by the sync worker after a confirmed upload. */
    @Synchronized
    fun markSent(file: File) {
        file.renameTo(File(sentDir, file.name))
        sentFiles().drop(KEEP_SENT).forEach { it.delete() }
        refreshPending()
    }

    private fun refreshPending() {
        pendingState.value = pendingFiles().size
    }

    private fun jsonFiles(dir: File): List<File> =
        dir.listFiles { f -> f.name.endsWith(".json") }.orEmpty().sortedByDescending { it.lastModified() }

    private fun readCurrent(): EngineLog = EngineLog(
        registration = prefs.getString(KEY_REG, null) ?: defaultRegistration,
        departure = prefs.getString(KEY_DEP, "").orEmpty(),
        destination = prefs.getString(KEY_DEST, "").orEmpty(),
        note = prefs.getString(KEY_NOTE, "").orEmpty(),
        events = EngineEventType.entries.mapNotNull { type ->
            prefs.getLong(timeKey(type), -1L).takeIf { it >= 0 }?.let { EngineEvent(type, Instant.ofEpochMilli(it)) }
        },
    )

    private fun writeCurrent(log: EngineLog) {
        prefs.edit().apply {
            putString(KEY_REG, log.registration)
            putString(KEY_DEP, log.departure)
            putString(KEY_DEST, log.destination)
            putString(KEY_NOTE, log.note)
            EngineEventType.entries.forEach { type ->
                val time = log.timeOf(type)
                if (time != null) putLong(timeKey(type), time.toEpochMilli()) else remove(timeKey(type))
            }
        }.commit()
    }

    private fun timeKey(type: EngineEventType) = "t_${type.id}"

    companion object {
        private const val PREFS = "enginetime"
        private const val KEY_REG = "registration"
        private const val KEY_DEFAULT_REG = "default_registration"
        private const val KEY_DEP = "departure"
        private const val KEY_DEST = "destination"
        private const val KEY_NOTE = "note"
        private const val KEEP_SENT = 50

        @Volatile
        private var instance: EngineLogStore? = null

        fun get(context: Context): EngineLogStore =
            instance ?: synchronized(this) {
                instance ?: EngineLogStore(context.applicationContext).also { instance = it }
            }
    }
}
