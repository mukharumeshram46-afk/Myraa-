package com.example.nativecontrol

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * Production-grade Persistent Foreground Audio Service.
 * Acquires PARTIAL_WAKE_LOCK to prevent CPU sleep, keeping microphone streaming
 * and voice interaction active even when the screen is locked or another app is open.
 */
class MyraaForegroundAudioService : Service() {

  companion object {
    const val TAG = "MyraaAudioService"
    const val NOTIFICATION_ID = 1001
    const val CHANNEL_ID = "myraa_audio_channel"

    const val ACTION_START = "com.example.myraa.action.START_AUDIO_SERVICE"
    const val ACTION_STOP = "com.example.myraa.action.STOP_AUDIO_SERVICE"

    private var isRunning = false
    fun isServiceRunning(): Boolean = isRunning

    fun startService(context: Context) {
      val intent = Intent(context, MyraaForegroundAudioService::class.java).apply {
        action = ACTION_START
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }

    fun stopService(context: Context) {
      val intent = Intent(context, MyraaForegroundAudioService::class.java).apply {
        action = ACTION_STOP
      }
      context.stopService(intent)
    }
  }

  private var wakeLock: PowerManager.WakeLock? = null

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
    acquireWakeLock()
  }

  private var isRecording = false
  private var audioRecordThread: Thread? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP) {
      stopSelf()
      return START_NOT_STICKY
    }

    val notification = buildNotification("♡ Myraa is active")

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(
        NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
      )
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }

    isRunning = true
    Log.i(TAG, "MYRAA Foreground Audio Service Started with WakeLock!")
    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()
    releaseWakeLock()
    isRunning = false
    Log.i(TAG, "MYRAA Foreground Audio Service Stopped")
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun acquireWakeLock() {
    try {
      val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
      wakeLock = powerManager.newWakeLock(
        PowerManager.PARTIAL_WAKE_LOCK,
        "Myraa:ForegroundAudioWakeLock"
      ).apply {
        setReferenceCounted(false)
        acquire(4 * 60 * 60 * 1000L) // 4 hours safety timeout
      }
      Log.d(TAG, "Acquired PARTIAL_WAKE_LOCK for audio persistence")
    } catch (e: Exception) {
      Log.e(TAG, "Error acquiring wake lock", e)
    }
  }

  private fun releaseWakeLock() {
    try {
      wakeLock?.let {
        if (it.isHeld) {
          it.release()
          Log.d(TAG, "Released PARTIAL_WAKE_LOCK")
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error releasing wake lock", e)
    }
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        "MYRAA Live Audio & Assistant",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Keeps voice interaction active in background for Piyush"
        setShowBadge(false)
      }
      val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      manager.createNotificationChannel(channel)
    }
  }

  private fun buildNotification(contentText: String): Notification {
    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("MYRAA AI Assistant")
      .setContentText(contentText)
      .setSmallIcon(android.R.drawable.ic_btn_speak_now)
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .build()
  }
}
