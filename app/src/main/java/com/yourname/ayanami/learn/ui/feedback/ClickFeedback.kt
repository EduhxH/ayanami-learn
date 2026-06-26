package com.yourname.ayanami.learn.ui.feedback

import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView

/**
 * Whether tactile/audible click feedback is enabled. Provided at the app root from the
 * user's `soundEffects` preference so every screen can honor it without extra wiring.
 */
val LocalSoundEffectsEnabled = staticCompositionLocalOf { true }

/**
 * Returns a callback that plays the system click sound plus a light haptic tick, gated by
 * [LocalSoundEffectsEnabled]. Wrap an `onClick` like: `onClick = { click(); doThing() }`.
 */
@Composable
fun rememberClickFeedback(): () -> Unit {
    val view = LocalView.current
    val enabled = LocalSoundEffectsEnabled.current
    return {
        if (enabled) {
            view.playSoundEffect(SoundEffectConstants.CLICK)
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
}
