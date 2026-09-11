package com.example.nativecontrol

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/**
 * Screen capture service that grabs frames at 2-5 FPS for Gemini Live multimodal vision analysis.
 */
class MyraaMediaProjectionService : Service() {

  companion object {
    const val TAG = "MyraaProjection"
    const val NOTIFICATION_ID = 1002
    const val CHANNEL_ID = "myraa_projection_channel"

    private var instance: MyraaMediaProjectionService? = null
    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private var onFrameCallback: ((base64Jpeg: String) -> Unit)? = null

    fun registerFrameCallback(callback: (base64Jpeg: String) -> Unit) {
      onFrameCallback = callback
    }

    fun unregisterFrameCallback() {
      onFrameCallback = null
    }

    var projectionDataIntent: Intent? = null
    var projectionResultCode: Int = 0
  }

  private var mediaProjection: MediaProjection? = null
  private var virtualDisplay: VirtualDisplay? = null
  private var imageReader: ImageReader? = null
  private var captureJob: Job? = null
  private val serviceScope = CoroutineScope(Dispatchers.Default)

  override fun onCreate() {
    super.onCreate()
    instance = this
    createNotificationChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val notification = buildNotification("MYRAA Screen Vision is active")

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(
        NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
      )
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }

    startScreenCapture()
    return START_NOT_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()
    stopScreenCapture()
    instance = null
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun startScreenCapture() {
    val data = projectionDataIntent
    if (data == null || projectionResultCode == 0) {
      Log.e(TAG, "Missing MediaProjection data or permission result")
      return
    }

    val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    mediaProjection = mpManager.getMediaProjection(projectionResultCode, data)

    val metrics = resources.displayMetrics
    val width = 540 // Scaled down for high performance & low network latency
    val height = (540f * metrics.heightPixels / metrics.widthPixels).toInt()
    val density = metrics.densityDpi

    imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
    virtualDisplay = mediaProjection?.createVirtualDisplay(
      "MyraaScreenVision",
      width,
      height,
      density,
      DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
      imageReader?.surface,
      null,
      null
    )

    _isCapturing.value = true
    Log.i(TAG, "MediaProjection started successfully at ${width}x${height} (target 2-5 FPS)")

    // Capture loop at ~2 FPS (every 500ms)
    captureJob = serviceScope.launch {
      while (isActive && _isCapturing.value) {
        delay(500)
        captureLatestFrame(width, height)
      }
    }
  }

  private fun captureLatestFrame(width: Int, height: Int) {
    val reader = imageReader ?: return
    try {
      val image = reader.acquireLatestImage() ?: return
      val planes = image.planes
      val buffer = planes[0].buffer
      val pixelStride = planes[0].pixelStride
      val rowStride = planes[0].rowStride
      val rowPadding = rowStride - pixelStride * width

      val bitmap = Bitmap.createBitmap(
        width + rowPadding / pixelStride,
        height,
        Bitmap.Config.ARGB_8888
      )
      bitmap.copyPixelsFromBuffer(buffer)
      image.close()

      // Crop if padded
      val finalBitmap = if (rowPadding > 0) {
        Bitmap.createBitmap(bitmap, 0, 0, width, height)
      } else {
        bitmap
      }

      val outputStream = ByteArrayOutputStream()
      finalBitmap.compress(Bitmap.CompressFormat.JPEG, 65, outputStream)
      val base64Jpeg = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

      onFrameCallback?.invoke(base64Jpeg)
    } catch (e: Exception) {
      Log.w(TAG, "Frame capture skipped: ${e.message}")
    }
  }

  private fun stopScreenCapture() {
    _isCapturing.value = false
    captureJob?.cancel()
    captureJob = null
    virtualDisplay?.release()
    virtualDisplay = null
    imageReader?.close()
    imageReader = null
    mediaProjection?.stop()
    mediaProjection = null
    Log.i(TAG, "MediaProjection stopped")
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        "MYRAA Screen Vision",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Analyzes screen content to assist Piyush"
        setShowBadge(false)
      }
      val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      manager.createNotificationChannel(channel)
    }
  }

  private fun buildNotification(contentText: String): Notification {
    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("MYRAA Screen Vision")
      .setContentText(contentText)
      .setSmallIcon(android.R.drawable.ic_menu_camera)
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .build()
  }
}
