package com.studyos.cainter.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.studyos.cainter.StudyOSApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object StepCounterManager : SensorEventListener {

    private const val PREFS_NAME = "studyos_step_counter_prefs"
    private const val KEY_BASELINE_STEPS = "key_baseline_steps"
    private const val KEY_BASELINE_DATE = "key_baseline_date"
    private const val KEY_LAST_STEPS = "key_last_steps"

    private var sensorManager: SensorManager? = null
    private var stepCounterSensor: Sensor? = null
    private var isRegistered = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var appContext: Context? = null

    private var lastStepTimestamp = 0L
    private var walkingStepsAccumulator = 0
    private var runningStepsAccumulator = 0

    fun hasActivityRecognitionPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasStepSensor(context: Context): Boolean {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return false
        return sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null ||
                sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext
        if (sensorManager == null) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        }
        startListening()
    }

    fun startListening() {
        val sm = sensorManager ?: return
        val sensor = stepCounterSensor ?: return
        if (!isRegistered) {
            try {
                sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
                isRegistered = true
            } catch (_: Exception) {}
        }
    }

    fun stopListening() {
        if (isRegistered) {
            try {
                sensorManager?.unregisterListener(this)
                isRegistered = false
            } catch (_: Exception) {}
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_STEP_COUNTER) return

        val totalHardwareSteps = event.values[0].toInt()
        val context = appContext ?: return
        val today = DateUtils.getTodayDateString()

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedDate = prefs.getString(KEY_BASELINE_DATE, "") ?: ""
        var baseline = prefs.getInt(KEY_BASELINE_STEPS, -1)

        // Reset baseline on new day or phone reboot
        if (savedDate != today || baseline == -1 || totalHardwareSteps < baseline) {
            baseline = totalHardwareSteps
            prefs.edit()
                .putString(KEY_BASELINE_DATE, today)
                .putInt(KEY_BASELINE_STEPS, baseline)
                .apply()
        }

        val todayTotalSteps = (totalHardwareSteps - baseline).coerceAtLeast(0)

        // Cadence estimation
        val now = System.currentTimeMillis()
        if (lastStepTimestamp > 0L) {
            val deltaMs = now - lastStepTimestamp
            // If steps occurring faster than 400ms apart (~150 steps/min) -> running
            if (deltaMs in 200..450) {
                runningStepsAccumulator++
            } else if (deltaMs in 451..1500) {
                walkingStepsAccumulator++
            }
        }
        lastStepTimestamp = now

        // Estimate distance: ~0.76m walking, ~1.05m running
        val walking = (todayTotalSteps * 0.85f).toInt()
        val running = (todayTotalSteps - walking).coerceAtLeast(0)
        val distanceMeters = (walking * 0.762f) + (running * 1.05f)
        val calories = (walking * 0.04f) + (running * 0.065f)

        scope.launch {
            try {
                val app = context as? StudyOSApplication ?: return@launch
                app.stepRepository.recordSteps(
                    totalSteps = todayTotalSteps,
                    walkingSteps = walking,
                    runningSteps = running,
                    distanceMeters = distanceMeters,
                    caloriesBurned = calories,
                    todayDate = today
                )
            } catch (_: Exception) {}
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
