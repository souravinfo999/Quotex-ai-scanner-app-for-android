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
                put("temperature", 0.42)
                put("max_tokens", 900)
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

        // --- STRICT ANTI-HALLUCINATION & CONSISTENCY ENGINE ---
        // 1. Bearish Engulfing Sanity Check:
        // A Bearish Engulfing CANNOT occur if the latest candle was GREEN or DOJI!
        val isClaimingBearishEngulfing = candlePattern.contains("Bearish", ignoreCase = true) &&
                candlePattern.contains("Engulf", ignoreCase = true)
        if (isClaimingBearishEngulfing) {
            if (lastCandleColor.contains("GREEN") || lastCandleColor == "DOJI") {
                candlePattern = if (wickRejection.contains("LOWER")) "Hammer / Bullish Pin Bar"
                else "Bullish Momentum Candle"
            } else if (lastCandleColor.contains("RED") && prevCandleColor.contains("RED")) {
                candlePattern = "Bearish Momentum / Trend Continuation"
            }
        }

        // 2. Bullish Engulfing Sanity Check:
        val isClaimingBullishEngulfing = candlePattern.contains("Bullish", ignoreCase = true) &&
                candlePattern.contains("Engulf", ignoreCase = true)
        if (isClaimingBullishEngulfing) {
            if (lastCandleColor.contains("RED") || lastCandleColor == "DOJI") {
                candlePattern = if (wickRejection.contains("UPPER")) "Shooting Star / Upper Wick Rejection"
                else "Bearish Momentum Candle"
            }
        }

        // 3. Directional Evidence Consistency Check:
        val allConfText = confirmationsList.joinToString(" ").lowercase()
        val bullishScore = listOf("support", "lower wick", "buyer", "bounce", "hammer", "demand", "bullish", "green", "absorption")
            .count { allConfText.contains(it) }
        val bearishScore = listOf("resistance", "upper wick", "seller", "breakdown", "shooting star", "supply", "bearish", "red", "rejection at resistance")
            .count { allConfText.contains(it) }

        if (prediction == "DOWN" && (lastCandleColor.contains("GREEN") || wickRejection.contains("LOWER") || keyLevel.contains("SUPPORT"))) {
            if (bullishScore > bearishScore) {
                prediction = "UP"
            }
        } else if (prediction == "UP" && (lastCandleColor.contains("RED") || wickRejection.contains("UPPER") || keyLevel.contains("RESISTANCE"))) {
            if (bearishScore > bullishScore) {
                prediction = "DOWN"
            }
        }

        var confidence = json.optInt("confidence", if (prediction == "NO_CHART") 0 else 78)
        if (confidence < 0) confidence = 0
        if (confidence > 100) confidence = 100

        // If confidence is below threshold, mark as UNCERTAIN (unless NO_CHART)
        if (prediction != "NO_CHART" && confidence < threshold && prediction != "UNCERTAIN") {
            prediction = "UNCERTAIN"
        }

        val defaultPrimary = when (prediction) {
            "NO_CHART" -> "No trading candlestick chart detected. Please open Quotex or trading screen and scan again."
            "UP" -> "Bullish Price Action Reaction at Support Zone"
            "DOWN" -> "Bearish Price Action Rejection at Resistance Zone"
            else -> "Market Consolidating / Wait for Confirmation"
        }
        var primarySignal = json.optString("primary_signal", defaultPrimary)
        if (prediction == "UP" && primarySignal.contains("Bearish", ignoreCase = true)) {
            primarySignal = "Bullish Price Action Reaction at Key Support"
        } else if (prediction == "DOWN" && primarySignal.contains("Bullish", ignoreCase = true)) {
            primarySignal = "Bearish Price Action Rejection at Key Resistance"
        }

        if (prediction == "NO_CHART" && confirmationsList.isEmpty()) {
            confirmationsList.add("No financial candlesticks found")
            confirmationsList.add("Screen displays non-chart content")
            confirmationsList.add("Open Quotex/Trading platform and re-scan")
        }

        val srZone = json.optString("sr_zone", if (prediction == "UP") "Support Level" else if (prediction == "DOWN") "Resistance Level" else "None")
        val fvgDetected = json.optBoolean("fvg_detected", false)
        val trend = json.optString("trend", if (prediction == "NO_CHART") "None" else if (prediction == "UP") "Bullish" else if (prediction == "DOWN") "Bearish" else "Sideways")
        val riskLevel = json.optString("risk_level", if (prediction == "NO_CHART") "HIGH" else "LOW").uppercase()
        val defaultAdvice = when (prediction) {
            "NO_CHART" -> "OPEN TRADING CHART & TRY AGAIN"
            "UP" -> "ENTER NOW (CALL / UP)"
            "DOWN" -> "ENTER NOW (PUT / DOWN)"
            else -> "WAIT FOR CLEAR SIGNAL"
        }
        val advice = json.optString("advice", defaultAdvice)

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

TASK:
Analyze the provided screenshot of a trading chart and forecast the IMMEDIATE NEXT CANDLE: "UP" (Call / Green), "DOWN" (Put / Red), or "UNCERTAIN".

STEP 0: SCREENSHOT VERIFICATION (STRICT):
Check if this screenshot contains an active financial candlestick trading chart (Quotex, Pocket Option, TradingView, MetaTrader, candlestick chart with green/red candles and price levels).
If the screenshot shows a browser (articles, search, scribd), home screen, camera, settings, document, or non-trading app:
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
    "Screen displays non-trading app or text document",
    "Please open Quotex or your trading platform to scan"
  ],
  "prediction": "NO_CHART",
  "confidence": 0,
  "sr_zone": "None",
  "fvg_detected": false,
  "risk_level": "HIGH",
  "advice": "OPEN TRADING CHART & TRY AGAIN"
}

STEP 1: ACCURATE VISUAL INSPECTION (CRITICAL RULES):
1. CANDLESTICK COLORS:
   - GREEN / CYAN = Bullish candle (price closed higher than open).
   - RED / ORANGE = Bearish candle (price closed lower than open).
   - Historical candles are on the left; the active/last closed candle is on the FAR RIGHT next to the current price line.

2. IGNORE APP INTERFACE BUTTONS (VERY IMPORTANT):
   - Quotex and mobile brokers have large interface trade buttons (a green "UP" / Call button and a red "DOWN" / Put button) at the bottom or side.
   - DO NOT let these app buttons bias your reading! Inspect ONLY the candlesticks drawn on the grid in the chart area.

3. STRICT CANDLESTICK PATTERN RULES (ZERO HALLUCINATION):
   - "Bearish Engulfing": REQUIRES that:
     (1) The previous candle was clearly GREEN (Bullish).
     (2) The latest closed candle is clearly RED (Bearish).
     (3) The RED candle's body COMPLETELY covers and engulfs the entire body of the previous GREEN candle.
     * IF THE CURRENT CANDLE IS GREEN, IT IS 100% IMPOSSIBLE TO BE A BEARISH ENGULFING. DO NOT HALLUCINATE BEARISH ENGULFING ON GREEN CANDLES! *
   - "Bullish Engulfing": REQUIRES that:
     (1) The previous candle was RED (Bearish).
     (2) The latest closed candle is GREEN (Bullish).
     (3) The GREEN candle's body COMPLETELY covers and engulfs the previous RED candle's body.
   - "Hammer / Bullish Pin Bar": Long LOWER wick (rejection of lower prices at support), small body at the top. Strongly indicates BUYERS entering -> Predict "UP"!
   - "Shooting Star / Bearish Pin Bar": Long UPPER wick (rejection of higher prices at resistance), small body at the bottom. Strongly indicates SELLERS defending -> Predict "DOWN"!
   - "Morning Star": Strong red candle -> small base/doji at support -> strong green candle -> Predict "UP"!
   - "Evening Star": Strong green candle -> small star at resistance -> strong red candle -> Predict "DOWN"!

STEP 2: BALANCED SYMMETRICAL EVALUATION:
Do NOT default to "DOWN". Symmetrically evaluate Bullish and Bearish evidence:

A. PREDICT "UP" (Call) WHEN:
- Prominent LOWER wick rejection showing strong buyer absorption from below.
- Price bouncing off a horizontal Support line, order block, round number, or lower Bollinger Band / moving average.
- Bullish pattern: Hammer, Bullish Engulfing, Morning Star, Piercing Pattern, or Consecutive Strong Green candles (Three White Soldiers).
- Price sweeping liquidity below a low and quickly snapping back up.

B. PREDICT "DOWN" (Put) WHEN:
- Prominent UPPER wick rejection showing seller defense from above.
- Price rejected at a horizontal Resistance line, supply zone, round number, or upper band.
- Bearish pattern: Shooting Star, Bearish Engulfing (RED candle engulfing GREEN), Evening Star, Dark Cloud Cover, or Consecutive Strong Red candles.
- Price sweeping liquidity above a high and breaking downward.

C. PREDICT "UNCERTAIN" WHEN:
- Equal wicks on both sides (Doji / Spinning Top) with no directional edge.
- Price trapped in tight horizontal consolidation with low volume.

OUTPUT FORMAT (STRICT JSON ONLY - observations MUST come first to ground your analysis):
{
  "is_chart_detected": true,
  "chart_observations": {
    "last_closed_candle_color": "GREEN" | "RED" | "DOJI",
    "candle_before_last_color": "GREEN" | "RED",
    "wick_rejection_type": "LOWER_WICK_REJECTION" | "UPPER_WICK_REJECTION" | "EQUAL_WICKS" | "NO_WICKS",
    "candle_size": "LARGE_BODY" | "MEDIUM_BODY" | "SMALL_OR_DOJI",
    "key_level_interaction": "BOUNCING_FROM_SUPPORT" | "REJECTED_AT_RESISTANCE" | "BREAKOUT_SUPPORT" | "BREAKOUT_RESISTANCE" | "MID_CHANNEL_NO_LEVEL"
  },
  "candle_pattern_found": "Hammer / Bullish Pin Bar" | "Shooting Star" | "Bullish Engulfing" | "Bearish Engulfing" | "Morning Star" | "Evening Star" | "Green Momentum" | "Red Momentum" | "None",
  "trend": "Bullish" | "Bearish" | "Sideways",
  "primary_signal": "Concise summary of trigger (e.g. 'Strong Lower Wick Rejection at Support Level' or 'Shooting Star Rejection at Resistance')",
  "confirmations": [
    "Specific confirmation 1 (e.g. 'Long lower wick indicates aggressive buyer absorption')",
    "Specific confirmation 2 (e.g. 'Reaction off horizontal support line')",
    "Specific confirmation 3 (e.g. 'Bullish momentum following consolidation')"
  ],
  "prediction": "UP" | "DOWN" | "UNCERTAIN",
  "confidence": 75-95,
  "sr_zone": "Support Zone" | "Resistance Zone" | "None",
  "fvg_detected": true | false,
  "risk_level": "LOW" | "MEDIUM" | "HIGH",
  "advice": "ENTER NOW (CALL / UP)" | "ENTER NOW (PUT / DOWN)" | "WAIT FOR CLEAR SIGNAL"
}
""".trimIndent()
    }
}
