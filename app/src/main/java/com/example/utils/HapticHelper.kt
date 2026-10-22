package com.example.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

@Immutable
class HapticController(
    private val context: Context?,
    private val view: View?
) {
    private val vibrator: Vibrator? by lazy {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context?.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context?.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }.getOrNull()
    }

    /**
     * Crisp, lightweight iOS-style selection tick (used for swipe tab transitions, sliders, filter chips).
     */
    fun tick() {
        runCatching {
            view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            val vib = vibrator
            if (vib != null && vib.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib.vibrate(VibrationEffect.createOneShot(10L, 65))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(10L)
                }
            }
        }
    }

    /**
     * Tactile glass button / navigation bar click haptic.
     */
    fun click() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                view?.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            } else {
                view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
            val vib = vibrator
            if (vib != null && vib.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib.vibrate(VibrationEffect.createOneShot(16L, 110))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(15L)
                }
            }
        }
    }

    /**
     * Distinct confirmation / shutter / long-press haptic thud.
     */
    fun heavy() {
        runCatching {
            view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            val vib = vibrator
            if (vib != null && vib.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib.vibrate(VibrationEffect.createOneShot(28L, 190))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(25L)
                }
            }
        }
    }
}

val LocalHaptics = staticCompositionLocalOf {
    HapticController(context = null, view = null)
}

@Composable
fun rememberHapticController(): HapticController {
    val context = LocalContext.current
    val view = LocalView.current
    return remember(context, view) {
        HapticController(context.applicationContext, view)
    }
}
