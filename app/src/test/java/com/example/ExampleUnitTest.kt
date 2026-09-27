package com.example

import com.example.data.model.PredictionResult
import com.example.utils.TradeExportHelper
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun tradeExportHelper_csvGeneration_containsHeadersAndRows() {
    val sampleScans = listOf(
      PredictionResult(
        id = 101L,
        prediction = "UP",
        confidence = 88,
        primarySignal = "Bullish Order Block Reversal",
        trend = "Bullish",
        riskLevel = "LOW",
        advice = "Enter now",
        candlePatternFound = "Bullish Engulfing",
        srZone = "S&R Support",
        fvgDetected = true,
        orderBlockZone = "Bullish Order Block (Demand)",
        liquiditySweep = "SSL Swept",
        otcPatternTrap = "None",
        confirmations = listOf("RSI Oversold", "Volume Spike"),
        userOutcome = "WIN"
      ),
      PredictionResult(
        id = 102L,
        prediction = "DOWN",
        confidence = 79,
        primarySignal = "Bearish Rejection at Resistance",
        trend = "Bearish",
        riskLevel = "MEDIUM",
        advice = "Enter now",
        candlePatternFound = "Shooting Star",
        srZone = "Major Resistance",
        fvgDetected = false,
        orderBlockZone = "Bearish Order Block",
        liquiditySweep = "BSL Swept",
        otcPatternTrap = "OTC Fakeout Sweep",
        confirmations = listOf("MACD Bearish Cross"),
        userOutcome = "LOSS"
      )
    )

    val csv = TradeExportHelper.generateCsv(sampleScans)
    assertTrue(csv.contains("Trade_ID,Timestamp_Millis,Date_Time,Signal,Signal_Score"))
    assertTrue(csv.contains("101"))
    assertTrue(csv.contains("UP,88/100"))
    assertTrue(csv.contains("Bullish Order Block (Demand)"))
    assertTrue(csv.contains("102"))
    assertTrue(csv.contains("DOWN,79/100"))
  }

  @Test
  fun quantSignalEngine_scoringAndContradictions() {
    // 1. High confluence with-trend setup
    val bullishEval = com.example.data.engine.QuantSignalEngine.evaluate(
      directionBias = "UP",
      marketStructure = "BULLISH_HH_HL",
      trend = "Bullish",
      srZone = "Key Support (1.08200)",
      liquiditySweep = "SSL Swept",
      fvgDetected = true,
      fvgFresh = true,
      orderBlockZone = "Bullish Order Block (Demand)",
      obFresh = true,
      candlePattern = "Bullish Engulfing",
      isDisplacementCandle = true,
      momentumStrong = true,
      otcPatternTrap = "None"
    )
    assertTrue(bullishEval.signalScore >= 80)
    assertTrue(bullishEval.setupGrade == "A" || bullishEval.setupGrade == "A+")
    assertTrue(bullishEval.decision == com.example.data.engine.QuantSignalEngine.SignalDecision.STRONG_CALL || bullishEval.decision == com.example.data.engine.QuantSignalEngine.SignalDecision.CALL)

    // 2. Severe contradiction (CALL placed directly under resistance with no sweep)
    val contradictionEval = com.example.data.engine.QuantSignalEngine.evaluate(
      directionBias = "UP",
      marketStructure = "BEARISH_LH_LL",
      trend = "Bearish",
      srZone = "Strong Resistance (1.08900)",
      liquiditySweep = "None",
      fvgDetected = false,
      fvgFresh = false,
      orderBlockZone = "None",
      obFresh = false,
      candlePattern = "None",
      isDisplacementCandle = false,
      momentumStrong = false,
      otcPatternTrap = "None"
    )
    assertTrue(contradictionEval.signalScore < 60)
    assertEquals("NO TRADE", contradictionEval.setupGrade)
    assertEquals(com.example.data.engine.QuantSignalEngine.SignalDecision.WAIT_NO_TRADE, contradictionEval.decision)
    assertTrue(contradictionEval.contradictions.isNotEmpty())
  }

  @Test
  fun quantSignalEngine_historicalBenchmarkAnalytics() {
    val benchmark = com.example.data.engine.QuantSignalEngine.getHistoricalAuditBenchmark()
    assertEquals(12, benchmark.size)

    val report = com.example.data.engine.QuantSignalEngine.calculateHistoricalAudit(benchmark)
    assertEquals(12, report.totalSignals)
    assertEquals(9, report.verifiedSignals)
    assertEquals(6, report.wins)
    assertEquals(3, report.losses)
    assertEquals(3, report.unverified)
    assertEquals(66.67f, report.winRatePct, 0.5f)
    assertTrue(report.gradeAWinRate > report.gradeCWinRate)
  }

  @Test
  fun tradeExportHelper_txtReportGeneration_containsSummaryAndTrades() {
    val sampleScans = listOf(
      PredictionResult(
        id = 201L,
        prediction = "UP",
        confidence = 92,
        primarySignal = "Strong Momentum Breakout",
        trend = "Bullish",
        riskLevel = "LOW",
        advice = "Enter now",
        userOutcome = "WIN"
      )
    )

    val report = TradeExportHelper.generateTxtReport(sampleScans)
    assertTrue(report.contains("QUOTEX AI PRO SCANNER — COMPREHENSIVE TRADE AUDIT LEDGER"))
    assertTrue(report.contains("Total Trades     : 1"))
    assertTrue(report.contains("TRADE #001 | ID: 201"))
    assertTrue(report.contains("Prediction Signal : UP"))
    assertTrue(report.contains("Verified Outcome  : WIN"))
    assertTrue(report.contains("AI Confidence     : 92%"))
  }
}

