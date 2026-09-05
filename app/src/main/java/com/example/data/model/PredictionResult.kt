package com.example.data.model

data class PredictionResult(
    val id: Long = 0L,
    val prediction: String, // "UP", "DOWN", "UNCERTAIN"
    val confidence: Int, // 0 - 100
    val primarySignal: String,
    val confirmations: List<String> = emptyList(),
    val candlePatternFound: String = "None",
    val srZone: String = "None",
    val fvgDetected: Boolean = false,
    val trend: String = "Sideways",
    val riskLevel: String = "MEDIUM", // "LOW", "MEDIUM", "HIGH"
    val advice: String = "Wait", // "Enter now", "Wait", "Avoid"
    val timestamp: Long = System.currentTimeMillis(),
    val isSample: Boolean = false,
    val userOutcome: String? = null // "WIN", "LOSS", null
) {
    val isUp: Boolean get() = prediction.equals("UP", ignoreCase = true)
    val isDown: Boolean get() = prediction.equals("DOWN", ignoreCase = true)
    val isUncertain: Boolean get() = !isUp && !isDown
}

data class ScanSettings(
    val apiKey: String = "",
    val confidenceThreshold: Int = 70, // 50 to 95
    val scanDelayMs: Long = 500L, // 300 to 2000
    val analysisMode: String = "fast", // "fast" or "deep"
    val preferredModel: String = "pixtral-12b-2409"
)
