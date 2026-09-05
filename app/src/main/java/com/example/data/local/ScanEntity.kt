package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.PredictionResult

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val prediction: String,
    val confidence: Int,
    val primarySignal: String,
    val confirmationsJoined: String,
    val candlePattern: String,
    val srZone: String,
    val fvgDetected: Boolean,
    val trend: String,
    val riskLevel: String,
    val advice: String,
    val userOutcome: String? = null // "WIN", "LOSS", null
) {
    fun toPredictionResult(): PredictionResult {
        return PredictionResult(
            id = id,
            prediction = prediction,
            confidence = confidence,
            primarySignal = primarySignal,
            confirmations = if (confirmationsJoined.isBlank()) emptyList() else confirmationsJoined.split("|||"),
            candlePatternFound = candlePattern,
            srZone = srZone,
            fvgDetected = fvgDetected,
            trend = trend,
            riskLevel = riskLevel,
            advice = advice,
            timestamp = timestamp,
            userOutcome = userOutcome
        )
    }

    companion object {
        fun fromPredictionResult(p: PredictionResult): ScanEntity {
            return ScanEntity(
                id = p.id,
                timestamp = p.timestamp,
                prediction = p.prediction,
                confidence = p.confidence,
                primarySignal = p.primarySignal,
                confirmationsJoined = p.confirmations.joinToString("|||"),
                candlePattern = p.candlePatternFound,
                srZone = p.srZone,
                fvgDetected = p.fvgDetected,
                trend = p.trend,
                riskLevel = p.riskLevel,
                advice = p.advice,
                userOutcome = p.userOutcome
            )
        }
    }
}
