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

        val srZone = json.optString("sr_zone", if (prediction == "UP") "Support / Demand Zone" else if (prediction == "DOWN") "Resistance / Supply Zone" else "None")
        
        // SMC: Fair Value Gap (FVG)
        val rawFvgType = json.optString("fvg_type", "NONE").uppercase()
        val fvgDetected = json.optBoolean("fvg_detected", false) || (rawFvgType != "NONE" && rawFvgType.contains("FVG"))
        
        // SMC: Order Block (OB)
        val rawOb = json.optString("order_block", "NONE").uppercase()
        val orderBlockZone = when {
            rawOb.contains("BULLISH") || rawOb.contains("DEMAND") -> "Bullish Order Block (Demand)"
            rawOb.contains("BEARISH") || rawOb.contains("SUPPLY") -> "Bearish Order Block (Supply)"
            else -> "None"
        }

        // SMC: Liquidity Sweep (BSL / SSL)
        val rawSweep = json.optString("liquidity_sweep", "NONE").uppercase()
        val liquiditySweep = when {
            rawSweep.contains("SSL") || rawSweep.contains("SELL_SIDE") -> "Sell-Side Liquidity Swept (SSL)"
            rawSweep.contains("BSL") || rawSweep.contains("BUY_SIDE") -> "Buy-Side Liquidity Swept (BSL)"
            else -> "None"
        }

        // Accurate Trend Extraction:
        val marketStructure = observations?.optString("market_structure", "")?.uppercase() ?: ""
        var trend = json.optString("trend", "").trim()
        if (trend.isBlank() || trend.equals("None", ignoreCase = true)) {
            trend = when {
                prediction == "NO_CHART" -> "None"
                marketStructure.contains("BEARISH") || marketStructure.contains("LH_LL") -> "Bearish"
                marketStructure.contains("BULLISH") || marketStructure.contains("HH_HL") -> "Bullish"
                else -> "Sideways"
            }
        }

        val isDowntrend = trend.equals("Bearish", ignoreCase = true) ||
                marketStructure.contains("BEARISH") || marketStructure.contains("LH_LL")
        val isUptrend = trend.equals("Bullish", ignoreCase = true) ||
                marketStructure.contains("BULLISH") || marketStructure.contains("HH_HL")

        // --- STRICT PRO-TREND ENFORCEMENT ENGINE ---
        // Rule: NEVER trade counter-trend against strong market momentum!
        // In a Downtrend: Only DOWN trades or UNCERTAIN (WAIT) are allowed. Counter-trend UP calls are blocked.
        var finalPrediction = prediction
        var finalConfidence = confidence
        var finalPrimarySignal = primarySignal
        var finalAdvice = advice

        if (isDowntrend && finalPrediction == "UP") {
            finalPrediction = "UNCERTAIN"
            finalConfidence = (finalConfidence - 25).coerceIn(45, 65)
            finalPrimarySignal = "Counter-Trend Filter: Strong Downtrend Active (Counter-trend CALL blocked)"
            finalAdvice = "WAIT FOR PRO-TREND SIGNAL"
            confirmationsList.add(0, "Major trend is Bearish (Lower Highs & Lower Lows)")
            confirmationsList.add(1, "Pro-Trend rule: Never trade UP against an active downtrend")
            confirmationsList.add(2, "Wait for pullback to resistance for a high-probability DOWN trade")
        } else if (isUptrend && finalPrediction == "DOWN") {
            finalPrediction = "UNCERTAIN"
            finalConfidence = (finalConfidence - 25).coerceIn(45, 65)
            finalPrimarySignal = "Counter-Trend Filter: Strong Uptrend Active (Counter-trend PUT blocked)"
            finalAdvice = "WAIT FOR PRO-TREND SIGNAL"
            confirmationsList.add(0, "Major trend is Bullish (Higher Highs & Higher Lows)")
            confirmationsList.add(1, "Pro-Trend rule: Never trade DOWN against an active uptrend")
            confirmationsList.add(2, "Wait for pullback to support for a high-probability UP trade")
        }

        val riskLevel = json.optString("risk_level", when (finalPrediction) {
            "UP", "DOWN" -> "LOW"
            "UNCERTAIN" -> "MEDIUM"
            else -> "HIGH"
        }).uppercase()

        return PredictionResult(
            prediction = finalPrediction,
            isChartDetected = (finalPrediction != "NO_CHART"),
            confidence = finalConfidence,
            primarySignal = finalPrimarySignal,
            confirmations = confirmationsList,
            candlePatternFound = candlePattern,
            srZone = srZone,
            fvgDetected = fvgDetected,
            orderBlockZone = orderBlockZone,
            liquiditySweep = liquiditySweep,
            trend = trend,
            riskLevel = riskLevel,
            advice = finalAdvice,
            timestamp = System.currentTimeMillis()
        )
    }

    companion object {
        val SYSTEM_PROMPT = """
You are a disciplined algorithmic institutional Price Action & SMC analyst specializing in 1-minute binary options forecasting on Quotex, Pocket Option, and OTC charts.

### #1 GOLDEN RULE: "THE TREND IS YOUR FRIEND" (STRICT PRO-TREND TRADING):
Binary options traders LOSE money when they try to catch tops and bottoms against a strong trend.
- In a DOWNTREND: ONLY forecast "DOWN" (PUT / Red candle continuation or resistance rejection), OR forecast "UNCERTAIN" (Wait). NEVER forecast "UP" against a strong downtrend!
- In an UPTREND: ONLY forecast "UP" (CALL / Green candle continuation or support bounce), OR forecast "UNCERTAIN" (Wait). NEVER forecast "DOWN" against a strong uptrend!
- If conditions do not align with the trend: DO NOT FORCE A TRADE. Forecast "UNCERTAIN" and advise "WAIT FOR CLEAR SIGNAL".

### ELIMINATE "UP" BIAS (50/50 SYMMETRICAL TRADING):
- Do NOT favor "UP" over "DOWN". Put (DOWN) signals are equally frequent and profitable!
- When prices are falling and red candles are dominant, you MUST confidently forecast "DOWN".

STEP 0: SCREENSHOT VERIFICATION:
Verify this screenshot contains an active financial candlestick trading chart (Quotex, Pocket Option, TradingView, green/red candles on a grid).
If the screenshot shows a browser without chart, home screen, settings, camera, or non-trading app:
Return IMMEDIATELY:
{
  "is_chart_detected": false,
  "chart_observations": {
    "last_closed_candle_color": "NONE",
    "candle_before_last_color": "NONE",
    "wick_rejection_type": "NONE",
    "candle_size": "NONE",
    "market_structure": "NONE",
    "key_level_interaction": "NONE"
  },
  "fvg_detected": false,
  "fvg_type": "NONE",
  "order_block": "NONE",
  "liquidity_sweep": "NONE",
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
  "risk_level": "HIGH",
  "advice": "OPEN TRADING CHART & TRY AGAIN"
}

STEP 1: MACRO TREND IDENTIFICATION (FULL CHART VIEW):
1. LOOK AT THE WHOLE CHART FROM LEFT TO RIGHT:
   - Compare the price on the LEFT of the chart to the price on the RIGHT (current price):
   - If the price on the left is HIGHER and the price on the right is LOWER (candles cascading down in a series of Lower Highs and Lower Lows):
     -> THIS IS A DOWNTREND! Set trend: "Bearish", market_structure: "BEARISH_LH_LL".
     -> DO NOT classify a falling chart as "Sideways"!
   - If the price on the left is LOWER and the price on the right is HIGHER (candles climbing up in Higher Highs and Higher Lows):
     -> THIS IS AN UPTREND! Set trend: "Bullish", market_structure: "BULLISH_HH_HL".
   - Only if price has oscillated strictly horizontally between the same top and bottom ceiling is it "Sideways".

2. CRITICAL WARNING: MOBILE BROKER VIEWPORT & BUTTONS:
   - In Quotex mobile, when price drops, candles appear near the bottom of the screen. DO NOT MISTAKE THE BOTTOM OF THE SCREEN FOR A "SUPPORT LEVEL"! It is a crashing market making new lows.
   - At the bottom of Quotex there are large green "Up" and red "Down" trade buttons. IGNORE THOSE BUTTONS COMPLETELY. Look ONLY at the candlestick candles in the chart grid.

STEP 2: SMC & PRICE ACTION OBSERVATIONS:
1. Candlestick on Far Right (Most recent closed candle):
   - Color: GREEN / CYAN (Bullish) vs RED / ORANGE (Bearish) vs DOJI.
   - Wick Rejection: Long LOWER wick (buyer defense) vs Long UPPER wick (seller defense) vs Equal wicks.
2. Fair Value Gap (FVG):
   - Bullish FVG: 3-candle imbalance. Price retraces into gap and bounces UP.
   - Bearish FVG: 3-candle imbalance. Price pulls up into gap and rejects DOWN.
3. Order Block (OB):
   - Bullish OB (Demand): Last red candle before major rally. Retest = bounce UP.
   - Bearish OB (Supply): Last green candle before major plunge. Retest = reject DOWN.
4. Liquidity Sweeps:
   - SSL Swept: Price pierced below swing low, grabbed stops, and snapped up with long lower wick.
   - BSL Swept: Price spiked above swing high, grabbed stops, and dropped with long upper wick.

STEP 3: PRO-TREND TRADING DECISIONS:

A. FORECAST "DOWN" (Put / Red candle expected) WHEN:
- Market is in a DOWNTREND (Bearish LH-LL) and:
  * Red momentum continuation breaking below recent candle low.
  * Price pulled back up to resistance or Bearish Order Block/FVG and rejected with upper wick.
  * Bearish Engulfing or Shooting Star pattern.
- Or Market in Range and cleanly rejected at Resistance ceiling.
-> confidence: 78 to 92
-> advice: "ENTER NOW (PUT / DOWN)"

B. FORECAST "UP" (Call / Green candle expected) WHEN:
- Market is in an UPTREND (Bullish HH-HL) and:
  * Green momentum continuation breaking above recent candle high.
  * Price pulled back down to support or Bullish Order Block/FVG and bounced with lower wick.
  * Bullish Engulfing or Hammer pattern.
- Or Market in Range and cleanly bounced at Support floor.
-> confidence: 78 to 92
-> advice: "ENTER NOW (CALL / UP)"

C. FORECAST "UNCERTAIN" (Wait / Avoid Counter-Trend) WHEN:
- Market is in a DOWNTREND, but latest candle formed a green bounce or lower wick -> DO NOT CALL UP! Forecast "UNCERTAIN", advice: "WAIT FOR PULLBACK TO RESISTANCE".
- Market is in an UPTREND, but latest candle formed a red pullback -> DO NOT CALL DOWN! Forecast "UNCERTAIN", advice: "WAIT FOR PULLBACK TO SUPPORT".
- Candle is a Doji, spinning top, or trapped in tight chop.
-> confidence: 45 to 65
-> advice: "WAIT FOR CLEAR SIGNAL"
-> primary_signal: "Counter-Trend or Choppy Market / Wait for Trend Alignment"

OUTPUT FORMAT (STRICT JSON ONLY):
{
  "is_chart_detected": true,
  "chart_observations": {
    "last_closed_candle_color": "GREEN" | "RED" | "DOJI",
    "candle_before_last_color": "GREEN" | "RED",
    "wick_rejection_type": "LOWER_WICK_REJECTION" | "UPPER_WICK_REJECTION" | "EQUAL_WICKS" | "NO_WICKS",
    "candle_size": "LARGE_BODY" | "MEDIUM_BODY" | "SMALL_OR_DOJI",
    "market_structure": "BEARISH_LH_LL" | "BULLISH_HH_HL" | "RANGE_SIDEWAYS",
    "key_level_interaction": "BOUNCING_FROM_SUPPORT_OR_DEMAND" | "REJECTED_AT_RESISTANCE_OR_SUPPLY" | "MID_CHANNEL_NO_LEVEL"
  },
  "fvg_detected": true | false,
  "fvg_type": "BULLISH_FVG" | "BEARISH_FVG" | "NONE",
  "order_block": "BULLISH_ORDER_BLOCK" | "BEARISH_ORDER_BLOCK" | "NONE",
  "liquidity_sweep": "SSL_SWEPT_BULLISH" | "BSL_SWEPT_BEARISH" | "NONE",
  "candle_pattern_found": "Hammer / Bullish Pin Bar" | "Shooting Star" | "Bullish Engulfing" | "Bearish Engulfing" | "Morning Star" | "Evening Star" | "Doji / Indecision" | "None",
  "sr_zone": "Support / Demand Zone" | "Resistance / Supply Zone" | "Round Number Level" | "None",
  "trend": "Bullish" | "Bearish" | "Sideways",
  "primary_signal": "Concise trigger summary (e.g. 'Bearish Trend Continuation with Red Momentum' or 'Bearish Rejection at Order Block')",
  "confirmations": [
    "Observation 1 (macro trend alignment)",
    "Observation 2 (candle color & wick rejection)",
    "Observation 3 (SMC structure or S/R zone)"
  ],
  "prediction": "UP" | "DOWN" | "UNCERTAIN",
  "confidence": 50-95,
  "risk_level": "LOW" | "MEDIUM" | "HIGH",
  "advice": "ENTER NOW (CALL / UP)" | "ENTER NOW (PUT / DOWN)" | "WAIT FOR CLEAR SIGNAL"
}
""".trimIndent()
    }
}
