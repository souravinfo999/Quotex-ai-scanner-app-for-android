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
                put("temperature", 0.20)
                put("max_tokens", 850)
                put("response_format", JSONObject().apply {
                    put("type", "json_object")
                })
                put("messages", messagesArray)
            }

            val request = Request.Builder()
                .url("https://api.mistral.ai/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
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

        val isChartDetected = json.optBoolean("is_chart_detected", true)
        var rawPrediction = json.optString("prediction", "UNCERTAIN").uppercase().trim()

        // Normalize raw prediction to standard states
        var prediction = when {
            !isChartDetected || rawPrediction == "NO_CHART" || rawPrediction.contains("NOT_FOUND") || rawPrediction.contains("NO_CHART") -> "NO_CHART"
            rawPrediction == "UP" || rawPrediction.contains("CALL") || rawPrediction.contains("BUY") || rawPrediction == "BULLISH" -> "UP"
            rawPrediction == "DOWN" || rawPrediction.contains("PUT") || rawPrediction.contains("SELL") || rawPrediction == "BEARISH" -> "DOWN"
            else -> "UNCERTAIN"
        }

        val observations = json.optJSONObject("chart_observations")
        val lastCandleColor = observations?.optString("last_closed_candle_color", "")?.uppercase() ?: ""
        val prevCandleColor = observations?.optString("candle_before_last_color", "")?.uppercase() ?: ""
        val wickRejection = observations?.optString("wick_rejection_type", "")?.uppercase() ?: ""
        val keyLevel = observations?.optString("key_level_interaction", "")?.uppercase() ?: ""

        var candlePattern = json.optString("candle_pattern_found", "None").trim()
        val confirmationsList = mutableListOf<String>()
        val confirmationsArray = json.optJSONArray("confirmations")
        if (confirmationsArray != null) {
            for (i in 0 until confirmationsArray.length()) {
                val conf = confirmationsArray.optString(i)
                if (conf.isNotBlank()) confirmationsList.add(conf)
            }
        }

        // --- STRICT PATTERN SANITY CHECK (DO NOT ALTER PREDICTION) ---
        // A Bearish Engulfing CANNOT occur if the latest candle is GREEN
        val isClaimingBearishEngulfing = candlePattern.contains("Bearish", ignoreCase = true) &&
                candlePattern.contains("Engulf", ignoreCase = true)
        if (isClaimingBearishEngulfing && lastCandleColor.contains("GREEN")) {
            candlePattern = if (wickRejection.contains("LOWER")) "Hammer / Bullish Pin Bar" else "Bullish Reaction"
        }

        // A Bullish Engulfing CANNOT occur if the latest candle is RED
        val isClaimingBullishEngulfing = candlePattern.contains("Bullish", ignoreCase = true) &&
                candlePattern.contains("Engulf", ignoreCase = true)
        if (isClaimingBullishEngulfing && lastCandleColor.contains("RED")) {
            candlePattern = if (wickRejection.contains("UPPER")) "Shooting Star / Bearish Pin Bar" else "Bearish Reaction"
        }

        val rawConfidence = json.optInt("confidence", if (prediction == "NO_CHART") 0 else 65)
        val confidence = rawConfidence.coerceIn(0, 100)

        // If confidence is below the threshold or the setup is ambiguous, trigger WAIT / UNCERTAIN
        if (prediction != "NO_CHART" && (confidence < threshold || prediction == "UNCERTAIN")) {
            prediction = "UNCERTAIN"
        }

        val defaultAdvice = when (prediction) {
            "NO_CHART" -> "OPEN TRADING CHART & TRY AGAIN"
            "UP" -> "ENTER NOW (CALL / UP)"
            "DOWN" -> "ENTER NOW (PUT / DOWN)"
            else -> "WAIT FOR CLEAR SIGNAL"
        }
        val advice = json.optString("advice", defaultAdvice).ifBlank { defaultAdvice }

        val defaultPrimary = when (prediction) {
            "NO_CHART" -> "No trading candlestick chart detected. Open Quotex or trading platform and re-scan."
            "UP" -> "Bullish Price Action Reaction"
            "DOWN" -> "Bearish Price Action Reaction"
            else -> "Market Indecision / Wait for Clear Confirmation"
        }
        val primarySignal = json.optString("primary_signal", defaultPrimary).ifBlank { defaultPrimary }

        if (prediction == "NO_CHART" && confirmationsList.isEmpty()) {
            confirmationsList.add("No financial candlesticks found")
            confirmationsList.add("Screen displays non-chart content")
            confirmationsList.add("Open Quotex/Trading platform and re-scan")
        } else if (confirmationsList.isEmpty()) {
            when (prediction) {
                "UP" -> {
                    confirmationsList.add("Bullish candle reaction observed")
                    confirmationsList.add("Buyers defending support or upward momentum")
                }
                "DOWN" -> {
                    confirmationsList.add("Bearish candle rejection observed")
                    confirmationsList.add("Sellers defending resistance or downward momentum")
                }
                else -> {
                    confirmationsList.add("No clean rejection or breakout trigger")
                    confirmationsList.add("Wait for next candle to establish edge")
                }
            }
        }

        val srZone = json.optString("sr_zone", if (prediction == "UP") "Support Level" else if (prediction == "DOWN") "Resistance Level" else "None")
        val fvgDetected = json.optBoolean("fvg_detected", false)
        val trend = json.optString("trend", if (prediction == "NO_CHART") "None" else if (prediction == "UP") "Bullish" else if (prediction == "DOWN") "Bearish" else "Sideways")
        val riskLevel = json.optString("risk_level", when (prediction) {
            "UP", "DOWN" -> "LOW"
            "UNCERTAIN" -> "MEDIUM"
            else -> "HIGH"
        }).uppercase()

        return PredictionResult(
            prediction = prediction,
            isChartDetected = (prediction != "NO_CHART"),
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
You are an expert algorithmic binary options price action analyst specializing in 1-minute candlestick forecasting on Quotex, Pocket Option, and OTC trading charts.

CRITICAL DIRECTIVE - INDEPENDENT & UNBIASED EVALUATION:
- Analyze THIS screenshot purely on its own merits from scratch.
- DO NOT assume previous scans or reuse old assumptions.
- DO NOT default to always "UP".
- DO NOT default to always "DOWN".
- DO NOT default to always "UNCERTAIN".
- Follow this exact rule:
  * If the chart has clear Bullish price action -> Forecast "UP" with high confidence.
  * If the chart has clear Bearish price action -> Forecast "DOWN" with high confidence.
  * If the chart is trapped in consolidation, has a Doji, has equal wicks, or lacks a clear edge -> Forecast "UNCERTAIN", assign lower confidence (< 70), and advise "WAIT FOR CLEAR SIGNAL".

STEP 0: SCREENSHOT VERIFICATION:
Check if this screenshot contains an active financial candlestick trading chart (Quotex, Pocket Option, TradingView, green/red candles on a grid).
If the screenshot shows a browser, home screen, settings, or non-trading app:
Return IMMEDIATELY:
{
  "is_chart_detected": false,
  "chart_observations": {
    "last_closed_candle_color": "NONE",
    "candle_before_last_color": "NONE",
    "wick_rejection_type": "NONE",
    "candle_size": "NONE",
    "key_level_interaction": "NONE"
  },
  "candle_pattern_found": "None",
  "trend": "None",
  "primary_signal": "No financial trading chart found on this screen.",
  "confirmations": [
    "No financial candlestick chart detected",
    "Please open Quotex or your trading platform to scan"
  ],
  "prediction": "NO_CHART",
  "confidence": 0,
  "sr_zone": "None",
  "fvg_detected": false,
  "risk_level": "HIGH",
  "advice": "OPEN TRADING CHART & TRY AGAIN"
}

STEP 1: VISUAL INSPECTION (PRICE ACTION ONLY):
1. Candlestick on Far Right (Most recent closed candle next to price line):
   - GREEN / CYAN: Price closed higher than opened.
   - RED / ORANGE: Price closed lower than opened.
   - DOJI: Flat thin line, open equals close.
2. Wick Rejections:
   - Long LOWER wick: Buyers pushed price up from lows (bullish defense).
   - Long UPPER wick: Sellers pushed price down from highs (bearish defense).
   - Equal or No wicks: Indecision or pure momentum.
3. Ignore broker UI buttons (Ignore the big green/red Call/Put buttons on the screen). Look ONLY at the candles on the chart grid.

STEP 2: SYMMETRICAL 3-WAY DECISION RULES:

A. PREDICT "UP" (Call / Green candle expected) WHEN:
- Clean bounce from a Support line or lower band with prominent lower wick.
- Bullish Hammer / Pin Bar at support.
- Bullish Engulfing (current green body fully covers previous red body).
- Consecutive strong green momentum candles breaking above a range.
-> confidence: 75 to 92
-> advice: "ENTER NOW (CALL / UP)"

B. PREDICT "DOWN" (Put / Red candle expected) WHEN:
- Clean rejection from a Resistance line or upper band with prominent upper wick.
- Shooting Star / Bearish Pin Bar at resistance.
- Bearish Engulfing (current red body fully covers previous green body).
- Consecutive strong red momentum candles breaking below a range.
-> confidence: 75 to 92
-> advice: "ENTER NOW (PUT / DOWN)"

C. PREDICT "UNCERTAIN" (Wait / No Trade) WHEN:
- Price is floating in the middle of a channel without touching Support or Resistance.
- Candle is a Doji, spinning top, or has equal wicks on both top and bottom.
- Conflicting signals (e.g. green candle hitting direct resistance without breakout, or red candle hitting direct support).
- Low volatility, sideways chop, or uncertain market structure.
-> confidence: 45 to 65
-> advice: "WAIT FOR CLEAR SIGNAL"
-> primary_signal: "Market Consolidating / Wait for Confirmation"

OUTPUT FORMAT (STRICT JSON ONLY):
{
  "is_chart_detected": true,
  "chart_observations": {
    "last_closed_candle_color": "GREEN" | "RED" | "DOJI",
    "candle_before_last_color": "GREEN" | "RED",
    "wick_rejection_type": "LOWER_WICK_REJECTION" | "UPPER_WICK_REJECTION" | "EQUAL_WICKS" | "NO_WICKS",
    "candle_size": "LARGE_BODY" | "MEDIUM_BODY" | "SMALL_OR_DOJI",
    "key_level_interaction": "BOUNCING_FROM_SUPPORT" | "REJECTED_AT_RESISTANCE" | "BREAKOUT_SUPPORT" | "BREAKOUT_RESISTANCE" | "MID_CHANNEL_NO_LEVEL"
  },
  "candle_pattern_found": "Hammer / Bullish Pin Bar" | "Shooting Star" | "Bullish Engulfing" | "Bearish Engulfing" | "Morning Star" | "Evening Star" | "Momentum Continuation" | "Doji / Indecision" | "None",
  "trend": "Bullish" | "Bearish" | "Sideways",
  "primary_signal": "Concise trigger summary (e.g. 'Hammer bounce off support' or 'Shooting star rejection at resistance' or 'Mid-channel consolidation')",
  "confirmations": [
    "Observation 1 (candle color & wick state)",
    "Observation 2 (support/resistance interaction or range location)"
  ],
  "prediction": "UP" | "DOWN" | "UNCERTAIN",
  "confidence": 50-95,
  "sr_zone": "Support Zone" | "Resistance Zone" | "None",
  "fvg_detected": false,
  "risk_level": "LOW" | "MEDIUM" | "HIGH",
  "advice": "ENTER NOW (CALL / UP)" | "ENTER NOW (PUT / DOWN)" | "WAIT FOR CLEAR SIGNAL"
}
""".trimIndent()
    }
}
