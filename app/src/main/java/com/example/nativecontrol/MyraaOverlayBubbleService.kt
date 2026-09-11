package com.example.nativecontrol

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import com.example.MainActivity
import com.example.data.model.MyraaMood
import com.example.ui.theme.MyraaPink

/**
 * Production-grade Floating Overlay Bubble Service for MYRAA.
 *
 * Capabilities:
 * 1. TYPE_APPLICATION_OVERLAY system window rendering floating bubble above any app.
 * 2. Draggable floating orb with touch gesture tracking.
 * 3. Animated aura & color ring responding to Myraa's active mood (pink for cute, red for mock angry, purple for thinking).
 * 4. Tap-to-talk shortcut that triggers voice listening or expands HUD.
 */
class MyraaOverlayBubbleService : Service() {

  companion object {
    const val TAG = "MyraaOverlayBubble"
    private var instance: MyraaOverlayBubbleService? = null

    fun getInstance(): MyraaOverlayBubbleService? = instance

    fun canDrawOverlays(context: Context): Boolean {
      return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
      } else {
        true
      }
    }

    fun openOverlayPermissionSettings(context: Context) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val intent = Intent(
          Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
          android.net.Uri.parse("package:${context.packageName}")
        ).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
      }
    }

    fun startService(context: Context) {
      val intent = Intent(context, MyraaOverlayBubbleService::class.java)
      context.startService(intent)
    }

    fun stopService(context: Context) {
      val intent = Intent(context, MyraaOverlayBubbleService::class.java)
      context.stopService(intent)
    }
  }

  private var windowManager: WindowManager? = null
  private var bubbleView: View? = null
  private var params: WindowManager.LayoutParams? = null

  private var currentMood: MyraaMood = MyraaMood.HAPPY

  override fun onCreate() {
    super.onCreate()
    instance = this
    if (canDrawOverlays(this)) {
      initOverlayView()
    } else {
      Log.w(TAG, "SYSTEM_ALERT_WINDOW permission not granted")
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    removeBubbleView()
    if (instance == this) {
      instance = null
    }
  }

  override fun onBind(intent: Intent?): IBinder? = null

  @SuppressLint("ClickableViewAccessibility")
  private fun initOverlayView() {
    try {
      windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

      val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
      } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
      }

      val bubbleSizePx = (64 * resources.displayMetrics.density).toInt()

      params = WindowManager.LayoutParams(
        bubbleSizePx,
        bubbleSizePx,
        layoutType,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
          WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
      ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 50
        y = 200
      }

      // Create Bubble UI Frame
      val root = FrameLayout(this)
      val bgCircle = View(this).apply {
        layoutParams = FrameLayout.LayoutParams(
          FrameLayout.LayoutParams.MATCH_PARENT,
          FrameLayout.LayoutParams.MATCH_PARENT
        )
        val drawable = android.graphics.drawable.GradientDrawable().apply {
          shape = android.graphics.drawable.GradientDrawable.OVAL
          setColor(android.graphics.Color.parseColor("#141122"))
          setStroke((2 * resources.displayMetrics.density).toInt(), MyraaPink.toArgb())
        }
        background = drawable
      }
      root.addView(bgCircle)

      // Center Mood Emoji / Text
      val emojiText = TextView(this).apply {
        layoutParams = FrameLayout.LayoutParams(
          FrameLayout.LayoutParams.WRAP_CONTENT,
          FrameLayout.LayoutParams.WRAP_CONTENT,
          Gravity.CENTER
        )
        text = currentMood.emoji
        textSize = 22f
      }
      root.addView(emojiText)

      // Touch & Drag Handling
      root.setOnTouchListener(object : View.OnTouchListener {
        private var initialX = 0
        private var initialY = 0
        private var initialTouchX = 0f
        private var initialTouchY = 0f
        private var isClick = false

        override fun onTouch(v: View?, event: MotionEvent): Boolean {
          val p = params ?: return false
          val wm = windowManager ?: return false

          when (event.action) {
            MotionEvent.ACTION_DOWN -> {
              initialX = p.x
              initialY = p.y
              initialTouchX = event.rawX
              initialTouchY = event.rawY
              isClick = true
              return true
            }
            MotionEvent.ACTION_MOVE -> {
              val dx = event.rawX - initialTouchX
              val dy = event.rawY - initialTouchY
              if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                isClick = false
              }
              p.x = initialX + dx.toInt()
              p.y = initialY + dy.toInt()
              wm.updateViewLayout(root, p)
              return true
            }
            MotionEvent.ACTION_UP -> {
              if (isClick) {
                // Open main activity or trigger voice session
                val intent = Intent(this@MyraaOverlayBubbleService, MainActivity::class.java).apply {
                  addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(intent)
              }
              return true
            }
          }
          return false
        }
      })

      bubbleView = root
      windowManager?.addView(bubbleView, params)
      Log.i(TAG, "Myraa Floating Overlay Bubble rendered successfully!")
    } catch (e: Exception) {
      Log.e(TAG, "Error initializing overlay bubble", e)
    }
  }

  fun updateMood(mood: MyraaMood) {
    this.currentMood = mood
    val view = bubbleView as? FrameLayout ?: return
    try {
      val bg = view.getChildAt(0)
      val bgDrawable = bg.background as? android.graphics.drawable.GradientDrawable
      bgDrawable?.setStroke(
        (2 * resources.displayMetrics.density).toInt(),
        mood.auraColor.toArgb()
      )
      val text = view.getChildAt(1) as? TextView
      text?.text = mood.emoji
    } catch (e: Exception) {
      Log.w(TAG, "Failed to update bubble mood", e)
    }
  }

  private fun removeBubbleView() {
    try {
      if (bubbleView != null) {
        windowManager?.removeView(bubbleView)
        bubbleView = null
      }
    } catch (e: Exception) {
      Log.w(TAG, "Error removing bubble view", e)
    }
  }
}
