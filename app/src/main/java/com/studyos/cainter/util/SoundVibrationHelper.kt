package com.studyos.cainter.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object SoundVibrationHelper {

    fun playSessionCompleteAlert(context: Context) {
        // Play audio chime
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 700)
        } catch (e: Exception) {
            // Silently fallback if audio hardware is occupied
        }

        // Trigger haptic vibration
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                val vibrationEffect = VibrationEffect.createWaveform(
                    longArrayOf(0, 400, 200, 400),
                    -1
                )
                vibrator?.vibrate(vibrationEffect)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val vibrationEffect = VibrationEffect.createWaveform(
                        longArrayOf(0, 400, 200, 400),
                        -1
                    )
                    vibrator?.vibrate(vibrationEffect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 400, 200, 400), -1)
                }
            }
        } catch (e: Exception) {
            // Silently ignore if vibration permission or hardware unavailable
        }
    }
}
