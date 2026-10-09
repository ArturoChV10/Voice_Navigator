package com.example.voicenavigator.model

/**
 * Los 10 comandos que el modelo debe reconocer.
 * El orden debe coincidir con el orden de las clases en el modelo ONNX.
 */
enum class VoiceCommand(val label: String) {
    YES("yes"),
    NO("no"),
    UP("up"),
    DOWN("down"),
    LEFT("left"),
    RIGHT("right"),
    ON("on"),
    OFF("off"),
    STOP("stop"),
    GO("go");

    companion object {
        /**
         * Convierte el índice de clase devuelto por el modelo en un comando.
         * IMPORTANTE: Este orden debe coincidir con el orden de clases usado al entrenar.
         */
        fun fromIndex(index: Int): VoiceCommand? = entries.getOrNull(index)
    }
}