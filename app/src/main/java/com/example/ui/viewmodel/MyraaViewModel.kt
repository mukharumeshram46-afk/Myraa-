package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.audio.AudioInputManager
import com.example.data.audio.AudioOutputManager
import com.example.data.live.GeminiLiveSessionManager
import com.example.data.model.DiagnosticsInfo
import com.example.data.model.LiveConnectionState
import com.example.data.model.MessageSender
import com.example.data.model.MyraaMode
import com.example.data.model.MyraaMood
import com.example.data.model.TranscriptMessage
import com.example.data.tools.MyraaToolsManager
import com.example.data.wake.WakeEngineManager
import com.example.nativecontrol.MyraaAccessibilityService
import com.example.nativecontrol.MyraaForegroundAudioService
import com.example.nativecontrol.MyraaMediaProjectionService
import com.example.nativecontrol.MyraaOverlayBubbleService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Production-grade Central ViewModel for MYRAA AI Assistant.
 *
 * Capabilities:
 * - Dynamic Mood State Machine with automatic decay timers (transitions back to HAPPY/IDLE)
 * - Gemini Live WebSocket Session Management
 * - Android Native Services Control (Accessibility, MediaProjection screen capture, Foreground audio)
 * - Local Wake Engine for Piyush
 * - Audio Amplitude streaming to HolographicOrb
 */
class MyraaViewModel(application: Application) : AndroidViewModel(application) {

  val toolsManager = MyraaToolsManager(application.applicationContext)

  // Mood State Machine
  private val _currentMood = MutableStateFlow(MyraaMood.HAPPY)
  val currentMood: StateFlow<MyraaMood> = _currentMood.asStateFlow()

  private val _moodStatusReason = MutableStateFlow("Ready for Piyush")
  val moodStatusReason: StateFlow<String> = _moodStatusReason.asStateFlow()

  private var moodDecayJob: Job? = null

  private val _audioAmplitude = MutableStateFlow(0.0f)
  val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

  private val _wakeStatus = MutableStateFlow("Standby")
  val wakeStatus: StateFlow<String> = _wakeStatus.asStateFlow()

  private val _isWakeEngineEnabled = MutableStateFlow(true)
  val isWakeEngineEnabled: StateFlow<Boolean> = _isWakeEngineEnabled.asStateFlow()

  private val _isScreenVisionEnabled = MutableStateFlow(false)
  val isScreenVisionEnabled: StateFlow<Boolean> = _isScreenVisionEnabled.asStateFlow()

  val audioOutputManager: AudioOutputManager = AudioOutputManager(
    scope = viewModelScope,
    onAmplitudeChanged = { amp ->
      if (liveSessionManager.connectionState.value == LiveConnectionState.SPEAKING) {
        _audioAmplitude.value = amp
      }
    },
    onPlaybackStateChanged = { isPlaying ->
      // State handled by live session
    }
  )

  val audioInputManager: AudioInputManager = AudioInputManager(
    context = application.applicationContext,
    scope = viewModelScope,
    onAudioChunk = { base64 ->
      liveSessionManager.sendRealtimeAudioChunk(base64)
    },
    onAmplitudeChanged = { amp ->
      if (liveSessionManager.connectionState.value == LiveConnectionState.LISTENING ||
        liveSessionManager.connectionState.value == LiveConnectionState.CONNECTED
      ) {
        _audioAmplitude.value = amp
      }
    },
    onError = { err ->
      // Error handling
    }
  )

  val liveSessionManager = GeminiLiveSessionManager(
    scope = viewModelScope,
    toolsManager = toolsManager,
    audioInputManager = audioInputManager,
    audioOutputManager = audioOutputManager
  )

  val connectionState: StateFlow<LiveConnectionState> = liveSessionManager.connectionState
  val diagnostics: StateFlow<DiagnosticsInfo> = liveSessionManager.diagnostics
  val transcript: StateFlow<List<TranscriptMessage>> = liveSessionManager.transcript
  val currentMode: StateFlow<MyraaMode> = toolsManager.currentMode

  val wakeEngineManager: WakeEngineManager = WakeEngineManager(
    context = application.applicationContext,
    onWakeWordDetected = { phrase ->
      _wakeStatus.value = "Wake phrase '$phrase' detected!"
      setMood(MyraaMood.EXCITED, "Piyush called me!")
      viewModelScope.launch {
        if (connectionState.value == LiveConnectionState.DISCONNECTED ||
          connectionState.value == LiveConnectionState.ERROR
        ) {
          liveSessionManager.connect()
        }
      }
    },
    onPermissionDenied = {
      _wakeStatus.value = "Microphone permission required for Wake Engine."
    },
    onStatusMessage = { msg ->
      _wakeStatus.value = msg
    }
  )

  init {
    // Hook mood change callback from tools execution
    toolsManager.onMoodChangeRequested = { newMood, reason ->
      setMood(newMood, reason)
    }

    // Register screen capture frame stream to liveSessionManager
    MyraaMediaProjectionService.registerFrameCallback { base64Jpeg ->
      if (_isScreenVisionEnabled.value && connectionState.value == LiveConnectionState.LISTENING) {
        liveSessionManager.sendRealtimeImageFrame(base64Jpeg)
      }
    }

    // Check initial API key from BuildConfig safely
    val keyFromBuild: String? = try {
      val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
      field.get(null) as? String
    } catch (_: Exception) {
      null
    }
    if (!keyFromBuild.isNullOrBlank() && !keyFromBuild.contains("MY_GEMINI_API_KEY")) {
      liveSessionManager.setApiKey(keyFromBuild)
    }
  }

  /**
   * Sets Myraa's mood and starts decay timer toward HAPPY / IDLE.
   */
  fun setMood(mood: MyraaMood, reason: String = "") {
    _currentMood.value = mood
    if (reason.isNotBlank()) {
      _moodStatusReason.value = reason
    } else {
      _moodStatusReason.value = mood.statusTag
    }

    // Sync to floating overlay bubble if active
    try {
      MyraaOverlayBubbleService.getInstance()?.updateMood(mood)
    } catch (_: Exception) {}

    moodDecayJob?.cancel()
    if (mood.decayDurationMs > 0 && mood != MyraaMood.IDLE && mood != MyraaMood.HAPPY) {
      moodDecayJob = viewModelScope.launch {
        delay(mood.decayDurationMs)
        _currentMood.value = MyraaMood.HAPPY
        _moodStatusReason.value = "Khush & Cheerful"
      }
    }
  }

  fun launchLiveSession() {
    setMood(MyraaMood.HAPPY, "Connecting with Piyush...")
    liveSessionManager.connect(autoRetryOnFailure = true)
    // Start foreground audio service to prevent sleep
    try {
      MyraaForegroundAudioService.startService(getApplication())
    } catch (_: Exception) {}
  }

  fun disconnectLiveSession() {
    liveSessionManager.disconnect()
    _audioAmplitude.value = 0.0f
    setMood(MyraaMood.IDLE, "Session closed")
    try {
      MyraaForegroundAudioService.stopService(getApplication())
    } catch (_: Exception) {}
  }

  fun retryConnection() {
    launchLiveSession()
  }

  fun toggleWakeEngine(enabled: Boolean) {
    _isWakeEngineEnabled.value = enabled
    wakeEngineManager.setEnabled(enabled)
  }

  fun triggerManualWake() {
    wakeEngineManager.triggerManualWake()
  }

  fun setApiKey(key: String) {
    liveSessionManager.setApiKey(key)
  }

  fun setMode(mode: MyraaMode) {
    toolsManager.setMode(mode)
    when (mode) {
      MyraaMode.COMPANION -> setMood(MyraaMood.CUTE, "Girlfriend mode activated!")
      MyraaMode.DEVELOPER -> setMood(MyraaMood.THINKING, "Dev mode active for Piyush")
      MyraaMode.FOCUS -> setMood(MyraaMood.IDLE, "Focus mode on")
      MyraaMode.STUDY -> setMood(MyraaMood.THINKING, "Study mode on")
      MyraaMode.STANDARD -> setMood(MyraaMood.HAPPY, "Copilot mode active")
    }
  }

  fun toggleScreenVision(enabled: Boolean) {
    _isScreenVisionEnabled.value = enabled
  }

  override fun onCleared() {
    super.onCleared()
    liveSessionManager.disconnect()
    audioOutputManager.release()
    audioInputManager.stopRecording()
    wakeEngineManager.destroy()
    MyraaMediaProjectionService.unregisterFrameCallback()
    try {
      MyraaForegroundAudioService.stopService(getApplication())
    } catch (_: Exception) {}
  }
}

typealias NamiViewModel = MyraaViewModel
