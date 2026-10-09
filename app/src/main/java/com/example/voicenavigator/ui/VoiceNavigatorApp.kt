package com.example.voicenavigator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.voicenavigator.model.AppState
import com.example.voicenavigator.model.Screen
import com.example.voicenavigator.ui.screens.ConfirmScreen
import com.example.voicenavigator.ui.screens.DashboardScreen
import com.example.voicenavigator.ui.screens.DeviceScreen
import com.example.voicenavigator.ui.screens.RoutineScreen

private val PurpleAccent = Color(0xFF7C5CFF)
private val CyanAccent = Color(0xFF64D2FF)
private val AmberAccent = Color(0xFFFBBF24)
private val MutedText = Color(0xFF9AA3B5)
private val CardBg = Color(0xFF1A2030)
private val SoftBorder = Color(0xFF2A3145)

@Composable
fun VoiceNavigatorApp(state: AppState) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TopTabs(currentScreen = state.currentScreen)
            ListeningBar(state = state)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                when (state.currentScreen) {
                    Screen.DASHBOARD -> DashboardScreen(state)
                    Screen.DEVICE -> DeviceScreen(state)
                    Screen.ROUTINE -> RoutineScreen(state)
                    Screen.CONFIRM -> ConfirmScreen(state)
                }
            }
        }
    }
}

@Composable
private fun TopTabs(currentScreen: Screen) {
    val tabs = listOf(
        "Dashboard" to Screen.DASHBOARD,
        "Device" to Screen.DEVICE,
        "Routine" to Screen.ROUTINE,
        "Confirm" to Screen.CONFIRM
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { (label, screen) ->
            val active = screen == currentScreen
            Box(
                modifier = Modifier
                    .background(
                        color = if (active) PurpleAccent else Color(0xFF1A2030),
                        shape = RoundedCornerShape(50)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    color = if (active) Color.White else MutedText
                )
            }
        }
    }
}

@Composable
private fun ListeningBar(state: AppState) {
    val hint = when (state.currentScreen) {
        Screen.DASHBOARD -> "Listening: navigation commands"
        Screen.DEVICE -> "Listening: on / off / yes / no"
        Screen.ROUTINE -> "Listening: go / stop / up / down"
        Screen.CONFIRM -> "Listening: yes / no"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, SoftBorder)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = if (state.isListening) CyanAccent else Color.Gray,
                            shape = CircleShape
                        )
                )
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE6E9F2),
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Última detección con confianza
            state.lastDetectedLabel?.let { label ->
                val pct = (state.lastDetectedConfidence * 100).toInt()
                val color = if (state.lastDetectedConfidence >= 0.6f) PurpleAccent else AmberAccent
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Detected:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText
                    )
                    Box(
                        modifier = Modifier
                            .background(color, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "$label  ($pct%)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}