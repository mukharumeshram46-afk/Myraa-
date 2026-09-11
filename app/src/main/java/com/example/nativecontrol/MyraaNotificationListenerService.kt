package com.example.nativecontrol

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Production-grade Notification Listener Service for MYRAA.
 *
 * Capabilities:
 * 1. Monitors incoming notifications (WhatsApp, Telegram, Messages, SMS, Gmail).
 * 2. Filters promotional / OTP / spam SMS silently.
 * 3. Extracts sender, content, and timestamp for alerting Piyush.
 * 4. Implements android_filterAndReplyNotification by extracting RemoteInput and executing reply PendingIntent.
 */
class MyraaNotificationListenerService : NotificationListenerService() {

  companion object {
    const val TAG = "MyraaNotification"
    private var instance: MyraaNotificationListenerService? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _recentNotifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val recentNotifications: StateFlow<List<NotificationItem>> = _recentNotifications.asStateFlow()

    // Callback when important message arrives for Piyush
    var onNotificationAlert: ((NotificationItem) -> Unit)? = null

    fun getInstance(): MyraaNotificationListenerService? = instance

    fun isNotificationAccessGranted(context: Context): Boolean {
      val enabledListeners = android.provider.Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
      ) ?: return false
      val componentName = "${context.packageName}/${MyraaNotificationListenerService::class.java.canonicalName}"
      return enabledListeners.contains(componentName)
    }

    fun openNotificationListenerSettings(context: Context) {
      val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
    }
  }

  data class NotificationItem(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val sender: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val isPromotional: Boolean
  )

  override fun onListenerConnected() {
    super.onListenerConnected()
    instance = this
    _isConnected.value = true
    Log.i(TAG, "MYRAA Notification Listener Service Connected!")
  }

  override fun onListenerDisconnected() {
    super.onListenerDisconnected()
    if (instance == this) {
      instance = null
      _isConnected.value = false
    }
    Log.w(TAG, "MYRAA Notification Listener Service Disconnected")
  }

  override fun onNotificationPosted(sbn: StatusBarNotification?) {
    if (sbn == null) return
    val notification = sbn.notification ?: return
    val extras = notification.extras ?: return

    val packageName = sbn.packageName ?: ""
    if (packageName == applicationContext.packageName) return // Ignore own notifications

    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
    val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
    val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

    if (title.isBlank() && text.isBlank()) return

    val appLabel = try {
      val pm = packageManager
      val appInfo = pm.getApplicationInfo(packageName, 0)
      pm.getApplicationLabel(appInfo).toString()
    } catch (_: Exception) {
      packageName
    }

    val sender = if (title.isNotBlank()) title else appLabel
    val isPromo = isPromotionalOrSpam(text, title, packageName)

    val item = NotificationItem(
      key = sbn.key,
      packageName = packageName,
      appLabel = appLabel,
      sender = sender,
      title = title,
      text = text,
      timestamp = sbn.postTime,
      isPromotional = isPromo
    )

    // Store in recent notifications list
    val current = _recentNotifications.value.toMutableList()
    current.add(0, item)
    if (current.size > 50) current.removeAt(current.size - 1)
    _recentNotifications.value = current

    // If important and not spam, trigger callback for voice alert
    if (!isPromo && isMessagingOrSocialApp(packageName)) {
      Log.d(TAG, "New message for Piyush from $sender ($appLabel): $text")
      onNotificationAlert?.invoke(item)
    }
  }

  override fun onNotificationRemoved(sbn: StatusBarNotification?) {
    if (sbn == null) return
    val key = sbn.key
    _recentNotifications.value = _recentNotifications.value.filter { it.key != key }
  }

  /**
   * Filter & reply to notification from WhatsApp, Telegram, Messages, etc.
   */
  fun replyToNotification(targetAppOrPackage: String, senderName: String, replyText: String): Pair<Boolean, String> {
    val activeSbn = activeNotifications ?: return Pair(false, "No active notifications found on phone")

    val matchingSbn = activeSbn.firstOrNull { sbn ->
      val pkg = sbn.packageName.lowercase()
      val title = sbn.notification.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.lowercase() ?: ""
      val target = targetAppOrPackage.lowercase()
      val sender = senderName.lowercase()

      (pkg.contains(target) || target.contains(pkg)) && (sender.isBlank() || title.contains(sender))
    } ?: return Pair(false, "Could not find active notification matching '$senderName' in $targetAppOrPackage")

    val notification = matchingSbn.notification
    val actions = notification.actions ?: return Pair(false, "Notification has no quick-reply actions")

    // Find action that contains RemoteInput
    for (action in actions) {
      val remoteInputs = action.remoteInputs
      if (remoteInputs != null && remoteInputs.isNotEmpty()) {
        for (remoteInput in remoteInputs) {
          try {
            val replyIntent = Intent()
            val bundle = Bundle()
            bundle.putCharSequence(remoteInput.resultKey, replyText)
            RemoteInput.addResultsToIntent(arrayOf(remoteInput), replyIntent, bundle)

            action.actionIntent.send(this, 0, replyIntent)
            Log.i(TAG, "Successfully sent auto-reply to $senderName: '$replyText'")
            return Pair(true, "Sent reply to $senderName: \"$replyText\"")
          } catch (e: Exception) {
            Log.e(TAG, "Failed to send notification reply", e)
            return Pair(false, "Failed to send reply: ${e.message}")
          }
        }
      }
    }

    return Pair(false, "Notification found, but no direct reply input was available")
  }

  /**
   * Return recent non-spam notifications as JSON array for Gemini.
   */
  fun getRecentNotificationsJson(): JSONArray {
    val array = JSONArray()
    for (n in _recentNotifications.value.take(15)) {
      val obj = JSONObject().apply {
        put("app", n.appLabel)
        put("package", n.packageName)
        put("sender", n.sender)
        put("title", n.title)
        put("text", n.text)
        put("isPromotional", n.isPromotional)
        put("time", n.timestamp)
      }
      array.put(obj)
    }
    return array
  }

  private fun isPromotionalOrSpam(text: String, title: String, pkg: String): Boolean {
    val combined = "$title $text".lowercase()
    val promoKeywords = listOf(
      "discount", "offer", "sale", "cashback", "coupon", "limited time",
      "flat off", "credit card", "loan", "pre-approved", "exclusive deal",
      "use code", "recharge now", "win cash", "spin & win", "claim your"
    )
    return promoKeywords.any { combined.contains(it) }
  }

  private fun isMessagingOrSocialApp(pkg: String): Boolean {
    val lower = pkg.lowercase()
    return lower.contains("whatsapp") ||
      lower.contains("telegram") ||
      lower.contains("messaging") ||
      lower.contains("mms") ||
      lower.contains("slack") ||
      lower.contains("discord") ||
      lower.contains("instagram") ||
      lower.contains("signal")
  }
}
