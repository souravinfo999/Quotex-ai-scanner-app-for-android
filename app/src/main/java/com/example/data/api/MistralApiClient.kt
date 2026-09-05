package com.example.data.api

import com.example.data.model.PredictionResult
import com.example.data.model.ScanSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class MistralApiClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun testConnection(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("❌ Please enter your Mistral API Key."))
        }

        try {
            val payload = JSONObject().apply {
                put("model", "pixtral-12b-2409")
                put("max_tokens", 10)
                val messages = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "ping")
                    })
                }
                put("messages", messages)
            }

            val request = Request.Builder()
                .url("https://api.mistral.ai/v1/chat/completions")
                .header("Authorization", "Bearer ${apiKey.trim()}")
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val code = response.code
            val body = response.body?.string().orEmpty()

            when (code) {
                200 -> Result.success("Connected successfully to Mistral AI!")
                401 -> Result.failure(Exception("❌ Invalid API Key. Please check and re-enter."))
                429 -> Result.failure(Exception("⏳ API limit reached. Wait a moment."))
                else -> {
                    val errMsg = extractErrorMessage(body) ?: "API error ($code)"
                    Result.failure(Exception("❌ $errMsg"))
                }
            }
        } catch (e: IOException) {
            Result.failure(Exception("📡 No Internet. Check connection and retry."))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Connection error"))
        }
    }

    suspend fun analyzeChart(
        base64Image: String,
        settings: ScanSettings
    ): Result<PredictionResult> = withContext(Dispatchers.IO) {
        val apiKey = settings.apiKey.trim()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("❌ Invalid API Key. Please check and re-enter."))
        }

        val modelToUse = if (settings.analysisMode.equals("deep", ignoreCase = true)) {
            "pixtral-12b-2409" // Vision supported
        } else {
            "pixtral-12b-2409"
        }

        try {
            val contentArray = JSONArray().apply {
                // Text prompt
                put(JSONObject().apply {
                    put("type", "text")
                    put("text", SYSTEM_PROMPT)
                })
                // Image part
                put(JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", "data:image/png;base64,$base64Image")
                })
            }

            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", contentArray)
                })
            }

            val requestJson = JSONObject().apply {
                put("model", modelToUse)
                put("temperature", 0.28)
                put("max_tokens", 800)
                put("response_format", JSONObject().apply {
                    put("type", "json_object")
                })
                put("messages", messagesArray)
            }

            val request = Request.Builder()
                .url("https://api.mistral.ai/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val code = response.code
            val body = response.body?.string().orEmpty()

            when (code) {
                200 -> {
                    val parsedResult = parsePredictionResponse(body, settings.confidenceThreshold)
                    Result.success(parsedResult)
                }
                401 -> Result.failure(Exception("❌ Invalid API Key. Please check and re-enter."))
                429 -> Result.failure(Exception("⏳ API limit reached. Wait a moment."))
                in 500..599 -> Result.failure(Exception("⚠️ Mistral server error. Try again."))
                else -> {
                    val errMsg = extractErrorMessage(body) ?: "API returned code $code"
                    Result.failure(Exception("❌ $errMsg"))
                }
            }
        } catch (e: IOException) {
            Result.failure(Exception("📡 No Internet. Check connection and retry."))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Analysis failed"))
        }
    }

    private fun extractErrorMessage(body: String): String? {
        return try {
            val json = JSONObject(body)
            if (json.has("message")) json.getString("message")
            else if (json.has("error")) {
                val err = json.get("error")
                if (err is JSONObject && err.has("message")) err.getString("message")
                else err.toString()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun parsePredictionResponse(responseBody: String, threshold: Int): PredictionResult {
        val root = JSONObject(responseBody)
        val choices = root.getJSONArray("choices")
        if (choices.length() == 0) {
            throw IllegalStateException("Empty response from AI")
        }
        val firstChoice = choices.getJSONObject(0)
        val message = firstChoice.getJSONObject("message")
        val contentStr = message.getString("content")

        // Clean possible markdown code fences
        val cleanJson = contentStr
            .replace("```json", "")
            .replace("```", "")
            .trim()

        val json = JSONObject(cleanJson)

        var prediction = json.optString("prediction", "UNCERTAIN").uppercase()
        if (prediction != "UP" && prediction != "DOWN") {
            prediction = "UNCERTAIN"
        }

        var confidence = json.optInt("confidence", 50)
        if (confidence < 0) confidence = 0
        if (confidence > 100) confidence = 100

        // If confidence is below threshold, mark as UNCERTAIN as required
        if (confidence < threshold && prediction != "UNCERTAIN") {
            prediction = "UNCERTAIN"
        }

        val primarySignal = json.optString("primary_signal", "Price Action analysis at Key Level")
        val confirmationsList = mutableListOf<String>()
        val confirmationsArray = json.optJSONArray("confirmations")
        if (confirmationsArray != null) {
            for (i in 0 until confirmationsArray.length()) {
                val conf = confirmationsArray.optString(i)
                if (conf.isNotBlank()) confirmationsList.add(conf)
            }
        }

        val candlePattern = json.optString("candle_pattern_found", "None")
        val srZone = json.optString("sr_zone", "None")
        val fvgDetected = json.optBoolean("fvg_detected", false)
        val trend = json.optString("trend", "Sideways")
        val riskLevel = json.optString("risk_level", "MEDIUM").uppercase()
        val advice = json.optString("advice", if (prediction == "UNCERTAIN") "Wait for clearer signal" else "Enter now")

        return PredictionResult(
            prediction = prediction,
            confidence = confidence,
            primarySignal = primarySignal,
            confirmations = confirmationsList,
            candlePatternFound = candlePattern,
            srZone = srZone,
            fvgDetected = fvgDetected,
            trend = trend,
            riskLevel = riskLevel,
            advice = advice,
            timestamp = System.currentTimeMillis()
        )
    }

    companion object {
        val SYSTEM_PROMPT = """
You are a senior algorithmic and price action binary options analyst specializing in 1-minute candlestick forecasting on Quotex and OTC trading platforms.

YOUR TASK:
Examine the trading chart screenshot and predict the direction of the IMMEDIATE NEXT CANDLE: "UP" (Call), "DOWN" (Put), or "UNCERTAIN".

CRITICAL FOCUS & CHART GEOMETRY:
1. FOCUS ON THE RIGHTMOST ACTIVE CANDLE:
   - On Quotex, historical candles are on the left; the active/latest candlestick is located on the FAR RIGHT next to the current price level, vertical dotted time-line, or price tag.
   - Do NOT base your prediction solely on the overall trend on the left. In 1-minute binary options, a strong bounce or rejection at a key level frequently produces winning counter-trend candles.

2. BALANCED & SYMMETRIC EVALUATION:
   Evaluate both Bullish (UP) and Bearish (DOWN) setups with equal weight and unbiased objectivity.

   A. BULLISH REASONS (PREDICT "UP"):
      - Prominent lower wick rejection showing strong buyer absorption from below.
      - Bullish candlestick patterns: Hammer, Inverted Hammer at support, Morning Star, Bullish Engulfing, Piercing Pattern, Bullish Harami.
      - Price bouncing off a horizontal Support line, order block, or round psychological number (e.g., .00, .50, .100).
      - Price filling a Fair Value Gap (FVG) or imbalance and finding upward support.
      - Oversold exhaustion after a drop, signaling an immediate relief/pullback green candle.

   B. BEARISH REASONS (PREDICT "DOWN"):
      - Prominent upper wick rejection showing strong seller defense from above.
      - Bearish candlestick patterns: Shooting Star, Hanging Man at resistance, Evening Star, Bearish Engulfing, Dark Cloud Cover, Bearish Harami.
      - Price getting rejected at a horizontal Resistance line, supply zone, or round psychological number.
      - Strong bearish momentum breakout below a previous floor with full body expansion.
      - Bearish Fair Value Gap (FVG) retest or liquidity sweep above highs followed by rejection.

   C. NEUTRAL / UNCERTAIN (PREDICT "UNCERTAIN"):
      - Doji or spinning top with equal wicks on both sides and no clear directional momentum.
      - Price trapped in choppy, low-volume horizontal consolidation with no clean S/R edge.
      - Conflicting indicators where neither buyers nor sellers have the advantage.

DECISION PROTOCOL:
- Compare the immediate bullish vs bearish price-action evidence at the rightmost candle.
- If Bullish evidence is stronger → Predict "UP".
- If Bearish evidence is stronger → Predict "DOWN".
- If evidence is ambiguous, low quality, or confidence is below 65% → Predict "UNCERTAIN".

OUTPUT STRICT JSON ONLY (no markdown fences, no explanatory text outside the JSON):
{
  "prediction": "UP" | "DOWN" | "UNCERTAIN",
  "confidence": 60-95,
  "primary_signal": "Concise summary of the key trigger (e.g. 'Lower Wick Support Bounce at Round Level' or 'Shooting Star Rejection at Resistance')",
  "confirmations": [
    "Specific confirmation 1 (e.g. 'Hammer candle formed on key support line')",
    "Specific confirmation 2 (e.g. 'Lower wick rejection indicates aggressive buyer absorption')",
    "Specific confirmation 3 (e.g. 'Bullish Fair Value Gap (FVG) mitigated')"
  ],
  "candle_pattern_found": "Name of the detected pattern (e.g. 'Hammer', 'Shooting Star', 'Bullish Engulfing', 'Bearish Engulfing', 'None')",
  "sr_zone": "Support" | "Resistance" | "None",
  "fvg_detected": true | false,
  "trend": "Bullish" | "Bearish" | "Sideways",
  "risk_level": "LOW" | "MEDIUM" | "HIGH",
  "advice": "ENTER NOW (CALL / UP)" | "ENTER NOW (PUT / DOWN)" | "WAIT FOR CLEAR SIGNAL"
}
""".trimIndent()
    }
}
