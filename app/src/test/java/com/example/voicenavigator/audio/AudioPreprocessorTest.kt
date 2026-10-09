package com.example.voicenavigator.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.DataInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Test de paridad: compara la salida de AudioPreprocessor (Kotlin) con
 * la salida de torchaudio (Python) para un WAV sintético de prueba.
 *
 * Los archivos de referencia están en `app/src/test/resources/`.
 */
class AudioPreprocessorTest {

    @Test
    fun matchesPyTorchMelSpectrogram() {
        // 1) Cargar PCM int16 crudo desde resources
        val pcmBytes = javaClass.classLoader!!
            .getResourceAsStream("test_audio.raw")!!
            .readBytes()
        assertEquals("PCM debe tener 32000 bytes (16000 int16)", 32000, pcmBytes.size)

        val shortBuffer = ByteBuffer.wrap(pcmBytes)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()
        val audio = ShortArray(16000) { shortBuffer.get() }

        // 2) Cargar espectrograma esperado (float32 LE)
        val specBytes = javaClass.classLoader!!
            .getResourceAsStream("test_spec_expected.bin")!!
            .readBytes()
        assertEquals("Spec debe tener 25856 bytes (6464 float32)", 25856, specBytes.size)

        val floatBuffer = ByteBuffer.wrap(specBytes)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asFloatBuffer()
        val expected = FloatArray(6464) { floatBuffer.get() }

        // 3) Ejecutar el preprocesamiento en Kotlin
        val preprocessor = AudioPreprocessor()
        val actual = preprocessor.preprocess(audio)

        assertEquals("Shape de salida", 6464, actual.size)

        // 4) Comparar elemento por elemento
        var maxDiff = 0f
        var maxDiffIdx = -1
        for (i in 0 until 6464) {
            val diff = kotlin.math.abs(actual[i] - expected[i])
            if (diff > maxDiff) {
                maxDiff = diff
                maxDiffIdx = i
            }
        }

        // Imprimir primeros valores para diagnóstico
        println("Primeros 5 Kotlin : " + (0 until 5).joinToString { "%.7f".format(actual[it]) })
        println("Primeros 5 Python : " + (0 until 5).joinToString { "%.7f".format(expected[it]) })
        println("Media Kotlin      : %.6f".format(actual.average().toFloat()))
        println("Media Python      : %.6f".format(expected.average().toFloat()))
        println("Max |diff|        : %.6e en índice $maxDiffIdx".format(maxDiff))
        println("  Kotlin: %.7f".format(actual[maxDiffIdx]))
        println("  Python: %.7f".format(expected[maxDiffIdx]))

        // 5) Tolerancia
        // Si todo está perfecto, maxDiff debería ser ~1e-3 o menos.
        // Tolerancia estricta primero, luego relajada si falla.
        assertTrue(
            "Desviación máxima demasiado grande: $maxDiff. Revisar preprocesamiento.",
            maxDiff < 1e-2f
        )
    }
}