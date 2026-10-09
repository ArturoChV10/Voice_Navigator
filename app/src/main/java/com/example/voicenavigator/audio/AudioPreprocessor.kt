package com.example.voicenavigator.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sin

/**
 * Replica exactamente el preprocesamiento de torchaudio usado al entrenar:
 *
 *   MelSpectrogram(sample_rate=16000, n_fft=400, win_length=400, hop_length=160,
 *                  n_mels=64, f_min=0, f_max=8000, power=2.0,
 *                  center=True, pad_mode='reflect', mel_scale='htk', norm=None)
 *   -> amplitude_to_DB(multiplier=10, amin=1e-10, db_multiplier=0)
 *   -> (x - mean) / std
 *
 * Entrada: PCM 16-bit mono a 16 kHz, exactamente 16000 muestras (1 segundo).
 * Salida : FloatArray de tamaño [1, 1, 64, 101] (channel, mel, frames)
 */
class AudioPreprocessor {

    companion object {
        const val SAMPLE_RATE = 16_000
        const val CLIP_SAMPLES = 16_000
        const val N_FFT = 400
        const val HOP_LENGTH = 160
        const val N_MELS = 64
        const val F_MIN = 0f
        const val F_MAX = 8_000f
        const val N_FRAMES = 101            // 1 + 16000/160
        const val N_FREQS = N_FFT / 2 + 1   // 201
        const val MEAN = -33.879398f
        const val STD = 23.381967f
        const val AMIN = 1e-10f
    }

    // Ventana Hann periódica: w[n] = 0.5 * (1 - cos(2*pi*n/N))
    private val hannWindow: FloatArray = FloatArray(N_FFT) { n ->
        (0.5 * (1.0 - cos(2.0 * PI * n / N_FFT))).toFloat()
    }

    // Tablas precalculadas de cos/sin para la DFT (201 x 400)
    private val cosTable: Array<FloatArray> = Array(N_FREQS) { k ->
        FloatArray(N_FFT) { n -> cos(2.0 * PI * k * n / N_FFT).toFloat() }
    }
    private val sinTable: Array<FloatArray> = Array(N_FREQS) { k ->
        FloatArray(N_FFT) { n -> sin(2.0 * PI * k * n / N_FFT).toFloat() }
    }

    // Banco de filtros mel (64 x 201), escala HTK
    private val melFilterbank: Array<FloatArray> = buildMelFilterbank()

    /**
     * @param audio PCM 16-bit mono. Puede ser más corto o más largo que 16000 muestras.
     * @return FloatArray con shape (1, 1, 64, 101)
     */
    fun preprocess(audio: ShortArray): FloatArray {
        // 1) Normalizar a [-1, 1] y ajustar longitud a 16000
        val fixed = FloatArray(CLIP_SAMPLES)
        val n = minOf(audio.size, CLIP_SAMPLES)
        for (i in 0 until n) {
            fixed[i] = audio[i] / 32768f
        }
        // Si audio.size < CLIP_SAMPLES, el resto queda en 0 (padding al final)

        // 2) Padding reflectivo (sin incluir el borde) de n_fft/2 a cada lado
        val pad = N_FFT / 2
        val padded = reflectPad(fixed, pad)

        // 3) STFT: para cada frame, aplicar ventana y calcular DFT
        val stftPower = Array(N_FREQS) { FloatArray(N_FRAMES) }
        val frame = FloatArray(N_FFT)
        for (t in 0 until N_FRAMES) {
            val offset = t * HOP_LENGTH
            for (i in 0 until N_FFT) {
                frame[i] = padded[offset + i] * hannWindow[i]
            }
            for (k in 0 until N_FREQS) {
                val cosRow = cosTable[k]
                val sinRow = sinTable[k]
                var re = 0f
                var im = 0f
                for (i in 0 until N_FFT) {
                    val v = frame[i]
                    re += v * cosRow[i]
                    im -= v * sinRow[i]
                }
                stftPower[k][t] = re * re + im * im
            }
        }

        // 4) Mel filterbank -> (64 x 101)
        val melSpec = Array(N_MELS) { FloatArray(N_FRAMES) }
        for (m in 0 until N_MELS) {
            val filterRow = melFilterbank[m]
            for (t in 0 until N_FRAMES) {
                var acc = 0f
                for (k in 0 until N_FREQS) {
                    acc += filterRow[k] * stftPower[k][t]
                }
                melSpec[m][t] = acc
            }
        }

        // 5) dB + normalización -> shape (1, 1, 64, 101) aplanado
        val out = FloatArray(1 * 1 * N_MELS * N_FRAMES)
        var idx = 0
        for (m in 0 until N_MELS) {
            for (t in 0 until N_FRAMES) {
                val v = melSpec[m][t]
                val db = 10f * log10(max(v, AMIN))
                out[idx++] = (db - MEAN) / STD
            }
        }
        return out
    }

    /** Padding reflectivo idéntico a torch.nn.functional.pad(mode='reflect'). */
    private fun reflectPad(x: FloatArray, pad: Int): FloatArray {
        val n = x.size
        val out = FloatArray(n + 2 * pad)
        // Lado izquierdo: [x[pad], x[pad-1], ..., x[1]]
        for (i in 0 until pad) {
            out[i] = x[pad - i]
        }
        // Original
        for (i in 0 until n) {
            out[pad + i] = x[i]
        }
        // Lado derecho: [x[n-2], x[n-3], ..., x[n-pad-1]]
        for (i in 0 until pad) {
            out[pad + n + i] = x[n - 2 - i]
        }
        return out
    }

    private fun buildMelFilterbank(): Array<FloatArray> {
        // Escala HTK: mel(f) = 2595 * log10(1 + f/700)
        fun hzToMel(f: Float): Float = (2595.0 * log10(1.0 + f / 700.0)).toFloat()
        fun melToHz(m: Float): Float = (700.0 * (10.0.pow(m / 2595.0) - 1.0)).toFloat()

        val melMin = hzToMel(F_MIN)
        val melMax = hzToMel(F_MAX)

        // nMels + 2 puntos equiespaciados en escala mel
        val melPoints = FloatArray(N_MELS + 2) { i ->
            melMin + i * (melMax - melMin) / (N_MELS + 1)
        }
        // A Hz
        val hzPoints = FloatArray(N_MELS + 2) { melToHz(melPoints[it]) }
        // A bins de FFT (índices fraccionales)
        val binPoints = FloatArray(N_MELS + 2) { hzPoints[it] * N_FFT / SAMPLE_RATE }

        val bank = Array(N_MELS) { FloatArray(N_FREQS) }
        for (m in 0 until N_MELS) {
            val left = binPoints[m]
            val center = binPoints[m + 1]
            val right = binPoints[m + 2]
            for (k in 0 until N_FREQS) {
                val kf = k.toFloat()
                val v = when {
                    kf < left || kf > right -> 0f
                    kf <= center -> (kf - left) / (center - left)
                    else -> (right - kf) / (right - center)
                }
                bank[m][k] = v
            }
        }
        return bank
    }

    private fun Double.pow(x: Double): Double = Math.pow(this, x)
}