package com.example.data.engine

import com.example.data.model.PredictionResult
import java.util.Locale

/**
 * Institutional Quantitative Signal Engine & Market Structure Validator.
 * Implements a 100-point transparent scoring model, contradiction penalty engine,
 * counter-trend filter, FVG/OB freshness validation, liquidity sweep confirmation,
 * duplicate filtering, and historical audit analytics.
 */
object QuantSignalEngine {

    enum class SetupGrade(val label: String, val minScore: Int, val maxScore: Int) {
        A_PLUS("A+", 90, 100),
        A("A", 80, 89),
        B("B", 70, 79),
        C("C", 60, 69),
        NO_TRADE("NO TRADE", 0, 59)
    }

    enum class SignalDecision(val displayLabel: String) {
        STRONG_CALL("STRONG CALL"),
        CALL("CALL"),
        WAIT_NO_TRADE("WAIT / NO TRADE"),
        PUT("PUT"),
        STRONG_PUT("STRONG PUT")
    }

    data class ScoringBreakdown(
        val marketStructurePoints: Int, // Max 20
        val srLocationPoints: Int,       // Max 15
        val liquiditySweepPoints: Int,   // Max 15
        val fvgConfirmationPoints: Int,  // Max 10
        val orderBlockPoints: Int,       // Max 10
        val candleConfirmationPoints: Int,// Max 10
        val trendAlignmentPoints: Int,   // Max 10
        val momentumVolumePoints: Int,   // Max 5
        val otcTrapPoints: Int,          // Max 5
        val contradictionPenalty: Int,   // Deductions
        val totalRawScore: Int,
        val finalScore: Int              // 0..100
    )

    data class QuantEvaluationResult(
        val decision: SignalDecision,
        val signalScore: Int,             // 0..100 Setup Quality Score
        val setupGrade: String,           // "A+", "A", "B", "C", "NO TRADE"
        val recommendation: String,       // "STRONG SETUP", "VALID SETUP", "WAIT / WEAK SETUP", "NO TRADE"
        val reasons: List<String>,        // Checkmarked confluences (✓)
        val warnings: List<String>,       // Warnings (⚠)
        val contradictions: List<String>, // Detected structural contradictions
        val scoringBreakdown: ScoringBreakdown
    )

    /**
     * Multi-Layer Signal Evaluation Pipeline.
     * Evaluates Market Structure, Trend, S/R Location, Liquidity, FVG, Order Block,
     * Candle Confirmation, Momentum, Contradictions, and OTC Traps.
     */
    fun evaluate(
        directionBias: String, // "UP", "DOWN", or "UNCERTAIN"
        marketStructure: String, // e.g. "BULLISH_HH_HL", "BEARISH_LH_LL", "CHoCH_BULLISH", "RANGING"
        trend: String,           // "Bullish", "Bearish", "Sideways"
        srZone: String,          // "Support", "Resistance", "Mid-Range", etc.
        liquiditySweep: String,  // "SSL Swept", "BSL Swept", "None"
        fvgDetected: Boolean,
        fvgFresh: Boolean = true,
        orderBlockZone: String,  // "Bullish Order Block (Demand)", "Bearish Order Block (Supply)", "None"
        obFresh: Boolean = true,
        candlePattern: String,
        isDisplacementCandle: Boolean = true,
        momentumStrong: Boolean = true,
        otcPatternTrap: String = "None"
    ): QuantEvaluationResult {
        val reasons = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val contradictions = mutableListOf<String>()

        val isCall = directionBias.equals("UP", ignoreCase = true)
        val isPut = directionBias.equals("DOWN", ignoreCase = true)

        if (!isCall && !isPut) {
            return QuantEvaluationResult(
                decision = SignalDecision.WAIT_NO_TRADE,
                signalScore = 50,
                setupGrade = "NO TRADE",
                recommendation = "NO TRADE (Consolidation)",
                reasons = listOf("Market in chop/range without directional edge"),
                warnings = listOf("Lack of directional momentum", "No structural displacement"),
                contradictions = listOf("Conflicting market signals"),
                scoringBreakdown = ScoringBreakdown(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 50, 50)
            )
        }

        // =========================================================================
        // 1. BASE SCORING MODEL (MAX 100 POINTS)
        // =========================================================================

        // Layer 1: Market Structure Alignment (Max 20)
        var structurePts = 0
        val isBullishStructure = marketStructure.contains("HH_HL", ignoreCase = true) ||
                marketStructure.contains("BULLISH", ignoreCase = true) ||
                marketStructure.contains("CHoCH_BULLISH", ignoreCase = true)
        val isBearishStructure = marketStructure.contains("LH_LL", ignoreCase = true) ||
                marketStructure.contains("BEARISH", ignoreCase = true) ||
                marketStructure.contains("CHoCH_BEARISH", ignoreCase = true)

        if (isCall) {
            if (isBullishStructure) {
                structurePts = 20
                reasons.add("Bullish Market Structure (HH+HL / Structural Shift)")
            } else if (marketStructure.contains("CHoCH", ignoreCase = true)) {
                structurePts = 18
                reasons.add("Bullish Change of Character (CHoCH Reversal)")
            } else if (marketStructure.contains("RANGING", ignoreCase = true)) {
                structurePts = 8
                warnings.add("Market structure is ranging/sideways")
            } else {
                structurePts = 0
                contradictions.add("CALL attempted in Bearish market structure (LH+LL)")
            }
        } else {
            if (isBearishStructure) {
                structurePts = 20
                reasons.add("Bearish Market Structure (LH+LL / Structural Shift)")
            } else if (marketStructure.contains("CHoCH", ignoreCase = true)) {
                structurePts = 18
                reasons.add("Bearish Change of Character (CHoCH Reversal)")
            } else if (marketStructure.contains("RANGING", ignoreCase = true)) {
                structurePts = 8
                warnings.add("Market structure is ranging/sideways")
            } else {
                structurePts = 0
                contradictions.add("PUT attempted in Bullish market structure (HH+HL)")
            }
        }

        // Layer 2: Support / Resistance Location (Max 15)
        var srPts = 0
        val isAtSupport = srZone.contains("Support", ignoreCase = true) || srZone.contains("Demand", ignoreCase = true)
        val isAtResistance = srZone.contains("Resistance", ignoreCase = true) || srZone.contains("Supply", ignoreCase = true)

        if (isCall) {
            if (isAtSupport) {
                srPts = 15
                reasons.add("Price sitting on Key Support / Institutional Demand")
            } else if (isAtResistance) {
                srPts = 0
                contradictions.add("CALL placed directly under Strong Resistance barrier")
            } else {
                srPts = 5
                warnings.add("Price is mid-range away from key Support")
            }
        } else {
            if (isAtResistance) {
                srPts = 15
                reasons.add("Price testing Key Resistance / Institutional Supply")
            } else if (isAtSupport) {
                srPts = 0
                contradictions.add("PUT placed directly sitting on Strong Support barrier")
            } else {
                srPts = 5
                warnings.add("Price is mid-range away from key Resistance")
            }
        }

        // Layer 3: Liquidity Event Confirmation (Max 15)
        var liquidityPts = 0
        val isSslSwept = liquiditySweep.contains("SSL", ignoreCase = true) || liquiditySweep.contains("SELL_SIDE", ignoreCase = true)
        val isBslSwept = liquiditySweep.contains("BSL", ignoreCase = true) || liquiditySweep.contains("BUY_SIDE", ignoreCase = true)
        val hasConfirmedSweep = (isCall && isSslSwept) || (isPut && isBslSwept)

        if (isCall) {
            if (isSslSwept) {
                liquidityPts = 15
                reasons.add("Sell-Side Liquidity Swept (SSL Grab below retail lows)")
            } else if (isBslSwept) {
                liquidityPts = 0
                contradictions.add("Buy-side liquidity already swept; reversal down imminent")
            } else {
                liquidityPts = 0
                warnings.add("No confirmed liquidity sweep before call")
            }
        } else {
            if (isBslSwept) {
                liquidityPts = 15
                reasons.add("Buy-Side Liquidity Swept (BSL Grab above retail highs)")
            } else if (isSslSwept) {
                liquidityPts = 0
                contradictions.add("Sell-side liquidity already swept; reversal up imminent")
            } else {
                liquidityPts = 0
                warnings.add("No confirmed liquidity sweep before put")
            }
        }

        // Layer 4: Fair Value Gap (FVG) Validation (Max 10)
        var fvgPts = 0
        if (fvgDetected && fvgFresh) {
            fvgPts = 10
            reasons.add("Fresh Fair Value Gap (FVG) unmitigated / imbalance filled")
        } else if (fvgDetected && !fvgFresh) {
            fvgPts = 3
            warnings.add("FVG has already been partially mitigated")
        }

        // Layer 5: Order Block Confirmation (Max 10)
        var obPts = 0
        val isBullishOb = orderBlockZone.contains("Bullish", ignoreCase = true) || orderBlockZone.contains("Demand", ignoreCase = true)
        val isBearishOb = orderBlockZone.contains("Bearish", ignoreCase = true) || orderBlockZone.contains("Supply", ignoreCase = true)

        if (isCall) {
            if (isBullishOb && obFresh) {
                obPts = 10
                reasons.add("Fresh Bullish Order Block reaction with institutional mitigation")
            } else if (isBearishOb) {
                contradictions.add("Opposing Bearish Order Block directly overhead")
            }
        } else {
            if (isBearishOb && obFresh) {
                obPts = 10
                reasons.add("Fresh Bearish Order Block reaction with institutional mitigation")
            } else if (isBullishOb) {
                contradictions.add("Opposing Bullish Order Block directly below")
            }
        }

        // Layer 6: Candle Confirmation (Max 10)
        // (Note: Pattern confirms setup, does NOT trigger on its own)
        var candlePts = 0
        if (candlePattern.isNotBlank() && !candlePattern.equals("None", ignoreCase = true)) {
            candlePts = if (isDisplacementCandle) 10 else 6
            reasons.add("Candle Confirmation: $candlePattern")
        } else {
            candlePts = 3
            warnings.add("Lack of strong individual candlestick trigger")
        }

        // Layer 7: Trend Alignment (Max 10)
        var trendPts = 0
        val isBullishTrend = trend.equals("Bullish", ignoreCase = true)
        val isBearishTrend = trend.equals("Bearish", ignoreCase = true)
        val isWithTrend = (isCall && isBullishTrend) || (isPut && isBearishTrend)
        val isCounterTrend = (isCall && isBearishTrend) || (isPut && isBullishTrend)

        if (isWithTrend) {
            trendPts = 10
            reasons.add("Trend Alignment: Macro & micro order flow aligned")
        } else if (isCounterTrend) {
            trendPts = 0
            warnings.add("Counter-Trend trade against prevailing momentum")
        } else {
            trendPts = 5
            reasons.add("Sideways market consolidation")
        }

        // Layer 8: Momentum & Volume Confirmation (Max 5)
        var momentumPts = 0
        if (momentumStrong && isDisplacementCandle) {
            momentumPts = 5
            reasons.add("Volume & Momentum displacement expansion")
        } else {
            momentumPts = 2
        }

        // Layer 9: OTC Fakeout / Trap Confirmation (Max 5)
        var otcTrapPts = 0
        if (otcPatternTrap.isNotBlank() && !otcPatternTrap.equals("None", ignoreCase = true)) {
            otcTrapPts = 5
            reasons.add("OTC Trap detected: $otcPatternTrap")
        }

        val baseScore = structurePts + srPts + liquidityPts + fvgPts + obPts + candlePts + trendPts + momentumPts + otcTrapPts

        // =========================================================================
        // 2. CONTRADICTION PENALTY ENGINE (Cases 1 - 5)
        // =========================================================================
        var penalty = 0

        // Case 1: Bearish Trend + CALL + No Liquidity Sweep + Weak Candle
        if (isCall && isBearishTrend && !hasConfirmedSweep) {
            penalty += 30
            contradictions.add("Counter-trend CALL without confirmed Sell-Side Liquidity sweep")
        }
        // Symmetrical Case 1 for PUT
        if (isPut && isBullishTrend && !hasConfirmedSweep) {
            penalty += 30
            contradictions.add("Counter-trend PUT without confirmed Buy-Side Liquidity sweep")
        }

        // Case 4: PUT signal + Sitting on strong Support + Bullish OB
        if (isPut && isAtSupport && isBullishOb) {
            penalty += 35
            contradictions.add("PUT directly into Major Support and Bullish Order Block")
        }

        // Case 5: CALL signal + Sitting under strong Resistance + Bearish OB
        if (isCall && isAtResistance && isBearishOb) {
            penalty += 35
            contradictions.add("CALL directly into Major Resistance and Bearish Order Block")
        }

        // Calling into an immediate opposing wall
        if (isCall && isAtResistance) {
            penalty += 25
            contradictions.add("Price blocked by strong overhead Resistance barrier")
        }
        if (isPut && isAtSupport) {
            penalty += 25
            contradictions.add("Price blocked by strong underlying Support floor")
        }

        // =========================================================================
        // 3. COUNTER-TREND FILTER (Strict Requirement Check)
        // For a CALL against bearish trend, require ALL:
        // 1. Strong Support
        // 2. Liquidity Sweep (SSL)
        // 3. Bullish displacement / rejection
        // 4. Bullish market structure shift / CHoCH
        // 5. FVG or Order Block confirmation
        // If not met -> FORCE NO TRADE!
        // =========================================================================
        var counterTrendGatingFailed = false
        if (isCall && isBearishTrend) {
            val satisfiesCounterTrend = isAtSupport && isSslSwept && isDisplacementCandle &&
                    (marketStructure.contains("CHoCH", ignoreCase = true) || isBullishStructure) &&
                    (fvgDetected || isBullishOb)
            if (!satisfiesCounterTrend) {
                counterTrendGatingFailed = true
                contradictions.add("Counter-trend filter failed: Missing structural reversal confirmations")
            }
        }
        if (isPut && isBullishTrend) {
            val satisfiesCounterTrend = isAtResistance && isBslSwept && isDisplacementCandle &&
                    (marketStructure.contains("CHoCH", ignoreCase = true) || isBearishStructure) &&
                    (fvgDetected || isBearishOb)
            if (!satisfiesCounterTrend) {
                counterTrendGatingFailed = true
                contradictions.add("Counter-trend filter failed: Missing structural reversal confirmations")
            }
        }

        // Calculate Final Calibrated Score (0..100)
        var finalScore = (baseScore - penalty).coerceIn(0, 100)

        // Force NO TRADE if major contradictions or counter-trend gating failed
        val forceNoTrade = counterTrendGatingFailed || contradictions.size >= 2 || (contradictions.isNotEmpty() && finalScore < 75)

        if (forceNoTrade && finalScore >= 60) {
            // Apply ceiling on contradictory setups
            finalScore = 58
        }

        // Map to Grade and Decision
        val grade: String
        val recommendation: String
        val decision: SignalDecision

        when {
            finalScore >= 90 && !forceNoTrade -> {
                grade = "A+"
                recommendation = "STRONG SETUP"
                decision = if (isCall) SignalDecision.STRONG_CALL else SignalDecision.STRONG_PUT
            }
            finalScore >= 80 && !forceNoTrade -> {
                grade = "A"
                recommendation = "STRONG SETUP"
                decision = if (isCall) SignalDecision.STRONG_CALL else SignalDecision.STRONG_PUT
            }
            finalScore >= 70 && !forceNoTrade -> {
                grade = "B"
                recommendation = "VALID SETUP"
                decision = if (isCall) SignalDecision.CALL else SignalDecision.PUT
            }
            finalScore >= 60 && !forceNoTrade -> {
                grade = "C"
                recommendation = "WAIT / WEAK SETUP"
                decision = SignalDecision.WAIT_NO_TRADE
            }
            else -> {
                grade = "NO TRADE"
                recommendation = "NO TRADE"
                decision = SignalDecision.WAIT_NO_TRADE
            }
        }

        val breakdown = ScoringBreakdown(
            marketStructurePoints = structurePts,
            srLocationPoints = srPts,
            liquiditySweepPoints = liquidityPts,
            fvgConfirmationPoints = fvgPts,
            orderBlockPoints = obPts,
            candleConfirmationPoints = candlePts,
            trendAlignmentPoints = trendPts,
            momentumVolumePoints = momentumPts,
            otcTrapPoints = otcTrapPts,
            contradictionPenalty = penalty,
            totalRawScore = baseScore,
            finalScore = finalScore
        )

        return QuantEvaluationResult(
            decision = decision,
            signalScore = finalScore,
            setupGrade = grade,
            recommendation = recommendation,
            reasons = reasons,
            warnings = warnings,
            contradictions = contradictions,
            scoringBreakdown = breakdown
        )
    }

    /**
     * False Signal Root Cause Analyzer (User requirement 22).
     * Diagnoses why a historical trade failed.
     */
    fun diagnoseFailure(
        prediction: String,
        trend: String,
        liquiditySweep: String,
        srZone: String,
        fvgDetected: Boolean,
        orderBlockZone: String,
        candlePattern: String,
        score: Int
    ): String {
        val isCall = prediction.equals("UP", ignoreCase = true)
        val isPut = prediction.equals("DOWN", ignoreCase = true)

        val isCounterTrend = (isCall && trend.equals("Bearish", ignoreCase = true)) ||
                (isPut && trend.equals("Bullish", ignoreCase = true))
        val noSweep = liquiditySweep.equals("None", ignoreCase = true) || liquiditySweep.isBlank()
        val opposingSr = (isCall && srZone.contains("Resistance", ignoreCase = true)) ||
                (isPut && srZone.contains("Support", ignoreCase = true))
        val opposingOb = (isCall && orderBlockZone.contains("Bearish", ignoreCase = true)) ||
                (isPut && orderBlockZone.contains("Bullish", ignoreCase = true))

        return when {
            opposingSr -> "Opposing barrier: Trade entered directly into strong ${if (isCall) "Resistance" else "Support"} level."
            opposingOb -> "Institutional supply: Price reversed off an opposing Order Block."
            isCounterTrend && noSweep -> "Counter-trend trap: Entered against prevailing order flow without prior liquidity sweep."
            noSweep -> "Liquidity void: No liquidity sweep occurred to fuel continuation; market fell into retail fakeout."
            !fvgDetected && orderBlockZone.equals("None", ignoreCase = true) -> "Weak institutional footprint: Missing FVG / Order Block mitigation confluence."
            score < 75 -> "Sub-optimal confluence score ($score/100): Setup lacked critical institutional validations."
            else -> "Market volatility / OTC algorithmic expansion beyond normal standard deviation."
        }
    }

    /**
     * Anti-Duplicate & Cooldown Fingerprint Filter (User requirement 17).
     * Prevents duplicate signals from the same market setup within 60 seconds.
     */
    fun isDuplicateSignal(
        lastScan: PredictionResult?,
        direction: String,
        structure: String,
        timestamp: Long,
        cooldownMs: Long = 55_000L
    ): Boolean {
        if (lastScan == null) return false
        val timeDiff = timestamp - lastScan.timestamp
        if (timeDiff < cooldownMs) {
            val sameDirection = lastScan.prediction.equals(direction, ignoreCase = true)
            val sameStructure = lastScan.marketStructure.equals(structure, ignoreCase = true)
            if (sameDirection && sameStructure) {
                return true
            }
        }
        return false
    }

    data class QuantAuditReport(
        val totalSignals: Int,
        val verifiedSignals: Int,
        val wins: Int,
        val losses: Int,
        val unverified: Int,
        val winRatePct: Float,
        val avgSignalScore: Float,
        val avgWinScore: Float,
        val avgLossScore: Float,
        val callWinRate: Float,
        val callCount: Int,
        val putWinRate: Float,
        val putCount: Int,
        val withTrendWinRate: Float,
        val counterTrendWinRate: Float,
        val sweepConfirmedWinRate: Float,
        val noSweepWinRate: Float,
        val gradeAWinRate: Float,
        val gradeBWinRate: Float,
        val gradeCWinRate: Float,
        val topFailureReasons: List<Pair<String, Int>>
    )

    /**
     * Historical Audit & Performance Engine (User requirement 18 & 21).
     */
    fun calculateHistoricalAudit(scans: List<PredictionResult>): QuantAuditReport {
        val total = scans.size
        val verifiedScans = scans.filter { it.userOutcome == "WIN" || it.userOutcome == "LOSS" }
        val verifiedCount = verifiedScans.size
        val wins = scans.count { it.userOutcome == "WIN" }
        val losses = scans.count { it.userOutcome == "LOSS" }
        val unverified = total - verifiedCount

        val winRate = if (verifiedCount > 0) (wins.toFloat() / verifiedCount) * 100f else 0f
        val avgScore = if (total > 0) scans.map { it.signalScore.toFloat() }.average().toFloat() else 0f

        val winScans = verifiedScans.filter { it.userOutcome == "WIN" }
        val lossScans = verifiedScans.filter { it.userOutcome == "LOSS" }

        val avgWinScore = if (winScans.isNotEmpty()) winScans.map { it.signalScore.toFloat() }.average().toFloat() else 0f
        val avgLossScore = if (lossScans.isNotEmpty()) lossScans.map { it.signalScore.toFloat() }.average().toFloat() else 0f

        // Direction stats
        val callVerified = verifiedScans.filter { it.isUp }
        val callWins = callVerified.count { it.userOutcome == "WIN" }
        val callWinRate = if (callVerified.isNotEmpty()) (callWins.toFloat() / callVerified.size) * 100f else 0f

        val putVerified = verifiedScans.filter { it.isDown }
        val putWins = putVerified.count { it.userOutcome == "WIN" }
        val putWinRate = if (putVerified.isNotEmpty()) (putWins.toFloat() / putVerified.size) * 100f else 0f

        // Trend stats
        val withTrendScans = verifiedScans.filter {
            (it.isUp && it.trend.equals("Bullish", ignoreCase = true)) ||
                    (it.isDown && it.trend.equals("Bearish", ignoreCase = true))
        }
        val withTrendWins = withTrendScans.count { it.userOutcome == "WIN" }
        val withTrendWinRate = if (withTrendScans.isNotEmpty()) (withTrendWins.toFloat() / withTrendScans.size) * 100f else 0f

        val counterTrendScans = verifiedScans.filter {
            (it.isUp && it.trend.equals("Bearish", ignoreCase = true)) ||
                    (it.isDown && it.trend.equals("Bullish", ignoreCase = true))
        }
        val counterTrendWins = counterTrendScans.count { it.userOutcome == "WIN" }
        val counterTrendWinRate = if (counterTrendScans.isNotEmpty()) (counterTrendWins.toFloat() / counterTrendScans.size) * 100f else 0f

        // Liquidity stats
        val sweepScans = verifiedScans.filter { it.hasLiquiditySweep }
        val sweepWins = sweepScans.count { it.userOutcome == "WIN" }
        val sweepWinRate = if (sweepScans.isNotEmpty()) (sweepWins.toFloat() / sweepScans.size) * 100f else 0f

        val noSweepScans = verifiedScans.filter { !it.hasLiquiditySweep }
        val noSweepWins = noSweepScans.count { it.userOutcome == "WIN" }
        val noSweepWinRate = if (noSweepScans.isNotEmpty()) (noSweepWins.toFloat() / noSweepScans.size) * 100f else 0f

        // Score tier stats
        val gradeAScans = verifiedScans.filter { it.signalScore >= 80 }
        val gradeAWins = gradeAScans.count { it.userOutcome == "WIN" }
        val gradeAWinRate = if (gradeAScans.isNotEmpty()) (gradeAWins.toFloat() / gradeAScans.size) * 100f else 0f

        val gradeBScans = verifiedScans.filter { it.signalScore in 70..79 }
        val gradeBWins = gradeBScans.count { it.userOutcome == "WIN" }
        val gradeBWinRate = if (gradeBScans.isNotEmpty()) (gradeBWins.toFloat() / gradeBScans.size) * 100f else 0f

        val gradeCScans = verifiedScans.filter { it.signalScore < 70 }
        val gradeCWins = gradeCScans.count { it.userOutcome == "WIN" }
        val gradeCWinRate = if (gradeCScans.isNotEmpty()) (gradeCWins.toFloat() / gradeCScans.size) * 100f else 0f

        // Failure reasons
        val failureCounts = mutableMapOf<String, Int>()
        for (loss in lossScans) {
            val diag = loss.failureReason ?: diagnoseFailure(
                prediction = loss.prediction,
                trend = loss.trend,
                liquiditySweep = loss.liquiditySweep,
                srZone = loss.srZone,
                fvgDetected = loss.fvgDetected,
                orderBlockZone = loss.orderBlockZone,
                candlePattern = loss.candlePatternFound,
                score = loss.signalScore
            )
            failureCounts[diag] = (failureCounts[diag] ?: 0) + 1
        }

        val topFailures = failureCounts.toList().sortedByDescending { it.second }.take(5)

        return QuantAuditReport(
            totalSignals = total,
            verifiedSignals = verifiedCount,
            wins = wins,
            losses = losses,
            unverified = unverified,
            winRatePct = winRate,
            avgSignalScore = avgScore,
            avgWinScore = avgWinScore,
            avgLossScore = avgLossScore,
            callWinRate = callWinRate,
            callCount = callVerified.size,
            putWinRate = putWinRate,
            putCount = putVerified.size,
            withTrendWinRate = withTrendWinRate,
            counterTrendWinRate = counterTrendWinRate,
            sweepConfirmedWinRate = sweepWinRate,
            noSweepWinRate = noSweepWinRate,
            gradeAWinRate = gradeAWinRate,
            gradeBWinRate = gradeBWinRate,
            gradeCWinRate = gradeCWinRate,
            topFailureReasons = topFailures
        )
    }

    /**
     * Seeds the historical benchmark audit sample dataset (12 trades: 6 WIN, 3 LOSS, 3 unverified)
     * as requested in the CSV audit report specification to enable immediate testing.
     */
    fun getHistoricalAuditBenchmark(): List<PredictionResult> {
        val now = System.currentTimeMillis()
        val m = 60_000L

        return listOf(
            // 1. WIN - A+ Setup: With-trend Call + SSL Sweep + Bullish OB + FVG
            PredictionResult(
                id = 101L,
                prediction = "UP",
                confidence = 88,
                signalScore = 88,
                setupGrade = "A",
                setupRecommendation = "STRONG SETUP",
                primarySignal = "Bullish Order Block Bounce with Sell-Side Liquidity Grab",
                confirmations = listOf("SSL Swept below key swing low", "Bullish Order Block defended", "Fresh FVG unfilled"),
                candlePatternFound = "Bullish Engulfing",
                srZone = "Key Support (1.08200)",
                fvgDetected = true,
                orderBlockZone = "Bullish Order Block (Demand)",
                liquiditySweep = "SSL Swept",
                marketStructure = "BULLISH_HH_HL",
                trend = "Bullish",
                riskLevel = "LOW",
                advice = "ENTER NOW (CALL / UP)",
                timestamp = now - (11 * m),
                userOutcome = "WIN",
                reasons = listOf("✓ Bullish market structure (HH+HL)", "✓ Sell-side liquidity swept", "✓ Fresh Bullish FVG", "✓ Strong Support bounce"),
                warnings = emptyList()
            ),
            // 2. WIN - A Setup: With-trend Put + BSL Sweep + Bearish OB
            PredictionResult(
                id = 102L,
                prediction = "DOWN",
                confidence = 88,
                signalScore = 88,
                setupGrade = "A",
                setupRecommendation = "STRONG SETUP",
                primarySignal = "Bearish Rejection at Supply Zone after BSL Grab",
                confirmations = listOf("BSL swept above session high", "Bearish displacement"),
                candlePatternFound = "Shooting Star / Pin Bar",
                srZone = "Major Resistance (1.08650)",
                fvgDetected = true,
                orderBlockZone = "Bearish Order Block (Supply)",
                liquiditySweep = "BSL Swept",
                marketStructure = "BEARISH_LH_LL",
                trend = "Bearish",
                riskLevel = "LOW",
                advice = "ENTER NOW (PUT / DOWN)",
                timestamp = now - (10 * m),
                userOutcome = "WIN",
                reasons = listOf("✓ Bearish market structure (LH+LL)", "✓ Buy-side liquidity swept", "✓ Fresh Bearish OB reaction"),
                warnings = emptyList()
            ),
            // 3. LOSS - Weak 85 score setup: Counter-trend CALL without liquidity sweep
            PredictionResult(
                id = 103L,
                prediction = "UP",
                confidence = 85,
                signalScore = 65,
                setupGrade = "C",
                setupRecommendation = "WAIT / WEAK SETUP",
                primarySignal = "Support Reaction (Counter-Trend)",
                confirmations = listOf("Minor support bounce"),
                candlePatternFound = "Hammer",
                srZone = "Weak Support",
                fvgDetected = false,
                orderBlockZone = "None",
                liquiditySweep = "None",
                marketStructure = "BEARISH_LH_LL",
                trend = "Bearish",
                riskLevel = "HIGH",
                advice = "WAIT FOR CONFIRMATION",
                timestamp = now - (9 * m),
                userOutcome = "LOSS",
                failureReason = "Counter-trend trap: Entered against prevailing order flow without prior liquidity sweep.",
                reasons = listOf("Minor bounce at support"),
                warnings = listOf("⚠ Counter-trend setup", "⚠ No liquidity sweep")
            ),
            // 4. WIN - With-trend Put: 88 score
            PredictionResult(
                id = 104L,
                prediction = "DOWN",
                confidence = 88,
                signalScore = 88,
                setupGrade = "A",
                setupRecommendation = "STRONG SETUP",
                primarySignal = "Bearish CHoCH Structural Breakout",
                confirmations = listOf("CHoCH confirmed on 1M chart", "Bearish FVG expansion"),
                candlePatternFound = "Bearish Marubozu",
                srZone = "Resistance (1.08500)",
                fvgDetected = true,
                orderBlockZone = "Bearish Order Block (Supply)",
                liquiditySweep = "BSL Swept",
                marketStructure = "BEARISH_LH_LL",
                trend = "Bearish",
                riskLevel = "LOW",
                advice = "ENTER NOW (PUT / DOWN)",
                timestamp = now - (8 * m),
                userOutcome = "WIN",
                reasons = listOf("✓ Bearish structural shift", "✓ High volume displacement"),
                warnings = emptyList()
            ),
            // 5. LOSS - Another 85 setup: CALL into immediate Resistance barrier
            PredictionResult(
                id = 105L,
                prediction = "UP",
                confidence = 85,
                signalScore = 58,
                setupGrade = "NO TRADE",
                setupRecommendation = "NO TRADE",
                primarySignal = "Breakout Attempt",
                confirmations = listOf("Green candle body"),
                candlePatternFound = "Bullish Continuation",
                srZone = "Strong Resistance (1.08900)",
                fvgDetected = false,
                orderBlockZone = "None",
                liquiditySweep = "None",
                marketStructure = "RANGING",
                trend = "Sideways",
                riskLevel = "HIGH",
                advice = "NO TRADE (Opposing Wall)",
                timestamp = now - (7 * m),
                userOutcome = "LOSS",
                failureReason = "Opposing barrier: Trade entered directly into strong Resistance level.",
                reasons = emptyList(),
                warnings = listOf("⚠ Overhead resistance wall", "⚠ No liquidity sweep")
            ),
            // 6. WIN - Valid B Setup: 76 score Call
            PredictionResult(
                id = 106L,
                prediction = "UP",
                confidence = 76,
                signalScore = 76,
                setupGrade = "B",
                setupRecommendation = "VALID SETUP",
                primarySignal = "Demand Zone Mitigation Bouncing Upward",
                confirmations = listOf("Support defended", "Bullish rejection wick"),
                candlePatternFound = "Bullish Pin Bar",
                srZone = "Support Zone (1.08150)",
                fvgDetected = true,
                orderBlockZone = "Bullish Order Block (Demand)",
                liquiditySweep = "SSL Swept",
                marketStructure = "BULLISH_HH_HL",
                trend = "Bullish",
                riskLevel = "MEDIUM",
                advice = "ENTER NOW (CALL / UP)",
                timestamp = now - (6 * m),
                userOutcome = "WIN",
                reasons = listOf("✓ Price bouncing from Demand", "✓ Rejection wick"),
                warnings = listOf("⚠ Moderate volume")
            ),
            // 7. WIN - A Setup: 88 score Put
            PredictionResult(
                id = 107L,
                prediction = "DOWN",
                confidence = 88,
                signalScore = 88,
                setupGrade = "A",
                setupRecommendation = "STRONG SETUP",
                primarySignal = "OTC Fakeout Liquidity Trap Reversal Down",
                confirmations = listOf("Fake breakout trapped retail buyers", "Rapid rejection"),
                candlePatternFound = "Shooting Star",
                srZone = "Resistance (1.08720)",
                fvgDetected = true,
                orderBlockZone = "Bearish Order Block (Supply)",
                liquiditySweep = "BSL Swept",
                otcPatternTrap = "OTC Fakeout Trap",
                marketStructure = "BEARISH_LH_LL",
                trend = "Bearish",
                riskLevel = "LOW",
                advice = "ENTER NOW (PUT / DOWN)",
                timestamp = now - (5 * m),
                userOutcome = "WIN",
                reasons = listOf("✓ OTC Fakeout Trap confirmed", "✓ BSL Swept"),
                warnings = emptyList()
            ),
            // 8. WIN - A Setup: 88 score Call
            PredictionResult(
                id = 108L,
                prediction = "UP",
                confidence = 88,
                signalScore = 88,
                setupGrade = "A",
                setupRecommendation = "STRONG SETUP",
                primarySignal = "Institutional Order Flow Continuation Call",
                confirmations = listOf("Higher Highs confirmed", "Buyers expanding range"),
                candlePatternFound = "Bullish Engulfing",
                srZone = "Support (1.08300)",
                fvgDetected = true,
                orderBlockZone = "Bullish Order Block (Demand)",
                liquiditySweep = "SSL Swept",
                marketStructure = "BULLISH_HH_HL",
                trend = "Bullish",
                riskLevel = "LOW",
                advice = "ENTER NOW (CALL / UP)",
                timestamp = now - (4 * m),
                userOutcome = "WIN",
                reasons = listOf("✓ Strong Bullish displacement", "✓ Higher Low held"),
                warnings = emptyList()
            ),
            // 9. LOSS - Third Loss in audit: Late Put into strong support
            PredictionResult(
                id = 109L,
                prediction = "DOWN",
                confidence = 80,
                signalScore = 62,
                setupGrade = "C",
                setupRecommendation = "WAIT / WEAK SETUP",
                primarySignal = "Breakdown Attempt at Major Floor",
                confirmations = listOf("Bearish candle"),
                candlePatternFound = "Bearish Candle",
                srZone = "Major Daily Support (1.08000)",
                fvgDetected = false,
                orderBlockZone = "None",
                liquiditySweep = "None",
                marketStructure = "RANGING",
                trend = "Bearish",
                riskLevel = "HIGH",
                advice = "WAIT FOR BOUNCE",
                timestamp = now - (3 * m),
                userOutcome = "LOSS",
                failureReason = "Opposing barrier: Trade entered directly into strong Support level.",
                reasons = listOf("Bearish candle"),
                warnings = listOf("⚠ Major support floor ahead", "⚠ No sweep")
            ),
            // 10. UNVERIFIED - 88 score
            PredictionResult(
                id = 110L,
                prediction = "UP",
                confidence = 88,
                signalScore = 88,
                setupGrade = "A",
                setupRecommendation = "STRONG SETUP",
                primarySignal = "SSL Sweep Reversal from Institutional Order Block",
                confirmations = listOf("SSL taken", "Fresh FVG gap filled"),
                candlePatternFound = "Hammer",
                srZone = "Support (1.08250)",
                fvgDetected = true,
                orderBlockZone = "Bullish Order Block (Demand)",
                liquiditySweep = "SSL Swept",
                marketStructure = "BULLISH_HH_HL",
                trend = "Bullish",
                riskLevel = "LOW",
                advice = "ENTER NOW (CALL / UP)",
                timestamp = now - (2 * m),
                userOutcome = null
            ),
            // 11. UNVERIFIED - 75 score
            PredictionResult(
                id = 111L,
                prediction = "DOWN",
                confidence = 75,
                signalScore = 75,
                setupGrade = "B",
                setupRecommendation = "VALID SETUP",
                primarySignal = "Supply Zone Rejection Pullback",
                confirmations = listOf("Resistance held"),
                candlePatternFound = "Doji Reversal",
                srZone = "Resistance (1.08600)",
                fvgDetected = true,
                orderBlockZone = "Bearish Order Block (Supply)",
                liquiditySweep = "None",
                marketStructure = "BEARISH_LH_LL",
                trend = "Bearish",
                riskLevel = "MEDIUM",
                advice = "ENTER NOW (PUT / DOWN)",
                timestamp = now - (1 * m),
                userOutcome = null
            ),
            // 12. UNVERIFIED - 88 score
            PredictionResult(
                id = 112L,
                prediction = "UP",
                confidence = 88,
                signalScore = 88,
                setupGrade = "A",
                setupRecommendation = "STRONG SETUP",
                primarySignal = "Bullish Market Structure Shift (MSS) Trigger",
                confirmations = listOf("MSS confirmed", "Demand zone active"),
                candlePatternFound = "Bullish Marubozu",
                srZone = "Support (1.08380)",
                fvgDetected = true,
                orderBlockZone = "Bullish Order Block (Demand)",
                liquiditySweep = "SSL Swept",
                marketStructure = "BULLISH_HH_HL",
                trend = "Bullish",
                riskLevel = "LOW",
                advice = "ENTER NOW (CALL / UP)",
                timestamp = now,
                userOutcome = null
            )
        )
    }
}
