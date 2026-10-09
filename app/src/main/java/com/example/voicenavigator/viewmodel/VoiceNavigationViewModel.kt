package com.example.voicenavigator.viewmodel

import androidx.lifecycle.ViewModel
import com.example.voicenavigator.model.AppState
import com.example.voicenavigator.model.Device
import com.example.voicenavigator.model.Screen
import com.example.voicenavigator.model.VoiceCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class VoiceNavigationViewModel : ViewModel() {

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    // ============================================================
    // API pública
    // ============================================================

    fun onCommand(command: VoiceCommand) {
        _state.update { current ->
            val next = reduce(current, command)
            next.copy(lastCommand = command)
        }
    }

    fun onMicStatusChanged(listening: Boolean) {
        _state.update { it.copy(isListening = listening) }
    }

    fun onDetection(command: VoiceCommand, label: String, confidence: Float) {
        _state.update {
            it.copy(
                lastDetectedLabel = label,
                lastDetectedConfidence = confidence
            )
        }
        onCommand(command)
    }

    fun clearDetection() {
        _state.update {
            it.copy(lastDetectedLabel = null, lastDetectedConfidence = 0f)
        }
    }

    // ============================================================
    // Reductor principal
    // ============================================================

    private fun reduce(state: AppState, command: VoiceCommand): AppState {
        return when (state.currentScreen) {
            Screen.DASHBOARD -> reduceDashboard(state, command)
            Screen.DEVICE -> reduceDevice(state, command)
            Screen.ROUTINE -> reduceRoutine(state, command)
            Screen.CONFIRM -> reduceConfirm(state, command)
        }
    }

    // ============================================================
    // Helpers de modo por dispositivo
    // ============================================================

    private fun modeDisplayName(device: Device?): String = when (device) {
        Device.TV -> "Mute"
        Device.FAN -> "Max Speed"
        Device.LIGHTS -> "Brightness mode"
        null -> "Mode"
    }

    private fun modeCurrentValue(state: AppState): Boolean = when (state.selectedDevice) {
        Device.TV -> state.mute
        Device.FAN -> state.maxSpeed
        Device.LIGHTS -> state.brightnessMode
        null -> false
    }

    private fun setModeValue(state: AppState, newValue: Boolean): AppState = when (state.selectedDevice) {
        Device.TV -> state.copy(mute = newValue)
        Device.FAN -> state.copy(maxSpeed = newValue)
        Device.LIGHTS -> state.copy(brightnessMode = newValue)
        null -> state
    }

    // ============================================================
    // Dashboard
    // ============================================================

    private fun reduceDashboard(state: AppState, command: VoiceCommand): AppState {
        val maxIndex = 3
        return when (command) {
            VoiceCommand.UP, VoiceCommand.LEFT -> state.copy(
                focusedIndex = (state.focusedIndex - 1).coerceAtLeast(0),
                statusMessage = "Focus moved"
            )
            VoiceCommand.DOWN, VoiceCommand.RIGHT -> state.copy(
                focusedIndex = (state.focusedIndex + 1).coerceAtMost(maxIndex),
                statusMessage = "Focus moved"
            )
            VoiceCommand.YES -> when (state.focusedIndex) {
                0 -> state.copy(
                    currentScreen = Screen.DEVICE,
                    selectedDevice = Device.LIGHTS,
                    focusedIndex = 0,
                    statusMessage = "Lights selected"
                )
                1 -> state.copy(
                    currentScreen = Screen.DEVICE,
                    selectedDevice = Device.FAN,
                    focusedIndex = 0,
                    statusMessage = "Fan selected"
                )
                2 -> state.copy(
                    currentScreen = Screen.DEVICE,
                    selectedDevice = Device.TV,
                    focusedIndex = 0,
                    statusMessage = "TV selected"
                )
                3 -> state.copy(
                    currentScreen = Screen.ROUTINE,
                    routineIndex = 0,
                    focusedIndex = 0,
                    statusMessage = "Routines opened"
                )
                else -> state
            }
            VoiceCommand.OFF -> state.copy(
                currentScreen = Screen.CONFIRM,
                confirmationMessage = "Turn off all devices?",
                statusMessage = "Confirmation required"
            )
            else -> state.copy(statusMessage = "Command not applicable here")
        }
    }

    // ============================================================
    // Device
    // ============================================================
    // Foco:
    //   0 = Turn On
    //   1 = Turn Off
    //   2 = Modo específico (Brightness / Max Speed / Mute)
    // "no" siempre vuelve al dashboard.
    // "yes" sobre el modo abre Confirm para habilitarlo o deshabilitarlo.

    private fun reduceDevice(state: AppState, command: VoiceCommand): AppState {
        val maxIndex = 2
        return when (command) {
            VoiceCommand.UP, VoiceCommand.LEFT -> state.copy(
                focusedIndex = (state.focusedIndex - 1).coerceAtLeast(0)
            )
            VoiceCommand.DOWN, VoiceCommand.RIGHT -> state.copy(
                focusedIndex = (state.focusedIndex + 1).coerceAtMost(maxIndex)
            )
            VoiceCommand.ON -> state.copy(
                deviceOn = true,
                statusMessage = "${state.selectedDevice?.displayName} turned ON"
            )
            VoiceCommand.OFF -> state.copy(
                deviceOn = false,
                statusMessage = "${state.selectedDevice?.displayName} turned OFF"
            )
            VoiceCommand.YES -> when (state.focusedIndex) {
                0 -> state.copy(deviceOn = true, statusMessage = "Turned ON")
                1 -> state.copy(deviceOn = false, statusMessage = "Turned OFF")
                2 -> {
                    val modeName = modeDisplayName(state.selectedDevice)
                    val current = modeCurrentValue(state)
                    val action = if (current) "Disable" else "Enable"
                    state.copy(
                        currentScreen = Screen.CONFIRM,
                        confirmationMessage = "$action $modeName?",
                        statusMessage = "Confirmation required"
                    )
                }
                else -> state
            }
            VoiceCommand.NO -> backToDashboard(state)
            else -> state.copy(statusMessage = "Command not applicable here")
        }
    }

    // ============================================================
    // Routine
    // ============================================================

    private fun reduceRoutine(state: AppState, command: VoiceCommand): AppState {
        val maxIndex = 2
        return when (command) {
            VoiceCommand.UP, VoiceCommand.LEFT -> state.copy(
                routineIndex = (state.routineIndex - 1).coerceAtLeast(0)
            )
            VoiceCommand.DOWN, VoiceCommand.RIGHT -> state.copy(
                routineIndex = (state.routineIndex + 1).coerceAtMost(maxIndex)
            )
            VoiceCommand.GO -> state.copy(
                routineRunning = true,
                statusMessage = "Routine started"
            )
            VoiceCommand.STOP -> state.copy(
                routineRunning = false,
                statusMessage = "Routine stopped"
            )
            VoiceCommand.NO -> backToDashboard(state)
            else -> state.copy(statusMessage = "Command not applicable here")
        }
    }

    // ============================================================
    // Confirm
    // ============================================================

    private fun reduceConfirm(state: AppState, command: VoiceCommand): AppState {
        // Detectar si la confirmación viene de un modo por dispositivo
        val isModeConfirmation = state.confirmationMessage.contains("Mute", true) ||
                state.confirmationMessage.contains("Max Speed", true) ||
                state.confirmationMessage.contains("Brightness", true)

        return when (command) {
            VoiceCommand.YES -> {
                if (isModeConfirmation) {
                    // Toggle real del modo
                    val current = modeCurrentValue(state)
                    val updated = setModeValue(state, !current)
                    updated.copy(
                        currentScreen = Screen.DEVICE,
                        focusedIndex = 2,
                        statusMessage = "${modeDisplayName(state.selectedDevice)} actualizado"
                    )
                } else {
                    // Confirmación global (ej: turn off all devices)
                    state.copy(
                        currentScreen = Screen.DASHBOARD,
                        focusedIndex = 0,
                        deviceOn = false,
                        statusMessage = "Action confirmed"
                    )
                }
            }
            VoiceCommand.NO -> {
                if (isModeConfirmation) {
                    // Cancelar: no cambia el modo, vuelve al Device screen
                    state.copy(
                        currentScreen = Screen.DEVICE,
                        focusedIndex = 2,
                        statusMessage = "Action cancelled"
                    )
                } else {
                    state.copy(
                        currentScreen = Screen.DASHBOARD,
                        focusedIndex = 0,
                        statusMessage = "Action cancelled"
                    )
                }
            }
            else -> state
        }
    }

    // ============================================================
    // Helpers
    // ============================================================

    private fun backToDashboard(state: AppState): AppState =
        state.copy(
            currentScreen = Screen.DASHBOARD,
            focusedIndex = 0,
            statusMessage = "Back to dashboard"
        )
}