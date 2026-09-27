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
    val orderBlockZone: String = "None",
    val liquiditySweep: String = "None",
    val otcStrategyTrap: String = "None",
    val trend: String,
    val riskLevel: String,
    val advice: String,
    val userOutcome: String? = null, // "WIN", "LOSS", null
    val signalScore: Int = confidence,
    val setupGrade: String = "B",
    val setupRecommendation: String = "VALID SETUP",
    val marketStructure: String = "None",
    val liquidityStatus: String = "None",
    val reasonsJoined: String = "",
    val warningsJoined: String = "",
    val failureReason: String? = null
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
            orderBlockZone = orderBlockZone,
            liquiditySweep = liquiditySweep,
            otcPatternTrap = otcStrategyTrap,
            trend = trend,
            riskLevel = riskLevel,
            advice = advice,
            timestamp = timestamp,
            userOutcome = userOutcome,
            signalScore = if (signalScore > 0) signalScore else confidence,
            setupGrade = setupGrade,
            setupRecommendation = setupRecommendation,
            marketStructure = marketStructure,
            liquidityStatus = liquidityStatus,
            reasons = if (reasonsJoined.isBlank()) emptyList() else reasonsJoined.split("|||"),
            warnings = if (warningsJoined.isBlank()) emptyList() else warningsJoined.split("|||"),
            failureReason = failureReason
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
                orderBlockZone = p.orderBlockZone,
                liquiditySweep = p.liquiditySweep,
                otcStrategyTrap = p.otcPatternTrap,
                trend = p.trend,
                riskLevel = p.riskLevel,
                advice = p.advice,
                userOutcome = p.userOutcome,
                signalScore = p.signalScore,
                setupGrade = p.setupGrade,
                setupRecommendation = p.setupRecommendation,
                marketStructure = p.marketStructure,
                liquidityStatus = p.liquidityStatus,
                reasonsJoined = p.reasons.joinToString("|||"),
                warningsJoined = p.warnings.joinToString("|||"),
                failureReason = p.failureReason
            )
        }
    }
}
