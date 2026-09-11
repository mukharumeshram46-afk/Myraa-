package com.example.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

/**
 * Manages smooth, gapless PCM audio playback for Gemini Live:
 * 24 kHz output (or 16 kHz fallback), 16-bit PCM, Mono.
 * Uses a single managed AudioTrack instance and a thread-safe chunk queue.
 */
class AudioOutputManager(
  private val scope: CoroutineScope,
  private val onAmplitudeChanged: (amplitude: Float) -> Unit,
  private val onPlaybackStateChanged: (isPlaying: Boolean) -> Unit
) {

  companion object {
    const val SAMPLE_RATE = 24000
    const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
    const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
  }

  private var audioTrack: AudioTrack? = null
  private val audioQueue = LinkedBlockingQueue<ByteArray>()
  private var playbackJob: Job? = null
  @Volatile private var isRunning = false

  init {
    initAudioTrack()
    startPlaybackLoop()
  }

  @Synchronized
  private fun initAudioTrack() {
    val minBufferSize = AudioTrack.getMinBufferSize(
      SAMPLE_RATE,
      CHANNEL_CONFIG,
      AUDIO_FORMAT
    )
    val bufferSize = maxOf(minBufferSize, 24000 * 2) // ~1 second buffer capacity

    try {
      audioTrack = AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AUDIO_FORMAT)
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(CHANNEL_CONFIG)
            .build()
        )
        .setBufferSizeInBytes(bufferSize)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()

      audioTrack?.play()
    } catch (_: Exception) {
      // AudioTrack init fallback
    }
  }

  private fun startPlaybackLoop() {
    isRunning = true
    playbackJob = scope.launch(Dispatchers.IO) {
      while (isActive && isRunning) {
        val chunk = audioQueue.poll(100, TimeUnit.MILLISECONDS)
        if (chunk != null && chunk.isNotEmpty()) {
          onPlaybackStateChanged(true)
          computeAndEmitAmplitude(chunk)

          if (audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
            try {
              audioTrack?.play()
            } catch (_: Exception) {}
          }

          var bytesWritten = 0
          while (bytesWritten < chunk.size && isActive && isRunning) {
            val written = audioTrack?.write(chunk, bytesWritten, chunk.size - bytesWritten) ?: -1
            if (written > 0) {
              bytesWritten += written
            } else {
              break
            }
          }
        } else {
          if (audioQueue.isEmpty()) {
            onPlaybackStateChanged(false)
            onAmplitudeChanged(0.0f)
          }
        }
      }
    }
  }

  private fun computeAndEmitAmplitude(bytes: ByteArray) {
    val sampleCount = bytes.size / 2
    if (sampleCount <= 0) return

    var sum = 0.0
    for (i in 0 until sampleCount) {
      val sample = ((bytes[i * 2 + 1].toInt() shl 8) or (bytes[i * 2].toInt() and 0xFF)).toShort()
      sum += sample * sample
    }
    val rms = sqrt(sum / sampleCount)
    val amp = (rms / 32768.0).toFloat().coerceIn(0.0f, 1.0f)
    onAmplitudeChanged(amp)
  }

  /**
   * Enqueues incoming PCM bytes from Gemini Live response.
   */
  fun enqueueAudio(bytes: ByteArray) {
    if (isRunning) {
      audioQueue.offer(bytes)
    }
  }

  /**
   * Immediately clears audio buffer and stops current playback (e.g. user interrupted).
   */
  @Synchronized
  fun interrupt() {
    audioQueue.clear()
    try {
      audioTrack?.pause()
      audioTrack?.flush()
    } catch (_: Exception) {}
    onPlaybackStateChanged(false)
    onAmplitudeChanged(0.0f)
  }

  @Synchronized
  fun release() {
    isRunning = false
    playbackJob?.cancel()
    audioQueue.clear()
    try {
      audioTrack?.stop()
      audioTrack?.release()
    } catch (_: Exception) {}
    audioTrack = null
    onPlaybackStateChanged(false)
    onAmplitudeChanged(0.0f)
  }
}
