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
    assertTrue(csv.contains("Trade_ID,Timestamp_Millis,Date_Time,Signal,Confidence_Percent"))
    assertTrue(csv.contains("101"))
    assertTrue(csv.contains("UP,88%,WIN"))
    assertTrue(csv.contains("Bullish Order Block (Demand)"))
    assertTrue(csv.contains("102"))
    assertTrue(csv.contains("DOWN,79%,LOSS"))
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

