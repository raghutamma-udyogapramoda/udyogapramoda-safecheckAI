package com.safecheck.android.accessibility

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Small Text-to-Speech controller for accessible risk results (R-6.5.1). Reads the band,
 * score, and explanation aloud. Lifecycle-aware so the engine is released with the composable.
 */
class TtsController(context: Context) {
    private var ready = false
    private lateinit var tts: TextToSpeech

    init {
        // Assign to the property first, then configure inside the init callback. This avoids a
        // self-referential initializer (val tts = TextToSpeech { ... tts ... }), which Kotlin
        // reports as "type checking has run into a recursive problem".
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) tts.language = Locale.getDefault()
        }
    }

    fun speak(text: String) {
        if (ready) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "safecheck-tts")
    }

    fun stop() {
        if (ready) tts.stop()
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}

/** Remembers a [TtsController] and shuts it down when it leaves the composition. */
@Composable
fun rememberTtsController(): TtsController {
    val context = LocalContext.current
    val controller = remember { TtsController(context) }
    DisposableEffect(Unit) {
        onDispose { controller.shutdown() }
    }
    return controller
}
