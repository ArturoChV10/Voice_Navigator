package com.example.voicenavigator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.example.voicenavigator.audio.VoiceRecognizer
import com.example.voicenavigator.model.VoiceCommand
import com.example.voicenavigator.ui.VoiceNavigatorApp
import com.example.voicenavigator.viewmodel.VoiceNavigationViewModel

private val VoiceNavigatorDarkColors = darkColorScheme(
    primary = Color(0xFF7C5CFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2A1F5C),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF64D2FF),
    onSecondary = Color.Black,
    background = Color(0xFF0A0E1A),
    onBackground = Color(0xFFE6E9F2),
    surface = Color(0xFF131826),
    onSurface = Color(0xFFE6E9F2),
    surfaceVariant = Color(0xFF1E2435),
    onSurfaceVariant = Color(0xFFB0B6C7),
    outline = Color(0xFF3A4258)
)

class MainActivity : ComponentActivity() {

    private val viewModel: VoiceNavigationViewModel by viewModels()
    private var recognizer: VoiceRecognizer? = null

    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening()
        else Toast.makeText(this, "Permiso de micrófono denegado", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = VoiceNavigatorDarkColors) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val state by viewModel.state.collectAsState()
                    VoiceNavigatorApp(state = state)
                }
            }
        }

        if (hasMicPermission()) {
            startListening()
        } else {
            requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED

    private fun startListening() {
        if (recognizer != null) return
        recognizer = VoiceRecognizer(
            context = this,
            onCommand = { index, label, confidence ->
                val cmd = VoiceCommand.fromIndex(index)
                if (cmd != null && confidence > 0.4f) {
                    runOnUiThread {
                        viewModel.onDetection(cmd, label, confidence)
                    }
                }
            },
            onError = { msg ->
                runOnUiThread {
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                }
            }
        ).also { it.start() }

        viewModel.onMicStatusChanged(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.onMicStatusChanged(false)
        recognizer?.close()
        recognizer = null
    }
}