package com.example.nativecontrol

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Production-grade Native Android Accessibility Service for MYRAA.
 * Enables:
 * - Real-time screen UI tree hierarchy inspection (readScreenNodes)
 * - Click elements by text, ID, or coordinates (clickElement)
 * - Automated typing and text pasting into focused fields (typeText)
 * - Programmatic smooth scrolling (UP, DOWN, LEFT, RIGHT)
 * - Global system actions (RECENTS, HOME, BACK, NOTIFICATIONS, QUICK_SETTINGS, LOCK_SCREEN)
 */
class MyraaAccessibilityService : AccessibilityService() {

  companion object {
    const val TAG = "MyraaAccessibility"
    private var instance: MyraaAccessibilityService? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    fun getInstance(): MyraaAccessibilityService? = instance

    fun isServiceEnabled(context: Context): Boolean {
      val expectedServiceName = "${context.packageName}/${MyraaAccessibilityService::class.java.canonicalName}"
      val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
      ) ?: return false
      return enabledServices.contains(expectedServiceName)
    }

    fun openAccessibilitySettings(context: Context) {
      val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
    }
  }

  override fun onServiceConnected() {
    super.onServiceConnected()
    instance = this
    _isConnected.value = true
    Log.i(TAG, "MYRAA Accessibility Service Connected and Active!")
  }

  override fun onDestroy() {
    super.onDestroy()
    if (instance == this) {
      instance = null
      _isConnected.value = false
    }
    Log.i(TAG, "MYRAA Accessibility Service Destroyed")
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    // Passive monitoring of screen transitions
  }

  override fun onInterrupt() {
    Log.w(TAG, "MYRAA Accessibility Service Interrupted")
  }

  /**
   * Reads all visible screen nodes as structured JSON.
   */
  fun readScreenNodes(maxDepth: Int = 6): JSONObject {
    val result = JSONObject()
    val root = rootInActiveWindow ?: return result.apply {
      put("error", "No active window accessible")
      put("nodes", JSONArray())
    }

    val nodesArray = JSONArray()
    fun traverse(node: AccessibilityNodeInfo?, depth: Int) {
      if (node == null || depth > maxDepth) return
      if (node.isVisibleToUser) {
        val nodeObj = JSONObject()
        val text = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        val viewId = node.viewIdResourceName ?: ""
        val className = node.className?.toString() ?: ""

        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        nodeObj.put("className", className)
        if (text.isNotBlank()) nodeObj.put("text", text)
        if (contentDesc.isNotBlank()) nodeObj.put("contentDescription", contentDesc)
        if (viewId.isNotBlank()) nodeObj.put("id", viewId)
        nodeObj.put("clickable", node.isClickable)
        nodeObj.put("editable", node.isEditable)
        nodeObj.put("scrollable", node.isScrollable)
        nodeObj.put("bounds", JSONObject().apply {
          put("left", bounds.left)
          put("top", bounds.top)
          put("right", bounds.right)
          put("bottom", bounds.bottom)
          put("centerX", bounds.centerX())
          put("centerY", bounds.centerY())
        })

        if (text.isNotBlank() || contentDesc.isNotBlank() || node.isClickable || node.isEditable) {
          nodesArray.put(nodeObj)
        }
      }

      for (i in 0 until node.childCount) {
        traverse(node.getChild(i), depth + 1)
      }
    }

    try {
      traverse(root, 0)
      result.put("success", true)
      result.put("nodeCount", nodesArray.length())
      result.put("nodes", nodesArray)
    } catch (e: Exception) {
      result.put("error", e.localizedMessage ?: "Failed reading nodes")
    }

    return result
  }

  /**
   * Click an element matching text, resource ID, or tap at specific x,y coordinates.
   */
  fun clickElement(target: String?, x: Float? = null, y: Float? = null, onResult: (Boolean, String) -> Unit) {
    if (x != null && y != null && x > 0 && y > 0) {
      performTapGesture(x, y) { success ->
        onResult(success, if (success) "Tapped coordinates ($x, $y)" else "Failed to tap coordinates")
      }
      return
    }

    if (target.isNullOrBlank()) {
      onResult(false, "No target text, ID, or coordinates provided")
      return
    }

    val root = rootInActiveWindow
    if (root == null) {
      onResult(false, "Active screen window not found")
      return
    }

    // Search by text
    var foundNode: AccessibilityNodeInfo? = null
    val nodesByText = root.findAccessibilityNodeInfosByText(target)
    if (!nodesByText.isNullOrEmpty()) {
      foundNode = nodesByText.firstOrNull { it.isVisibleToUser }
    }

    // Search by view ID if not found
    if (foundNode == null && target.contains(":id/")) {
      val nodesById = root.findAccessibilityNodeInfosByViewId(target)
      if (!nodesById.isNullOrEmpty()) {
        foundNode = nodesById.firstOrNull { it.isVisibleToUser }
      }
    }

    // If still null, search recursively matching text or content description
    if (foundNode == null) {
      foundNode = searchNodeRecursively(root) { node ->
        val nodeText = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        nodeText.contains(target, ignoreCase = true) || contentDesc.contains(target, ignoreCase = true)
      }
    }

    if (foundNode != null) {
      var clickableNode: AccessibilityNodeInfo? = foundNode
      while (clickableNode != null && !clickableNode.isClickable) {
        clickableNode = clickableNode.parent
      }

      val targetClick = clickableNode ?: foundNode
      val clicked = targetClick.performAction(AccessibilityNodeInfo.ACTION_CLICK)
      if (clicked) {
        onResult(true, "Successfully clicked element matching '$target'")
      } else {
        // Fallback to coordinates tap
        val bounds = Rect()
        targetClick.getBoundsInScreen(bounds)
        performTapGesture(bounds.centerX().toFloat(), bounds.centerY().toFloat()) { tapSuccess ->
          onResult(tapSuccess, if (tapSuccess) "Tapped center of '$target'" else "Click action failed for '$target'")
        }
      }
    } else {
      onResult(false, "Could not locate visible element '$target' on screen")
    }
  }

  /**
   * Types text into the currently focused or editable input field.
   */
  fun typeText(text: String, clearFirst: Boolean = false, onResult: (Boolean, String) -> Unit) {
    val root = rootInActiveWindow
    if (root == null) {
      onResult(false, "Cannot access active window")
      return
    }

    val focusedNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
      ?: searchNodeRecursively(root) { it.isEditable && it.isVisibleToUser }

    if (focusedNode == null) {
      onResult(false, "No active input or editable field focused on screen")
      return
    }

    val args = Bundle()
    if (clearFirst) {
      args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
    } else {
      val existing = focusedNode.text?.toString() ?: ""
      args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, existing + text)
    }

    val success = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    onResult(success, if (success) "Entered text successfully" else "Failed to set text in field")
  }

  /**
   * Smoothly scrolls the screen in specified direction.
   */
  fun scrollScreen(direction: String, onResult: (Boolean, String) -> Unit) {
    val root = rootInActiveWindow
    if (root == null) {
      onResult(false, "No active screen window")
      return
    }

    val scrollable = searchNodeRecursively(root) { it.isScrollable && it.isVisibleToUser }
    val isForward = direction.equals("DOWN", ignoreCase = true) || direction.equals("RIGHT", ignoreCase = true)

    if (scrollable != null) {
      val action = if (isForward) {
        AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
      } else {
        AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
      }
      val ok = scrollable.performAction(action)
      onResult(ok, if (ok) "Scrolled $direction" else "Scroll action returned false")
    } else {
      // Gesture scroll fallback
      val displayMetrics = resources.displayMetrics
      val width = displayMetrics.widthPixels.toFloat()
      val height = displayMetrics.heightPixels.toFloat()

      val path = Path()
      if (isForward) {
        path.moveTo(width / 2f, height * 0.75f)
        path.lineTo(width / 2f, height * 0.25f)
      } else {
        path.moveTo(width / 2f, height * 0.25f)
        path.lineTo(width / 2f, height * 0.75f)
      }

      val gesture = GestureDescription.Builder()
        .addStroke(GestureDescription.StrokeDescription(path, 0, 350))
        .build()

      dispatchGesture(gesture, object : GestureResultCallback() {
        override fun onCompleted(gestureDescription: GestureDescription?) {
          onResult(true, "Scrolled $direction via gesture")
        }

        override fun onCancelled(gestureDescription: GestureDescription?) {
          onResult(false, "Scroll gesture cancelled")
        }
      }, null)
    }
  }

  /**
   * Execute global system actions.
   */
  fun performSystemAction(actionName: String): Pair<Boolean, String> {
    val actionId = when (actionName.uppercase()) {
      "BACK" -> GLOBAL_ACTION_BACK
      "HOME" -> GLOBAL_ACTION_HOME
      "RECENTS", "APP_SWITCH" -> GLOBAL_ACTION_RECENTS
      "NOTIFICATIONS" -> GLOBAL_ACTION_NOTIFICATIONS
      "QUICK_SETTINGS" -> GLOBAL_ACTION_QUICK_SETTINGS
      "LOCK_SCREEN" -> GLOBAL_ACTION_LOCK_SCREEN
      "POWER_DIALOG" -> GLOBAL_ACTION_POWER_DIALOG
      else -> null
    }

    if (actionId == null) {
      return Pair(false, "Unsupported system action: $actionName")
    }

    val success = performGlobalAction(actionId)
    return Pair(success, if (success) "Executed $actionName" else "Failed to execute $actionName")
  }

  private fun performTapGesture(x: Float, y: Float, onComplete: (Boolean) -> Unit) {
    val path = Path().apply {
      moveTo(x, y)
    }
    val gesture = GestureDescription.Builder()
      .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
      .build()

    dispatchGesture(gesture, object : GestureResultCallback() {
      override fun onCompleted(gestureDescription: GestureDescription?) {
        onComplete(true)
      }

      override fun onCancelled(gestureDescription: GestureDescription?) {
        onComplete(false)
      }
    }, null)
  }

  data class ChainStep(
    val action: String,
    val target: String? = null,
    val input: String? = null,
    val delayMs: Long = 800L
  )

  /**
   * Cross-App Task Chaining Engine:
   * Sequentially executes multi-step chains across applications (e.g. open app -> search -> click -> type -> send).
   */
  fun executeAppChain(
    steps: List<ChainStep>,
    onStepResult: (stepIndex: Int, message: String) -> Unit,
    onFinished: (success: Boolean, summary: String) -> Unit
  ) {
    if (steps.isEmpty()) {
      onFinished(false, "No chain steps provided")
      return
    }

    val serviceScope = CoroutineScope(Dispatchers.Main)
    serviceScope.launch {
      val logList = mutableListOf<String>()
      var overallSuccess = true

      for ((index, step) in steps.withIndex()) {
        val action = step.action.uppercase().trim()
        val target = step.target ?: ""
        val input = step.input ?: ""
        var stepLog = "Step ${index + 1} ($action)"

        when (action) {
          "OPEN_APP" -> {
            val intent = packageManager.getLaunchIntentForPackage(target)
            if (intent != null) {
              intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
              startActivity(intent)
              stepLog += ": Launched $target"
            } else {
              stepLog += ": App not found for package $target"
              overallSuccess = false
            }
          }

          "CLICK" -> {
            var clicked = false
            clickElement(target) { success, msg ->
              clicked = success
              stepLog += ": $msg"
            }
            if (!clicked) overallSuccess = false
          }

          "TYPE" -> {
            var typed = false
            typeText(input, false) { success, msg ->
              typed = success
              stepLog += ": $msg"
            }
            if (!typed) overallSuccess = false
          }

          "SCROLL" -> {
            scrollScreen(if (target.isNotBlank()) target else "DOWN") { success, msg ->
              stepLog += ": $msg"
            }
          }

          "SYSTEM_ACTION" -> {
            val (_, msg) = performSystemAction(target)
            stepLog += ": $msg"
          }

          "WAIT" -> {
            stepLog += ": Waited ${step.delayMs}ms"
          }

          else -> {
            stepLog += ": Unknown action"
          }
        }

        logList.add(stepLog)
        onStepResult(index, stepLog)
        delay(step.delayMs)
      }

      onFinished(overallSuccess, logList.joinToString(" | "))
    }
  }

  private fun searchNodeRecursively(
    node: AccessibilityNodeInfo?,
    predicate: (AccessibilityNodeInfo) -> Boolean
  ): AccessibilityNodeInfo? {
    if (node == null) return null
    if (predicate(node)) return node
    for (i in 0 until node.childCount) {
      val child = node.getChild(i)
      val found = searchNodeRecursively(child, predicate)
      if (found != null) return found
    }
    return null
  }
}
