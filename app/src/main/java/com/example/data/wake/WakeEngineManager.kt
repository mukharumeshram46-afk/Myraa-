package com.example.data.wake

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * Wake Engine that operates locally in standby mode for Piyush.
 * Detects wake phrases: "Myraa", "Myra", "Hey Myraa", "Hello Myraa", "Hlo Myraa", "Okay Myraa", "Suno Myraa", "Nami".
 * Keeps standby audio strictly local; does NOT stream audio to Gemini until wake word triggers.
 */
class WakeEngineManager(
  private val context: Context,
  private val onWakeWordDetected: (phrase: String) -> Unit,
  private val onPermissionDenied: () -> Unit,
  private val onStatusMessage: (String) -> Unit
) {

  private val wakeWords = listOf(
    "myraa",
    "myra",
    "mira",
    "hey myraa",
    "hey myra",
    "hello myraa",
    "hlo myraa",
    "okay myraa",
    "suno myraa",
    "sun myraa",
    "nami",
    "hey nami"
  )

  private var speechRecognizer: SpeechRecognizer? = null
  private var isListening = false
  private var isEngineEnabled = true
  private val mainHandler = Handler(Looper.getMainLooper())

  fun setEnabled(enabled: Boolean) {
    isEngineEnabled = enabled
    if (!enabled) {
      stopListening()
      onStatusMessage("Wake Engine standby paused")
    } else {
      startListening()
      onStatusMessage("Wake Engine active (listening for 'Hey Myraa'...)")
    }
  }

  fun isEnabled(): Boolean = isEngineEnabled

  fun startListening() {
    if (!isEngineEnabled) return
    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
      onPermissionDenied()
      return
    }

    mainHandler.post {
      try {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
          onStatusMessage("Speech recognizer unavailable on device; using manual wake.")
          return@post
        }

        if (speechRecognizer == null) {
          speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createListener())
          }
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
          putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
          putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
          putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
          putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        speechRecognizer?.startListening(intent)
        isListening = true
        onStatusMessage("Standby: Listening for \"Hey Myraa\"...")
      } catch (e: Exception) {
        onStatusMessage("Wake Engine initialization error: ${e.message}")
        isListening = false
      }
    }
  }

  fun stopListening() {
    mainHandler.post {
      try {
        speechRecognizer?.stopListening()
        speechRecognizer?.cancel()
      } catch (_: Exception) {}
      isListening = false
    }
  }

  fun triggerManualWake() {
    onWakeWordDetected("Manual Wake")
  }

  fun destroy() {
    mainHandler.post {
      try {
        speechRecognizer?.destroy()
      } catch (_: Exception) {}
      speechRecognizer = null
      isListening = false
    }
  }

  private fun createListener(): RecognitionListener = object : RecognitionListener {
    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}

    override fun onError(error: Int) {
      isListening = false
      if (isEngineEnabled) {
        // Automatically cycle back into listening standby after error timeout
        mainHandler.postDelayed({
          if (isEngineEnabled && !isListening) {
            startListening()
          }
        }, 1200)
      }
    }

    override fun onResults(results: Bundle?) {
      isListening = false
      val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
      checkMatches(matches)

      if (isEngineEnabled) {
        mainHandler.postDelayed({
          if (isEngineEnabled && !isListening) {
            startListening()
          }
        }, 400)
      }
    }

    override fun onPartialResults(partialResults: Bundle?) {
      val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
      checkMatches(matches)
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
  }

  private fun checkMatches(matches: List<String>?) {
    if (matches == null) return
    for (text in matches) {
      val lower = text.lowercase(Locale.getDefault()).trim()
      for (target in wakeWords) {
        if (lower.contains(target)) {
          stopListening()
          onWakeWordDetected(target)
          return
        }
      }
    }
  }
}
