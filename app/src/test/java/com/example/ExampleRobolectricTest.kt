package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.api.MistralApiClient
import com.example.data.model.PredictionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Quotex AI Pro Scanner", appName)
  }

  @Test
  fun `verify otc strategy and trap parsing`() {
    val mockClient = MistralApiClient()
    val rawJson = """
      {
        "is_chart_detected": true,
        "prediction": "DOWN",
        "confidence": 88,
        "otc_strategy": "OTC Fakeout Wick Sweep Trap",
        "primary_signal": "Resistance Fakeout with Upper Wick (PUT)",
        "trend": "Bearish",
        "confirmations": [
          "Upper wick rejection > 40%",
          "Liquidity sweep above resistance"
        ],
        "risk_level": "LOW",
        "advice": "ENTER NOW (PUT / DOWN)"
      }
    """.trimIndent()

    val wrappedResponse = org.json.JSONObject().apply {
      put("choices", org.json.JSONArray().apply {
        put(org.json.JSONObject().apply {
          put("message", org.json.JSONObject().apply {
            put("content", rawJson)
          })
        })
      })
    }.toString()

    val result = mockClient.parsePredictionResponse(wrappedResponse, 70)
    assertEquals("DOWN", result.prediction)
    assertEquals(88, result.confidence)
    assertEquals("OTC Fakeout Wick Sweep Trap", result.otcPatternTrap)
    assertTrue(result.hasOtcTrap)
  }

  @Test
  fun `verify bullish up signal parsing`() {
    val mockClient = MistralApiClient()
    val rawJson = """
      {
        "is_chart_detected": true,
        "prediction": "UP",
        "confidence": 91,
        "otc_strategy": "OTC Exhaustion Candle Trap",
        "primary_signal": "Bullish Reversal from Giant Red Exhaustion (CALL)",
        "trend": "Bullish",
        "confirmations": [
          "Giant red candle exhausted into key demand level",
          "Clean lower wick rejection showing institutional absorption"
        ],
        "risk_level": "LOW",
        "advice": "ENTER NOW (CALL / UP)"
      }
    """.trimIndent()

    val wrappedResponse = org.json.JSONObject().apply {
      put("choices", org.json.JSONArray().apply {
        put(org.json.JSONObject().apply {
          put("message", org.json.JSONObject().apply {
            put("content", rawJson)
          })
        })
      })
    }.toString()

    val result = mockClient.parsePredictionResponse(wrappedResponse, 70)
    assertEquals("UP", result.prediction)
    assertEquals(91, result.confidence)
    assertEquals("OTC Exhaustion Candle Trap", result.otcPatternTrap)
    assertTrue(result.hasOtcTrap)
  }
}

