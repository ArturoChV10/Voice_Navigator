package com.example.voicenavigator.audio

import android.content.Context
import android.util.Log

/**
 * Orquesta captura -> preprocesamiento -> inferencia.
 * Devuelve por `onCommand` el índice de la clase con mayor probabilidad.
 */
class VoiceRecognizer(
    context: Context,
    private val onCommand: (index: Int, label: String, confidence: Float) -> Unit,
    private val onError: (String) -> Unit = {}
) {
    companion object {
        private const val TAG = "VoiceRecognizer"
    }

    private val labels: List<String> = context.assets.open("labels.txt")
        .bufferedReader()
        .useLines { it.toList() }
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    private val preprocessor = AudioPreprocessor()
    private val classifier = OnnxClassifier(context)

    private val recorder = AudioRecorder(
        onAudioReady = { pcm ->
            try {
                val spec = preprocessor.preprocess(pcm)
                val (idx, logits) = classifier.classify(spec)
                val probs = classifier.softmax(logits)
                val label = labels.getOrElse(idx) { "?" }
                val conf = probs[idx]
                Log.d(TAG, "Predicción: $label ($idx) conf=${"%.3f".format(conf)}")
                onCommand(idx, label, conf)
            } catch (e: Exception) {
                Log.e(TAG, "Error en clasificación", e)
                onError("Error clasificando: ${e.message}")
            }
        },
        onError = { msg ->
            Log.e(TAG, msg)
            onError(msg)
        }
    )

    fun start() {
        if (labels.size != 10) {
            onError("labels.txt debe tener 10 clases, tiene ${labels.size}")
            return
        }
        recorder.start()
    }

    fun stop() {
        recorder.stop()
    }

    fun close() {
        stop()
        classifier.close()
    }
}