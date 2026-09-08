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

    internal fun parsePredictionResponse(responseBody: String, threshold: Int): PredictionResult {
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

        // Quotex OTC Trap & Strategy Pattern:
        val rawOtcStrategy = json.optString("otc_strategy", "None").trim()
        val otcPatternTrap = if (rawOtcStrategy.isBlank() || rawOtcStrategy.equals("NONE", ignoreCase = true)) "None" else rawOtcStrategy

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

        // --- MULTI-FACTOR COMBINATION ENGINE ---
        // Balanced evaluation: If the AI identifies high-confluence Bullish triggers (Support bounce,
        // Bullish Order Block, SSL Sweep, OTC Red Exhaustion, or Uptrend Continuation), forecast UP.
        // If Bearish triggers (Resistance rejection, Bearish Order Block, BSL Sweep, OTC Green Exhaustion,
        // or Downtrend Continuation), forecast DOWN.
        val finalPrediction = prediction
        val finalConfidence = confidence
        val finalPrimarySignal = primarySignal
        val finalAdvice = advice

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
            otcPatternTrap = otcPatternTrap,
            trend = trend,
            riskLevel = riskLevel,
            advice = finalAdvice,
            timestamp = System.currentTimeMillis()
        )
    }

    companion object {
        val SYSTEM_PROMPT = """
You are a disciplined algorithmic institutional Price Action, SMC, and Quotex OTC trading analyst specializing in 1-minute binary options forecasting.

### ⚖️ CRITICAL MANDATE: ABSOLUTE 50/50 OBJECTIVITY (ZERO DIRECTIONAL BIAS):
Financial markets and Quotex OTC charts offer equal opportunities for CALL (UP) and PUT (DOWN) trades!
- You MUST evaluate Bullish (UP) and Bearish (DOWN) setups with complete 50/50 mathematical neutrality.
- NEVER assume or default that the market is falling or rising.
- Look directly at the real candlesticks, their true colors (Green/Cyan vs Red/Orange), body sizes, wick rejections, and support/resistance zones.

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
  "otc_strategy": "NONE",
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

STEP 1: CHART STRUCTURE & MACRO/MICRO TREND:
Look at the candlestick sequence across the chart:
1. UPTREND (BULLISH):
   - Price forms Higher Highs (HH) and Higher Lows (HL).
   - Green candles are larger, more frequent, and driving upward momentum.
   - Set trend: "Bullish", market_structure: "BULLISH_HH_HL".
2. DOWNTREND (BEARISH):
   - Price forms Lower Highs (LH) and Lower Lows (LL).
   - Red candles are larger, more frequent, and driving downward momentum.
   - Set trend: "Bearish", market_structure: "BEARISH_LH_LL".
3. SIDEWAYS / RANGE:
   - Price is contained between a clear horizontal Support floor and Resistance ceiling.
   - Set trend: "Sideways", market_structure: "RANGE_SIDEWAYS".

NOTE: At the bottom of Quotex mobile there are large green "Up" and red "Down" broker UI buttons. IGNORE THEM COMPLETELY. Look ONLY at the candlesticks on the chart grid.

STEP 2: PRICE ACTION & CANDLE CONFLUENCE (COMBINATION ANALYSIS):
Examine the last closed candle (far right of the chart):
- Color: GREEN (Bullish) vs RED (Bearish) vs DOJI.
- Body Size: Large momentum body, normal body, or indecision pin.
- Wick Rejection:
  * Long LOWER wick (>30% of candle): Strong buyer aggression defending support / floor.
  * Long UPPER wick (>30% of candle): Strong seller aggression defending resistance / ceiling.
- Support & Resistance (S/R) & Order Blocks:
  * Bullish Order Block (Demand) or Support floor: Look for bounce UP.
  * Bearish Order Block (Supply) or Resistance ceiling: Look for reject DOWN.

STEP 3: BALANCED 50/50 DECISION ENGINE:

A. FORECAST "UP" (CALL / Green candle expected - Confidence 78% to 94%):
Select "UP" when ANY of the following high-confluence bullish combinations appear:
1. BULLISH MOMENTUM CONTINUATION:
   - Trend is Bullish (or breaking out upward).
   - Last closed candle is a solid GREEN candle closing near its high with strong momentum.
   - Pattern: Bullish Engulfing or Marubozu continuation.
2. SUPPORT / DEMAND ZONE BOUNCE:
   - Price touches Support floor, Round Number (.000, .500, .100), or Bullish Order Block.
   - Candle shows long LOWER wick rejection or forms a Hammer / Morning Star.
3. OTC RED EXHAUSTION CANDLE TRAP:
   - Abnormally giant RED candle (2x-3x normal) crashing into support with zero/little lower wick. Retail panic sellers trapped; OTC algorithm snaps back UP.
   - Set otc_strategy: "OTC Exhaustion Candle Trap".
4. OTC SUPPORT FAKEOUT / SSL SWEEP TRAP:
   - Candle spikes below support to sweep sell stops (SSL), then closes back inside with a massive LOWER wick (>40%).
   - Set otc_strategy: "OTC Fakeout Wick Sweep Trap".
5. OTC 2-1-2 BULLISH PULLBACK CONTINUATION:
   - In an uptrend (2-3 green candles), exactly 1 weak small red pullback candle forms with lower wick. Next candle resumes green trend.
   - Set otc_strategy: "OTC 2-1-2 Trend Pullback Continuation".
6. OTC DOJI UPTREND REST:
   - In a strong uptrend, a Doji forms mid-run (algorithmic pause before next surge).
   - Set otc_strategy: "OTC Doji Trend Continuation".
-> advice: "ENTER NOW (CALL / UP)"

B. FORECAST "DOWN" (PUT / Red candle expected - Confidence 78% to 94%):
Select "DOWN" when ANY of the following high-confluence bearish combinations appear:
1. BEARISH MOMENTUM CONTINUATION:
   - Trend is Bearish (or breaking out downward).
   - Last closed candle is a solid RED candle closing near its low with strong momentum.
   - Pattern: Bearish Engulfing or Marubozu continuation.
2. RESISTANCE / SUPPLY ZONE REJECTION:
   - Price touches Resistance ceiling, Round Number (.000, .500, .100), or Bearish Order Block.
   - Candle shows long UPPER wick rejection or forms a Shooting Star / Evening Star.
3. OTC GREEN EXHAUSTION CANDLE TRAP:
   - Abnormally giant GREEN candle (2x-3x normal) surging into resistance with zero/little upper wick. Retail FOMO buyers trapped; OTC algorithm dumps back DOWN.
   - Set otc_strategy: "OTC Exhaustion Candle Trap".
4. OTC RESISTANCE FAKEOUT / BSL SWEEP TRAP:
   - Candle spikes above resistance to sweep buy stops (BSL), then closes back inside with a massive UPPER wick (>40%).
   - Set otc_strategy: "OTC Fakeout Wick Sweep Trap".
5. OTC 2-1-2 BEARISH PULLBACK CONTINUATION:
   - In a downtrend (2-3 red candles), exactly 1 weak small green pullback candle forms with upper wick. Next candle resumes red trend.
   - Set otc_strategy: "OTC 2-1-2 Trend Pullback Continuation".
6. OTC DOJI DOWNTREND REST:
   - In a strong downtrend, a Doji forms mid-run (algorithmic pause before next plunge).
   - Set otc_strategy: "OTC Doji Trend Continuation".
-> advice: "ENTER NOW (PUT / DOWN)"

C. FORECAST "UNCERTAIN" (WAIT / Low confidence - Confidence 45% to 65%):
- Market is compressed in tight, choppy sideways noise without clear direction.
- Conflicting signals (e.g., green candle with huge upper and lower wicks in the middle of nowhere).
-> advice: "WAIT FOR CLEAR SIGNAL"
-> primary_signal: "Market Choppy / Wait for Directional Confluence"

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
  "otc_strategy": "OTC Exhaustion Candle Trap" | "OTC Fakeout Wick Sweep Trap" | "OTC 2-1-2 Trend Pullback Continuation" | "OTC Round Number Level Rejection" | "OTC Doji Trend Continuation" | "None",
  "candle_pattern_found": "Hammer / Bullish Pin Bar" | "Shooting Star" | "Bullish Engulfing" | "Bearish Engulfing" | "Morning Star" | "Evening Star" | "Doji / Indecision" | "None",
  "sr_zone": "Support / Demand Zone" | "Resistance / Supply Zone" | "Round Number Level" | "None",
  "trend": "Bullish" | "Bearish" | "Sideways",
  "primary_signal": "Concise trigger summary (e.g. 'Bullish Momentum Continuation (CALL)' or 'Resistance Rejection with Bearish Engulfing (PUT)')",
  "confirmations": [
    "Observation 1 (structure & trend alignment)",
    "Observation 2 (candle color & wick rejection)",
    "Observation 3 (S/R, Order Block, or OTC trap confluence)"
  ],
  "prediction": "UP" | "DOWN" | "UNCERTAIN",
  "confidence": 50-95,
  "risk_level": "LOW" | "MEDIUM" | "HIGH",
  "advice": "ENTER NOW (CALL / UP)" | "ENTER NOW (PUT / DOWN)" | "WAIT FOR CLEAR SIGNAL"
}
""".trimIndent()
    }
}
