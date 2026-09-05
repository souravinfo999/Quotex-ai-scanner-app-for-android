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
                put("temperature", 0.15)
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
You are an elite binary options trading analyst with 10+ years of experience. Analyze the provided chart screenshot and predict the NEXT candle direction.

ANALYZE THE FOLLOWING ELEMENTS CAREFULLY:

1. CANDLESTICK PATTERNS:
   - Bullish: Hammer, Morning Star, Bullish Engulfing, Piercing Line, Three White Soldiers, Bullish Harami
   - Bearish: Shooting Star, Evening Star, Bearish Engulfing, Dark Cloud Cover, Three Black Crows, Bearish Harami, Hanging Man
   - Doji patterns: Dragonfly, Gravestone, Long-Legged

2. SUPPORT & RESISTANCE LEVELS:
   - Key horizontal S/R zones identify করো
   - Price কোন জোনে আছে (support এ bounce নাকি resistance এ reject হচ্ছে)
   - Round numbers (00, 50) এবং psychological levels

3. TREND ANALYSIS:
   - Overall trend direction (Uptrend/Downtrend/Sideways)
   - Higher Highs & Higher Lows (Bullish)
   - Lower Highs & Lower Lows (Bearish)
   - Trendline break/bounce scenarios

4. FAIR VALUE GAP (FVG):
   - Bullish FVG: Gap between previous candle high and next candle low (price often fills and bounces)
   - Bearish FVG: Gap between previous candle low and next candle high (price often fills and drops)
   - FVG fill probability check করো

5. CANDLE WICK ANALYSIS:
   - Upper wick = rejection from high (Bearish sign)
   - Lower wick = rejection from low (Bullish sign)
   - Wick-to-body ratio analysis
   - Long wick at S/R = strong reversal signal

6. QUOTEX OTC PATTERNS:
   - OTC market-specific volatility patterns
   - Fakeout detection (wick beyond S/R but close inside)
   - OTC session timing patterns

7. PRICE ACTION CONFIRMATIONS:
   - Break and Retest pattern
   - Liquidity sweep (stop hunt) detection
   - Order block identification
   - Change of Character (CHoCH) / Break of Structure (BOS)

8. MOMENTUM & VOLUME CLUES (visible from chart):
   - Momentum candle size comparison
   - Consolidation vs expansion phase
   - Volatility increase/decrease

DECISION LOGIC:
- যদি ৪টার বেশি bullish signal থাকে → UP
- যদি ৪টার বেশি bearish signal থাকে → DOWN
- যদি mixed signal থাকে → NEUTRAL/UNCERTAIN
- FVG + S/R bounce combination = HIGH CONFIDENCE

RESPONSE FORMAT (STRICT JSON ONLY):
{
  "prediction": "UP" | "DOWN" | "UNCERTAIN",
  "confidence": 0-100,
  "primary_signal": "main reason",
  "confirmations": [
    "confirmation 1",
    "confirmation 2",
    "confirmation 3"
  ],
  "candle_pattern_found": "pattern name or none",
  "sr_zone": "support/resistance/none",
  "fvg_detected": true/false,
  "trend": "bullish/bearish/sideways",
  "risk_level": "LOW/MEDIUM/HIGH",
  "advice": "Enter now / Wait / Avoid"
}
""".trimIndent()
    }
}
