package com.henryai.aviationwatch.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow

/** Static pressure readings in hPa from the watch barometer. */
object PressureSensor {
    fun isAvailable(context: Context): Boolean = sensorManager(context)
        .getDefaultSensor(Sensor.TYPE_PRESSURE) != null

    /** Emits pressure in hPa; empty if the device has no barometer. */
    fun readings(context: Context): Flow<Float> {
        val manager = sensorManager(context)
        val sensor = manager.getDefaultSensor(Sensor.TYPE_PRESSURE) ?: return emptyFlow()
        return callbackFlow {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    trySend(event.values[0])
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            awaitClose { manager.unregisterListener(listener) }
        }
    }

    private fun sensorManager(context: Context) =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
}
