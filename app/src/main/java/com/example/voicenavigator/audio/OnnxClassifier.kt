package com.example.voicenavigator.audio

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import java.nio.FloatBuffer

/**
 * Carga `model.ort` desde assets y expone una función de clasificación.
 *
 * Contrato verificado en Python:
 *   input : "input"  shape [batch, 1, 64, 101]  float32
 *   output: "logits" shape [batch, 10]           float32
 *
 * IMPORTANTE: el orden de las 10 clases coincide con `labels.txt`:
 *   yes, no, up, down, left, right, on, off, stop, go
 */
class OnnxClassifier(private val context: Context) {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    init {
        val options = OrtSession.SessionOptions().apply {
            // Opcional: usar NNAPI para aceleración en dispositivos compatibles
            // setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
        }
        val modelBytes = context.assets.open("model.ort").use { it.readBytes() }
        session = env.createSession(modelBytes, options)
    }

    /**
     * @param input  FloatArray de tamaño 6464 en orden (1, 1, 64, 101)
     * @return par (índice de clase, scores completos de 10 clases)
     */
    fun classify(input: FloatArray): Pair<Int, FloatArray> {
        require(input.size == 64 * 101) { "Input must be 6464 elements, got ${input.size}" }

        val shape = longArrayOf(1, 1, 64, 101)
        val tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(input), shape)
        tensor.use { t ->
            session.run(mapOf("input" to t)).use { result ->
                @Suppress("UNCHECKED_CAST")
                val output = result.get(0) as OnnxTensor
                output.use { o ->
                    val fb = o.floatBuffer
                    val scores = FloatArray(10)
                    fb.get(scores)
                    val argmax = scores.indices.maxByOrNull { scores[it] } ?: 0
                    return argmax to scores
                }
            }
        }
    }

    /**
     * Aplica softmax sobre los logits. Útil si quieres mostrar confianza.
     */
    fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.max()
        val exps = FloatArray(logits.size) { kotlin.math.exp(logits[it] - maxLogit) }
        val sum = exps.sum()
        return FloatArray(logits.size) { exps[it] / sum }
    }

    fun close() {
        session.close()
        env.close()
    }
}