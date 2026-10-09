package com.example.voicenavigator.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.math.sqrt

/**
 * Captura audio continuo a 16 kHz mono PCM 16-bit con `AudioRecord`.
 *
 * Estrategia:
 *   - Un buffer circular interno acumula muestras hasta tener 16000 (1 s).
 *   - Cuando se llena, calcula el RMS y si supera un umbral (hay voz probable),
 *     emite el buffer completo por `onAudioReady`.
 *   - Luego espera un `cooldown` antes de aceptar el siguiente buffer, para
 *     no clasificar continuamente.
 *
 * El consumidor (`VoiceRecognizer`) es responsable de llamar a `start()` y `stop()`.
 */
class AudioRecorder(
    private val onAudioReady: (ShortArray) -> Unit,
    private val onError: (String) -> Unit = {},
    private val rmsThreshold: Float = 0.02f,
    private val cooldownMs: Long = 700L
) {
    companion object {
        const val SAMPLE_RATE = 16_000
        const val BUFFER_SAMPLES = 16_000
    }

    private var audioRecord: AudioRecord? = null
    private var recording = false
    private var thread: Thread? = null

    @Volatile
    private var lastEmitTime = 0L

    @SuppressLint("MissingPermission")
    fun start() {
        if (recording) return

        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuf <= 0) {
            onError("AudioRecord: buffer size inválido ($minBuf)")
            return
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuf * 4
            )
        } catch (e: Exception) {
            onError("No se pudo crear AudioRecord: ${e.message}")
            return
        }

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            onError("AudioRecord no inicializado")
            audioRecord?.release()
            audioRecord = null
            return
        }

        recording = true
        audioRecord?.startRecording()

        thread = Thread {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)
            val readChunk = ShortArray(1600)   // 100 ms por lectura
            val accumulator = ShortArray(BUFFER_SAMPLES)
            var filled = 0

            while (recording) {
                val read = audioRecord?.read(readChunk, 0, readChunk.size) ?: 0
                if (read <= 0) continue

                var offset = 0
                while (offset < read) {
                    val space = BUFFER_SAMPLES - filled
                    val copy = minOf(space, read - offset)
                    System.arraycopy(readChunk, offset, accumulator, filled, copy)
                    filled += copy
                    offset += copy

                    if (filled == BUFFER_SAMPLES) {
                        val rms = computeRms(accumulator)
                        val now = System.currentTimeMillis()
                        val quietEnough = (now - lastEmitTime) > cooldownMs

                        if (rms >= rmsThreshold && quietEnough) {
                            lastEmitTime = now
                            onAudioReady(accumulator.copyOf())
                        }
                        // Reiniciar buffer deslizando la mitad (50% overlap)
                        val half = BUFFER_SAMPLES / 2
                        System.arraycopy(accumulator, half, accumulator, 0, half)
                        filled = half
                    }
                }
            }
        }.apply { start() }
    }

    fun stop() {
        recording = false
        thread?.join(500)
        thread = null
        try {
            audioRecord?.stop()
        } catch (_: Exception) {
        }
        audioRecord?.release()
        audioRecord = null
    }

    private fun computeRms(buf: ShortArray): Float {
        var sum = 0.0
        for (v in buf) {
            val f = v / 32768.0
            sum += f * f
        }
        return sqrt(sum / buf.size).toFloat()
    }
}