package com.example.data.audio

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Manages real-time microphone capture for Gemini Live API:
 * 16 kHz, 16-bit PCM, Mono.
 */
class AudioInputManager(
  private val context: Context,
  private val scope: CoroutineScope,
  private val onAudioChunk: (base64Pcm: String) -> Unit,
  private val onAmplitudeChanged: (amplitude: Float) -> Unit,
  private val onError: (String) -> Unit
) {

  companion object {
    const val SAMPLE_RATE = 16000
    const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    // Chunks of ~100ms (1600 samples = 3200 bytes)
    const val CHUNK_SAMPLES = 1600
  }

  private var audioRecord: AudioRecord? = null
  private var recordingJob: Job? = null
  private var isRecording = false

  fun hasPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
      context,
      android.Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
  }

  @SuppressLint("MissingPermission")
  @Synchronized
  fun startRecording(): Boolean {
    if (isRecording) return true

    if (!hasPermission()) {
      onError("Microphone permission required for Audio Input.")
      return false
    }

    val minBufferSize = AudioRecord.getMinBufferSize(
      SAMPLE_RATE,
      CHANNEL_CONFIG,
      AUDIO_FORMAT
    )

    if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
      onError("AudioRecord configuration not supported on this device.")
      return false
    }

    val bufferSize = maxOf(minBufferSize, CHUNK_SAMPLES * 2 * 2)

    try {
      audioRecord = AudioRecord(
        MediaRecorder.AudioSource.VOICE_RECOGNITION,
        SAMPLE_RATE,
        CHANNEL_CONFIG,
        AUDIO_FORMAT,
        bufferSize
      )

      if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
        // Fallback to MIC
        audioRecord?.release()
        audioRecord = AudioRecord(
          MediaRecorder.AudioSource.MIC,
          SAMPLE_RATE,
          CHANNEL_CONFIG,
          AUDIO_FORMAT,
          bufferSize
        )
      }

      if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
        onError("Failed to initialize AudioRecord hardware.")
        return false
      }

      audioRecord?.startRecording()
      isRecording = true

      recordingJob = scope.launch(Dispatchers.IO) {
        val audioBuffer = ShortArray(CHUNK_SAMPLES)
        val byteBuffer = ByteArray(CHUNK_SAMPLES * 2)

        while (isActive && isRecording) {
          val readResult = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: -1
          if (readResult > 0) {
            // Calculate RMS amplitude for visualizer
            var sum = 0.0
            for (i in 0 until readResult) {
              val sample = audioBuffer[i]
              sum += sample * sample
              // Convert little-endian short to byte array
              byteBuffer[i * 2] = (sample.toInt() and 0xFF).toByte()
              byteBuffer[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
            }
            val rms = sqrt(sum / readResult)
            val normalizedAmp = (rms / 32768.0).toFloat().coerceIn(0.0f, 1.0f)
            onAmplitudeChanged(normalizedAmp)

            // Base64 encode PCM 16-bit
            val base64 = Base64.encodeToString(byteBuffer, 0, readResult * 2, Base64.NO_WRAP)
            onAudioChunk(base64)
          }
        }
      }

      return true
    } catch (e: Exception) {
      onError("Microphone error: ${e.localizedMessage ?: "Unknown error"}")
      stopRecording()
      return false
    }
  }

  @Synchronized
  fun stopRecording() {
    isRecording = false
    recordingJob?.cancel()
    recordingJob = null
    try {
      audioRecord?.stop()
    } catch (_: Exception) {}
    try {
      audioRecord?.release()
    } catch (_: Exception) {}
    audioRecord = null
    onAmplitudeChanged(0.0f)
  }
}
