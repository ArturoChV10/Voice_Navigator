package com.example.voicenavigator.model

enum class Screen { DASHBOARD, DEVICE, ROUTINE, CONFIRM }

enum class Device(val displayName: String) {
    LIGHTS("Lights"),
    FAN("Fan"),
    TV("TV")
}

data class AppState(
    val currentScreen: Screen = Screen.DASHBOARD,
    val focusedIndex: Int = 0,
    val selectedDevice: Device? = null,
    val deviceOn: Boolean = false,
    val routineRunning: Boolean = false,
    val routineIndex: Int = 0,
    val confirmationMessage: String = "",
    val lastCommand: VoiceCommand? = null,
    val statusMessage: String = "Say a command or tap below",

    // Feedback del micrófono
    val isListening: Boolean = false,
    val lastDetectedLabel: String? = null,
    val lastDetectedConfidence: Float = 0f,

    // Modos por dispositivo (uno por cada device)
    val brightnessMode: Boolean = false,  // Lights
    val maxSpeed: Boolean = false,        // Fan
    val mute: Boolean = false             // TV
)