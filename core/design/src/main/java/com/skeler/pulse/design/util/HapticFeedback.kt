package com.skeler.pulse.design.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.getSystemService

/**
 * Haptic feedback utility for send-button interactions.
 *
 * - API 29+: Uses [VibrationEffect.createPredefined] with [VibrationEffect.EFFECT_CLICK]
 * - API < 29: Falls back to `vibrate(10L)` for a brief tactile pulse
 *
 * Call [performSendClick] on pointer-up of the send SplitButton.
 */
object HapticFeedback {

    /**
     * Triggers a brief click haptic appropriate for a send action.
     */
    @Suppress("DEPRECATION")
    fun performSendClick(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService<VibratorManager>()?.defaultVibrator
        } else {
            context.getSystemService<Vibrator>()
        } ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            )
        } else {
            vibrator.vibrate(10L)
        }
    }
}
