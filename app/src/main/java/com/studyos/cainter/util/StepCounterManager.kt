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
    private const val KEY_REBOOT_OFFSET = "key_reboot_offset"
    private const val KEY_LAST_STEPS = "key_last_steps"
    private const val KEY_TRACKING_ENABLED = "key_step_tracking_enabled"

    private var sensorManager: SensorManager? = null
    private var stepCounterSensor: Sensor? = null
    private var stepDetectorSensor: Sensor? = null
    private var isRegistered = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var appContext: Context? = null

    private var lastStepTimestamp = 0L
    private var walkingStepsAccumulator = 0
    private var runningStepsAccumulator = 0

    @Volatile
    var currentTodaySteps: Int = 0

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

    fun isTrackingEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_TRACKING_ENABLED, true)
    }

    fun setTrackingEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_TRACKING_ENABLED, enabled).apply()
        if (enabled) {
            startTracking(context)
        } else {
            stopTracking(context)
        }
    }

    fun getTodaySteps(context: Context): Int {
        if (currentTodaySteps > 0) return currentTodaySteps
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = DateUtils.getTodayDateString()
        val savedDate = prefs.getString(KEY_BASELINE_DATE, "") ?: ""
        return if (savedDate == today) {
            prefs.getInt(KEY_LAST_STEPS, 0)
        } else {
            0
        }
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext
        if (sensorManager == null) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            stepDetectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        }
        if (isTrackingEnabled(context)) {
            startTracking(context)
        } else {
            stopTracking(context)
        }
    }

    fun startTracking(context: Context) {
        val appCtx = context.applicationContext
        appContext = appCtx
        if (sensorManager == null) {
            sensorManager = appCtx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            stepDetectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        }
        startListening()
        try {
            StepTrackingService.startService(appCtx)
        } catch (_: Exception) {}
    }

    fun stopTracking(context: Context) {
        stopListening()
        try {
            StepTrackingService.stopService(context.applicationContext)
        } catch (_: Exception) {}
    }

    fun startListening() {
        val sm = sensorManager ?: return
        val sensor = stepCounterSensor ?: stepDetectorSensor ?: return
        if (!isRegistered) {
            try {
                // SENSOR_DELAY_NORMAL allows hardware batching for maximum battery efficiency
                sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
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
        if (event == null) return
        val context = appContext ?: return

        if (!isTrackingEnabled(context)) {
            stopListening()
            return
        }

        val today = DateUtils.getTodayDateString()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val todayTotalSteps: Int

        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val totalHardwareSteps = event.values[0].toInt()
            val savedDate = prefs.getString(KEY_BASELINE_DATE, "") ?: ""
            var baseline = prefs.getInt(KEY_BASELINE_STEPS, -1)
            var offsetBeforeReboot = prefs.getInt(KEY_REBOOT_OFFSET, 0)

            if (savedDate != today) {
                // New day: reset baseline and clear reboot offset
                baseline = totalHardwareSteps
                offsetBeforeReboot = 0
                walkingStepsAccumulator = 0
                runningStepsAccumulator = 0
                prefs.edit()
                    .putString(KEY_BASELINE_DATE, today)
                    .putInt(KEY_BASELINE_STEPS, baseline)
                    .putInt(KEY_REBOOT_OFFSET, 0)
                    .putInt(KEY_LAST_STEPS, 0)
                    .apply()
            } else if (baseline == -1) {
                baseline = totalHardwareSteps
                prefs.edit().putInt(KEY_BASELINE_STEPS, baseline).apply()
            } else if (totalHardwareSteps < baseline) {
                // Phone rebooted mid-day: hardware counter reset to 0!
                // Preserve previous steps walked earlier today as an offset!
                offsetBeforeReboot = prefs.getInt(KEY_LAST_STEPS, 0)
                baseline = totalHardwareSteps
                prefs.edit()
                    .putInt(KEY_BASELINE_STEPS, baseline)
                    .putInt(KEY_REBOOT_OFFSET, offsetBeforeReboot)
                    .apply()
            }

            val hardwareDelta = (totalHardwareSteps - baseline).coerceAtLeast(0)
            todayTotalSteps = offsetBeforeReboot + hardwareDelta
        } else if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
            val savedDate = prefs.getString(KEY_BASELINE_DATE, "") ?: ""
            var current = if (savedDate == today) prefs.getInt(KEY_LAST_STEPS, 0) else 0
            if (savedDate != today) {
                prefs.edit().putString(KEY_BASELINE_DATE, today).apply()
            }
            current += 1
            todayTotalSteps = current
        } else {
            return
        }

        currentTodaySteps = todayTotalSteps
        prefs.edit().putInt(KEY_LAST_STEPS, todayTotalSteps).apply()

        // Cadence estimation
        val now = System.currentTimeMillis()
        if (lastStepTimestamp > 0L) {
            val deltaMs = now - lastStepTimestamp
            // If steps occurring faster than 450ms apart (~135-150 steps/min) -> running
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

        // Update ongoing background notification
        try {
            StepTrackingService.updateNotification(context, todayTotalSteps)
        } catch (_: Exception) {}

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
