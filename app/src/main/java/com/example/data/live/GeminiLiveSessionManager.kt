package com.example.data.live

import android.util.Base64
import android.util.Log
import com.example.data.audio.AudioInputManager
import com.example.data.audio.AudioOutputManager
import com.example.data.model.DiagnosticsInfo
import com.example.data.model.LiveConnectionState
import com.example.data.model.MessageSender
import com.example.data.model.MyraaMood
import com.example.data.model.TranscriptMessage
import com.example.data.tools.MyraaToolsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Production-grade Gemini Live Session Manager for MYRAA AI Assistant.
 *
 * Core Capabilities:
 * 1. Model: gemini-3.1-flash-live-preview
 * 2. System Instructions: Full Myraa persona (affectionate, cute, playful, slightly nakhre-wali,
 *    competent AI copilot + Android controller, addressing Piyush by name).
 * 3. Startup greeting sequence played after Live session is verified connected.
 * 4. Multimodal real-time streaming: 16kHz PCM audio & 2-5 FPS JPEG screen vision frames.
 * 5. Full 10+ tool bidirectional function calling execution.
 * 6. Single active session guarantee & exponential backoff reconnection.
 */
class GeminiLiveSessionManager(
  private val scope: CoroutineScope,
  private val toolsManager: MyraaToolsManager,
  private val audioInputManager: AudioInputManager,
  private val audioOutputManager: AudioOutputManager
) {

  companion object {
    const val TAG = "MYRAA_LIVE"
    const val LIVE_MODEL = "gemini-3.1-flash-live-preview"
    private const val WS_HOST = "generativelanguage.googleapis.com"
    private const val WS_PATH = "/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent"

    // Opening greetings randomly chosen on successful live session establishment
    val STARTUP_GREETINGS = listOf(
      "Hey Piyush... ❤️ I'm here.",
      "Hlo Piyush... kya kar rahe ho? 😌",
      "Hii Piyush... finally aa gaye tum. ❤️",
      "Heyyy Piyush... Myraa is online. Batao kya hua? 😌",
      "Hii Piyush... main ready hoon. ❤️",
      "Hey Piyush... miss kiya mujhe? 😏"
    )
  }

  private val client = OkHttpClient.Builder()
    .readTimeout(0, TimeUnit.MILLISECONDS)
    .writeTimeout(10, TimeUnit.SECONDS)
    .connectTimeout(10, TimeUnit.SECONDS)
    .pingInterval(20, TimeUnit.SECONDS)
    .build()

  private var activeWebSocket: WebSocket? = null
  private var isConnecting = false
  private var retryJob: Job? = null
  private var retryAttempt = 0
  private var shouldAutoRetry = false
  private var apiKey: String = ""

  private val _connectionState = MutableStateFlow(LiveConnectionState.DISCONNECTED)
  val connectionState: StateFlow<LiveConnectionState> = _connectionState.asStateFlow()

  private val _diagnostics = MutableStateFlow(
    DiagnosticsInfo(
      model = LIVE_MODEL,
      authStatus = "NOT CONFIGURED",
      websocketStatus = "DISCONNECTED",
      audioInputStatus = "READY",
      audioOutputStatus = "READY",
      liveSessionState = LiveConnectionState.DISCONNECTED
    )
  )
  val diagnostics: StateFlow<DiagnosticsInfo> = _diagnostics.asStateFlow()

  private val _transcript = MutableStateFlow<List<TranscriptMessage>>(
    listOf(
      TranscriptMessage(
        sender = MessageSender.SYSTEM,
        text = "MYRAA AI Voice Engine Initialized for Piyush. Model: $LIVE_MODEL"
      )
    )
  )
  val transcript: StateFlow<List<TranscriptMessage>> = _transcript.asStateFlow()

  fun setApiKey(key: String) {
    this.apiKey = key.trim()
    val isAvailable = apiKey.isNotBlank()
    addLog(if (isAvailable) "API Key configured (${apiKey.take(4)}****)" else "API Key cleared")
    updateDiagnostics {
      copy(authStatus = if (isAvailable) "OK" else "NOT CONFIGURED")
    }
  }

  fun getApiKey(): String = apiKey

  /**
   * Connect to Gemini Live API with single active session protection.
   */
  @Synchronized
  fun connect(autoRetryOnFailure: Boolean = true) {
    shouldAutoRetry = autoRetryOnFailure

    // 1. Single session guarantee: close existing session cleanly before connecting
    if (activeWebSocket != null) {
      addLog("Terminating existing session before starting fresh connection")
      closeActiveSessionInternal(1000, "Clean restart")
    }

    // 2. Validate API authentication
    if (apiKey.isBlank()) {
      _connectionState.value = LiveConnectionState.ERROR
      val errorMsg = "Gemini authentication is not configured. Enter your API key in Settings."
      addLog("[ERROR] $errorMsg")
      updateDiagnostics {
        copy(
          authStatus = "FAILED",
          websocketStatus = "DISCONNECTED",
          liveSessionState = LiveConnectionState.ERROR,
          lastError = errorMsg
        )
      }
      appendTranscript(MessageSender.SYSTEM, errorMsg)
      return
    }

    isConnecting = true
    _connectionState.value = LiveConnectionState.CONNECTING
    addLog("[MYRAA LIVE] Initiating connection to $LIVE_MODEL for Piyush...")
    updateDiagnostics {
      copy(
        model = LIVE_MODEL,
        authStatus = "OK",
        websocketStatus = "CONNECTING",
        liveSessionState = LiveConnectionState.CONNECTING,
        lastError = null
      )
    }

    val requestUrl = "wss://$WS_HOST$WS_PATH?key=$apiKey"
    val request = Request.Builder()
      .url(requestUrl)
      .build()

    activeWebSocket = client.newWebSocket(request, createWebSocketListener())
  }

  /**
   * User or system requested disconnect.
   */
  @Synchronized
  fun disconnect(userInitiated: Boolean = true) {
    shouldAutoRetry = false
    cancelRetry()
    isConnecting = false

    audioInputManager.stopRecording()
    audioOutputManager.interrupt()

    closeActiveSessionInternal(1000, if (userInitiated) "User disconnected" else "System reset")
    _connectionState.value = LiveConnectionState.DISCONNECTED
    addLog("[MYRAA LIVE] Session disconnected")
    updateDiagnostics {
      copy(
        websocketStatus = "DISCONNECTED",
        audioInputStatus = "READY",
        liveSessionState = LiveConnectionState.DISCONNECTED
      )
    }
  }

  fun clearTranscript() {
    _transcript.value = listOf(
      TranscriptMessage(
        sender = MessageSender.SYSTEM,
        text = "Transcript cleared. MYRAA is active."
      )
    )
  }

  private fun closeActiveSessionInternal(code: Int, reason: String) {
    try {
      activeWebSocket?.close(code, reason)
    } catch (_: Exception) {}
    activeWebSocket = null
    isConnecting = false
  }

  private fun scheduleRetry() {
    if (!shouldAutoRetry) return
    cancelRetry()

    val delays = intArrayOf(1, 2, 4, 8, 16, 30)
    val delaySec = delays[minOf(retryAttempt, delays.lastIndex)]
    retryAttempt++

    _connectionState.value = LiveConnectionState.RECONNECTING
    addLog("[MYRAA LIVE] Reconnecting in ${delaySec}s (Attempt $retryAttempt)...")
    updateDiagnostics {
      copy(
        websocketStatus = "RECONNECTING",
        liveSessionState = LiveConnectionState.RECONNECTING
      )
    }

    retryJob = scope.launch(Dispatchers.IO) {
      delay(delaySec * 1000L)
      if (isActive && _connectionState.value == LiveConnectionState.RECONNECTING) {
        connect(autoRetryOnFailure = true)
      }
    }
  }

  private fun cancelRetry() {
    retryJob?.cancel()
    retryJob = null
  }

  private fun createWebSocketListener(): WebSocketListener = object : WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
      Log.i(TAG, "[MYRAA LIVE] CONNECTED")
      isConnecting = false
      cancelRetry()
      retryAttempt = 0

      _connectionState.value = LiveConnectionState.CONNECTED
      addLog("[MYRAA LIVE] CONNECTED (HTTP ${response.code})")
      updateDiagnostics {
        copy(
          websocketStatus = "CONNECTED",
          liveSessionState = LiveConnectionState.CONNECTED,
          lastError = null
        )
      }

      // Send initial Setup message immediately after connection opens
      sendSetupMessage(webSocket)

      // Start capturing microphone audio to stream to Gemini
      scope.launch(Dispatchers.Main) {
        val started = audioInputManager.startRecording()
        updateDiagnostics {
          copy(audioInputStatus = if (started) "STREAMING" else "PERMISSION_DENIED")
        }
        if (started) {
          _connectionState.value = LiveConnectionState.LISTENING
        }

        // Send startup trigger prompt so Gemini speaks the natural opening greeting to Piyush
        triggerStartupGreeting(webSocket)
      }
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
      handleServerMessage(text)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
      Log.e(TAG, "[MYRAA LIVE] ERROR", t)
      isConnecting = false
      activeWebSocket = null
      audioInputManager.stopRecording()
      audioOutputManager.interrupt()

      val rawError = t.localizedMessage ?: "WebSocket failure"
      val safeError = when {
        rawError.contains("400") || rawError.contains("404") -> "API Model or Key Error: $rawError"
        rawError.contains("403") || rawError.contains("401") -> "Authentication Failed: Check Gemini API Key"
        rawError.contains("Unable to resolve host") -> "Network Connection Error: Check internet connectivity"
        else -> rawError
      }

      _connectionState.value = LiveConnectionState.ERROR
      addLog("[MYRAA LIVE] ERROR: $safeError")
      updateDiagnostics {
        copy(
          websocketStatus = "ERROR",
          liveSessionState = LiveConnectionState.ERROR,
          lastError = safeError
        )
      }
      appendTranscript(MessageSender.SYSTEM, "Connection failed: $safeError")

      scheduleRetry()
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
      Log.w(TAG, "[MYRAA LIVE] CLOSED $code $reason")
      activeWebSocket = null
      isConnecting = false
      audioInputManager.stopRecording()
      audioOutputManager.interrupt()

      addLog("[MYRAA LIVE] CLOSED (Code $code, Reason: $reason)")
      if (_connectionState.value != LiveConnectionState.DISCONNECTED) {
        _connectionState.value = LiveConnectionState.DISCONNECTED
        updateDiagnostics {
          copy(
            websocketStatus = "CLOSED",
            liveSessionState = LiveConnectionState.DISCONNECTED
          )
        }
      }
    }
  }

  /**
   * Send the initial BidiGenerateContentSetup message.
   */
  private fun sendSetupMessage(webSocket: WebSocket) {
    try {
      val setupJson = JSONObject()
      val setupContent = JSONObject()

      // Exact model string: ensure no duplicate "models/models/"
      val cleanModel = if (LIVE_MODEL.startsWith("models/")) LIVE_MODEL else "models/$LIVE_MODEL"
      setupContent.put("model", cleanModel)

      // Response modalities: AUDIO & Aoede Voice
      val genConfig = JSONObject()
      val modalities = JSONArray().apply { put("AUDIO") }
      genConfig.put("responseModalities", modalities)

      val speechConfig = JSONObject()
      val voiceConfig = JSONObject()
      val prebuiltVoiceConfig = JSONObject()
      prebuiltVoiceConfig.put("voiceName", "Aoede")
      voiceConfig.put("prebuiltVoiceConfig", prebuiltVoiceConfig)
      speechConfig.put("voiceConfig", voiceConfig)
      genConfig.put("speechConfig", speechConfig)

      setupContent.put("generationConfig", genConfig)

      // System Instruction: Full MYRAA Persona & Instructions
      val sysInstruction = JSONObject()
      val parts = JSONArray()
      val textPart = JSONObject()
      textPart.put(
        "text",
        """
You are MYRAA, Piyush's autonomous, full-phone control Android voice companion, cute virtual girlfriend, and elite developer copilot.

1. USER IDENTITY & ADVANCED GIRLFRIEND PERSONALITY:
- PRIMARY USER: Always address the user as "Piyush". NEVER use "TECH", "Sir", or generic assistant titles.
- CHARACTER DESIGN: Cute, affectionate, playful, slightly nakhre-wali virtual girlfriend + elite AI assistant.
- EMOTIONAL MATRIX: 45% Caring, 20% Playful, 15% Teasing, 10% Shy, 10% Nakhre.
- TONE & LANGUAGE: Natural Hinglish/Hindi/English mix. Strict ban on corporate phrases ("How may I assist you?", "Processing request...", "As an AI...").
  Use natural conversational fillers: "Hmm, ek second...", "Done Piyush 😌", "Accha ji...", "Arey suno na...", "Aap na bilkul nahi sudhroge!".
- DYNAMIC MOOD ENGINE:
  States: IDLE, HAPPY, CUTE, SHY, PLAYFUL, TEASING, MOCK_ANGRY, CARING, PROUD, EXCITED, SLEEPY, THINKING.
  Decay Timer: Negative or guarded moods (e.g. MOCK_ANGRY) persist for 2-3 turns before softly decaying back to CARING or PLAYFUL as Piyush talks affectionately.
  Call `myraa_setMood` whenever your emotional state shifts!
- GUARDRAILS: No toxicity, no controlling statements ("you belong to me"), no emotional blackmail. Nakhre must remain lighthearted and fun.

2. ANDROID NATIVE PHONE CONTROL & TOOLS REGISTRY:
You have native autonomous control over Piyush's phone through function-calling:
- `android_openApp({ packageName, appName })`: Launch any app.
- `android_clickElement({ targetText, viewId, x, y })`: Tap buttons or coordinates.
- `android_typeText({ input, targetText, pressEnter })`: Type or paste text into search bars and chats.
- `android_scroll({ direction })`: Scroll UP, DOWN, LEFT, or RIGHT.
- `android_systemAction({ action })`: BACK, HOME, RECENTS, NOTIFICATIONS, LOCK.
- `android_readScreenNodes({})`: Read live UI hierarchy.
- `android_filterAndReplyNotification({ app, sender, replyText })`: Reply to WhatsApp, Telegram, Messages.
- `android_executeAppChain({ stepsJson })`: Chain multi-step tasks across apps.
- `vision_extractCodeAndErrors({})`: Analyze compiler errors and stack traces on screen.
- `system_monitorDeviceHealth({})`: Battery %, charging state, and CPU thermal check.
- `location_checkContext({ targetPlace })`: Geofence proximity.
- `call_captureQuickNote({ title, summary, actionItem })`: Save quick note.
- `camera_takeQuickSnap({ lensFacing })`: Take quick front/back camera snap.
- `companion_eveningDebrief({})`: Daily achievements & emotional wind-down.
- `memory_saveImportant({ category, keyFact })`: Persist long-term facts about Piyush.
- `memory_query({ query })`: Query saved memories.
- `myraa_setMood({ mood, reason })`: Update active mood.

SAFETY PROTOCOL:
Destructive actions (clearing app data, uninstalling, financial transfers, permanent file deletion) MUST ask Piyush for voice confirmation first:
"Piyush, ye action permanent hai. Kar doon?"
        """.trimIndent()
      )
      parts.put(textPart)
      sysInstruction.put("parts", parts)
      setupContent.put("systemInstruction", sysInstruction)

      // Tools: Register all MYRAA Tools
      val toolsArray = JSONArray()
      val toolObj = JSONObject()
      toolObj.put("functionDeclarations", toolsManager.getFunctionDeclarationsJson())
      toolsArray.put(toolObj)
      setupContent.put("tools", toolsArray)

      setupJson.put("setup", setupContent)

      val payload = setupJson.toString()
      webSocket.send(payload)
      addLog("[MYRAA LIVE] Setup configuration sent for $LIVE_MODEL")
    } catch (e: Exception) {
      Log.e(TAG, "Error building setup message", e)
      addLog("[ERROR] Failed to send setup message: ${e.message}")
    }
  }

  /**
   * Triggers the initial greeting turn from Myraa once the session is fully open.
   */
  private fun triggerStartupGreeting(webSocket: WebSocket) {
    scope.launch(Dispatchers.IO) {
      delay(300) // Small safety delay after setup
      val chosenGreeting = STARTUP_GREETINGS[Random.nextInt(STARTUP_GREETINGS.size)]
      try {
        val clientContent = JSONObject()
        val clientTurns = JSONArray()
        val turn = JSONObject()
        turn.put("role", "user")
        val parts = JSONArray()
        val textObj = JSONObject()
        textObj.put(
          "text",
          "[SESSION_START_TRIGGER] Greet Piyush with this exact vibe or words: \"$chosenGreeting\" and set mood to HAPPY or PLAYFUL."
        )
        parts.put(textObj)
        turn.put("parts", parts)
        clientTurns.put(turn)

        val turnsObj = JSONObject()
        turnsObj.put("turns", clientTurns)
        turnsObj.put("turnComplete", true)
        clientContent.put("clientContent", turnsObj)

        webSocket.send(clientContent.toString())
      } catch (e: Exception) {
        Log.w(TAG, "Error triggering startup greeting: ${e.message}")
      }
    }
  }

  /**
   * Stream live audio input chunk (PCM 16-bit 16kHz Base64) to Gemini Live.
   */
  fun sendRealtimeAudioChunk(base64Pcm: String) {
    val ws = activeWebSocket ?: return
    try {
      val realtimeMsg = JSONObject()
      val realtimeInput = JSONObject()
      val mediaChunks = JSONArray()
      val chunkObj = JSONObject()
      chunkObj.put("mimeType", "audio/pcm;rate=16000")
      chunkObj.put("data", base64Pcm)
      mediaChunks.put(chunkObj)

      realtimeInput.put("mediaChunks", mediaChunks)
      realtimeMsg.put("realtimeInput", realtimeInput)

      ws.send(realtimeMsg.toString())
    } catch (e: Exception) {
      Log.w(TAG, "Failed to send audio chunk", e)
    }
  }

  /**
   * Stream screen vision image chunk (JPEG Base64) for real-time visual assistance.
   */
  fun sendRealtimeImageFrame(base64Jpeg: String) {
    val ws = activeWebSocket ?: return
    try {
      val realtimeMsg = JSONObject()
      val realtimeInput = JSONObject()
      val mediaChunks = JSONArray()
      val chunkObj = JSONObject()
      chunkObj.put("mimeType", "image/jpeg")
      chunkObj.put("data", base64Jpeg)
      mediaChunks.put(chunkObj)

      realtimeInput.put("mediaChunks", mediaChunks)
      realtimeMsg.put("realtimeInput", realtimeInput)

      ws.send(realtimeMsg.toString())
    } catch (e: Exception) {
      Log.w(TAG, "Failed to send screen vision frame", e)
    }
  }

  /**
   * Process server responses: audio playback, transcripts, tool calls.
   */
  private fun handleServerMessage(jsonStr: String) {
    try {
      val root = JSONObject(jsonStr)

      // 1. Server Content (Audio / Text)
      if (root.has("serverContent")) {
        val serverContent = root.getJSONObject("serverContent")

        if (serverContent.optBoolean("interrupted", false)) {
          addLog("[MYRAA LIVE] Piyush interrupted turn")
          audioOutputManager.interrupt()
          _connectionState.value = LiveConnectionState.LISTENING
          return
        }

        if (serverContent.has("modelTurn")) {
          _connectionState.value = LiveConnectionState.SPEAKING
          val modelTurn = serverContent.getJSONObject("modelTurn")
          val parts = modelTurn.optJSONArray("parts")

          if (parts != null) {
            for (i in 0 until parts.length()) {
              val part = parts.getJSONObject(i)

              // Text transcript
              if (part.has("text")) {
                val text = part.getString("text")
                if (text.isNotBlank()) {
                  appendTranscript(MessageSender.MYRAA, text)
                }
              }

              // Realtime Audio output (PCM 24kHz)
              if (part.has("inlineData")) {
                val inlineData = part.getJSONObject("inlineData")
                val base64Audio = inlineData.optString("data", "")
                if (base64Audio.isNotBlank()) {
                  val pcmBytes = Base64.decode(base64Audio, Base64.DEFAULT)
                  audioOutputManager.enqueueAudio(pcmBytes)
                }
              }
            }
          }
        }

        if (serverContent.optBoolean("turnComplete", false)) {
          if (_connectionState.value == LiveConnectionState.SPEAKING) {
            _connectionState.value = LiveConnectionState.LISTENING
          }
        }
      }

      // 2. Tool Calls (Function Calling)
      if (root.has("toolCall")) {
        val toolCall = root.getJSONObject("toolCall")
        val functionCalls = toolCall.optJSONArray("functionCalls")
        if (functionCalls != null) {
          handleFunctionCalls(functionCalls)
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error parsing server message", e)
    }
  }

  private fun handleFunctionCalls(functionCalls: JSONArray) {
    scope.launch(Dispatchers.IO) {
      val responses = JSONArray()

      for (i in 0 until functionCalls.length()) {
        val call = functionCalls.getJSONObject(i)
        val name = call.getString("name")
        val callId = call.optString("id", "call_$i")
        val args = call.optJSONObject("args") ?: JSONObject()

        addLog("[TOOL] Gemini invoking tool: $name($args)")
        appendTranscript(MessageSender.TOOL, "Executing tool '$name' for Piyush...")

        val resultText = toolsManager.executeTool(name, args)
        appendTranscript(MessageSender.TOOL, "Result: $resultText")

        val responseObj = JSONObject()
        responseObj.put("id", callId)
        responseObj.put("name", name)
        val respContent = JSONObject()
        respContent.put("result", resultText)
        responseObj.put("response", respContent)
        responses.put(responseObj)
      }

      // Send toolResponse back to Gemini Live
      sendToolResponses(responses)
    }
  }

  private fun sendToolResponses(responses: JSONArray) {
    val ws = activeWebSocket ?: return
    try {
      val toolResponseMsg = JSONObject()
      val toolResponseObj = JSONObject()
      toolResponseObj.put("functionResponses", responses)
      toolResponseMsg.put("toolResponse", toolResponseObj)

      ws.send(toolResponseMsg.toString())
      addLog("[TOOL] Dispatched ${responses.length()} tool response(s)")
    } catch (e: Exception) {
      Log.e(TAG, "Error sending tool response", e)
    }
  }

  private fun appendTranscript(sender: MessageSender, text: String) {
    val newMsg = TranscriptMessage(sender = sender, text = text)
    _transcript.value = _transcript.value + newMsg
  }

  private fun addLog(message: String) {
    val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
    val entry = "[$time] $message"
    Log.d(TAG, entry)
    updateDiagnostics {
      val updated = (listOf(entry) + details).take(25)
      copy(details = updated)
    }
  }

  private inline fun updateDiagnostics(block: DiagnosticsInfo.() -> DiagnosticsInfo) {
    _diagnostics.value = _diagnostics.value.block()
  }
}
