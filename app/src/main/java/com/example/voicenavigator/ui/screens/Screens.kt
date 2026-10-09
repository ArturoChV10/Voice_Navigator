package com.example.voicenavigator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.voicenavigator.model.AppState
import com.example.voicenavigator.model.Device

// -------------------- Paleta --------------------
private val PurpleAccent = Color(0xFF7C5CFF)
private val CyanAccent = Color(0xFF64D2FF)
private val AmberAccent = Color(0xFFFBBF24)
private val MutedText = Color(0xFF9AA3B5)
private val CardBg = Color(0xFF1A2030)
private val CardBgFocused = Color(0xFF242A48)
private val SoftBorder = Color(0xFF2A3145)

// -------------------- Helpers comunes --------------------

@Composable
private fun ScreenHeader(title: String, screenNumber: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE6E9F2)
        )
        Text(
            text = "Screen $screenNumber",
            style = MaterialTheme.typography.labelMedium,
            color = MutedText
        )
    }
}

@Composable
private fun VoiceFlowBox(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(12.dp))
            .border(1.dp, SoftBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = "Voice flow: ",
                style = MaterialTheme.typography.bodySmall,
                color = PurpleAccent,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MutedText
            )
        }
    }
}

@Composable
private fun StatusBadge(text: String, active: Boolean, activeColor: Color = PurpleAccent) {
    val bg = if (active) activeColor else Color(0xFF2A3145)
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun FocusableCard(
    focused: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (focused)
                    Modifier.border(2.dp, PurpleAccent, RoundedCornerShape(14.dp))
                else
                    Modifier.border(1.dp, SoftBorder, RoundedCornerShape(14.dp))
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (focused) CardBgFocused else CardBg
        )
    ) {
        content()
    }
}

// -------------------- DASHBOARD --------------------

@Composable
fun DashboardScreen(state: AppState) {
    val lightsOn = state.deviceOn && state.selectedDevice == Device.LIGHTS
    val fanOn = state.deviceOn && state.selectedDevice == Device.FAN
    val tvOn = state.deviceOn && state.selectedDevice == Device.TV

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        ScreenHeader("Home Dashboard", "1/4")
        Spacer(Modifier.height(16.dp))

        // Fila 1: Lights | Fan
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) {
                DashboardTile(
                    title = "Living Room Lights",
                    subtitle = "Living room and bedroom lights",
                    badgeText = if (lightsOn) "ON" else "OFF",
                    badgeActive = lightsOn,
                    focused = state.focusedIndex == 0
                )
            }
            Box(Modifier.weight(1f)) {
                DashboardTile(
                    title = "Fan",
                    subtitle = "Air circulation and comfort mode",
                    badgeText = if (fanOn) "ON" else "OFF",
                    badgeActive = fanOn,
                    focused = state.focusedIndex == 1
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        // Fila 2: TV | Routines
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) {
                DashboardTile(
                    title = "TV",
                    subtitle = "Media and ambient screen",
                    badgeText = if (tvOn) "ON" else "OFF",
                    badgeActive = tvOn,
                    focused = state.focusedIndex == 2
                )
            }
            Box(Modifier.weight(1f)) {
                DashboardTile(
                    title = "Routines",
                    subtitle = "Study, sleep, welcome scenes",
                    badgeText = if (state.routineRunning) "RUNNING" else "OPEN",
                    badgeActive = state.routineRunning,
                    focused = state.focusedIndex == 3,
                    activeColor = CyanAccent
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        VoiceFlowBox(
            "use up/down/left/right to move the focus and yes to open the selected card."
        )
    }
}

@Composable
private fun DashboardTile(
    title: String,
    subtitle: String,
    badgeText: String,
    badgeActive: Boolean,
    focused: Boolean,
    activeColor: Color = PurpleAccent
) {
    FocusableCard(focused = focused) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE6E9F2)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MutedText,
                minLines = 2
            )
            Spacer(Modifier.height(12.dp))
            StatusBadge(text = badgeText, active = badgeActive, activeColor = activeColor)
        }
    }
}

// -------------------- DEVICE --------------------

@Composable
fun DeviceScreen(state: AppState) {
    val deviceName = state.selectedDevice?.displayName ?: "Device"

    // Configuración del modo según dispositivo
    val modeTitle: String
    val modeSubtitle: String
    val modeEnabled: Boolean
    val modeBadgeText: String
    when (state.selectedDevice) {
        Device.TV -> {
            modeTitle = "Mute"
            modeSubtitle = "Mute audio after confirmation."
            modeEnabled = state.mute
            modeBadgeText = if (modeEnabled) "MUTED" else "YES"
        }
        Device.FAN -> {
            modeTitle = "Max Speed"
            modeSubtitle = "Boost fan speed after confirmation."
            modeEnabled = state.maxSpeed
            modeBadgeText = if (modeEnabled) "MAX" else "YES"
        }
        Device.LIGHTS -> {
            modeTitle = "Brightness mode"
            modeSubtitle = "Adjust scene intensity after confirmation."
            modeEnabled = state.brightnessMode
            modeBadgeText = if (modeEnabled) "BRIGHT" else "YES"
        }
        null -> {
            modeTitle = "Mode"
            modeSubtitle = ""
            modeEnabled = false
            modeBadgeText = "YES"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        ScreenHeader(deviceName, "2/4")
        Spacer(Modifier.height(16.dp))

        // Estado actual
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SoftBorder, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Current state",
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedText
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Primary device selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText
                    )
                }
                StatusBadge(
                    text = if (state.deviceOn) "ON" else "OFF",
                    active = state.deviceOn
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Turn On | Turn Off
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) {
                FocusableCard(focused = state.focusedIndex == 0) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            text = "Turn On",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE6E9F2)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Activate with command on",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText
                        )
                    }
                }
            }
            Box(Modifier.weight(1f)) {
                FocusableCard(focused = state.focusedIndex == 1) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            text = "Turn Off",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE6E9F2)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Deactivate with command off",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Modo específico del dispositivo
        FocusableCard(focused = state.focusedIndex == 2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = modeTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE6E9F2)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = modeSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText
                    )
                }
                Spacer(Modifier.height(8.dp))
                StatusBadge(
                    text = modeBadgeText,
                    active = true,
                    activeColor = if (modeEnabled) PurpleAccent else CyanAccent
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Back to dashboard
        FocusableCard(focused = false) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Back to dashboard",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE6E9F2)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Return using the command no.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText
                    )
                }
                Spacer(Modifier.height(8.dp))
                StatusBadge(
                    text = "BACK",
                    active = false
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        VoiceFlowBox(
            "on/off control the selected device; yes toggles the highlighted mode; no returns."
        )
    }
}

// -------------------- ROUTINE --------------------

@Composable
fun RoutineScreen(state: AppState) {
    val routines = listOf(
        Triple("Study Mode", "Desk light on, fan low, notifications muted.", "GO"),
        Triple("Night Mode", "Warm lights, TV off, reduced brightness.", "STOP"),
        Triple("Welcome Home", "Lights on and ambient music prepared.", "GO")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        ScreenHeader("Routines", "3/4")
        Spacer(Modifier.height(16.dp))

        routines.forEachIndexed { index, (name, desc, action) ->
            val focused = state.routineIndex == index
            val running = state.routineRunning && focused

            FocusableCard(focused = focused) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE6E9F2)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    StatusBadge(
                        text = if (running) "RUNNING" else action,
                        active = running
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(12.dp))
        VoiceFlowBox(
            "move with up/down, activate with go, interrupt with stop."
        )
    }
}

// -------------------- CONFIRM --------------------

@Composable
fun ConfirmScreen(state: AppState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        ScreenHeader("Confirm action", "4/4")
        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SoftBorder, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    text = state.confirmationMessage.ifEmpty { "Are you sure?" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE6E9F2)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "This confirmation view appears after a sensitive command. The user must explicitly say yes to accept or no to cancel and return.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) {
                FocusableCard(focused = true) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            text = "yes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE6E9F2)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Execute the action.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText
                        )
                    }
                }
            }
            Box(Modifier.weight(1f)) {
                FocusableCard(focused = false) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            text = "no",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE6E9F2)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Cancel and go back.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        VoiceFlowBox(
            "confirmation screens reduce ambiguity and make the application safer and more structured."
        )
    }
}