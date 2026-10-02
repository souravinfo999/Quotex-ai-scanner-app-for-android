package com.example.utils

import com.example.data.model.PredictionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Sends A/A+ grade trade signals to the user's Telegram channel/group
 * through their own Telegram bot (BotFather token + channel chat ID).
 */
object TelegramNotifier {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun formatSignalMessage(result: PredictionResult): String {
        val dirLine = if (result.isUp) "🟢 <b>CALL (UP)</b>" else "🔴 <b>PUT (DOWN)</b>"
        val sb = StringBuilder()
        sb.append("🤖 <b>Quotex AI Signal</b>\n")
        sb.append("$dirLine — Grade <b>${escape(result.setupGrade)}</b> (${result.effectiveScore}/100)\n\n")
        sb.append("🎯 ${escape(result.primarySignal)}\n")
        if (result.candlePatternFound != "None") {
            sb.append("🕯️ Pattern: ${escape(result.candlePatternFound)}\n")
        }
        sb.append("📍 Zone: ${escape(result.srZone)}\n")
        if (result.hasLiquiditySweep) sb.append("💧 ${escape(result.liquiditySweep)}\n")
        if (result.hasOrderBlock) sb.append("📦 ${escape(result.orderBlockZone)}\n")
        if (result.fvgDetected) sb.append("📐 FVG imbalance detected\n")
        if (result.hasOtcTrap) sb.append("⚡ ${escape(result.otcPatternTrap)}\n")
        sb.append("📊 Trend: ${escape(result.trend)} | Risk: ${escape(result.riskLevel)}\n")
        if (result.reasons.isNotEmpty()) {
            sb.append("\n✅ Confluences:\n")
            result.reasons.take(5).forEach { sb.append("• ${escape(it)}\n") }
        }
        if (result.warnings.isNotEmpty()) {
            sb.append("\n⚠️ ${escape(result.warnings.first())}\n")
        }
        val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        sb.append("\n⏰ ${timeFmt.format(Date(result.timestamp))} • ${escape(result.setupRecommendation)}")
        return sb.toString()
    }

    suspend fun sendSignal(
        botToken: String,
        chatId: String,
        result: PredictionResult
    ): Result<String> = withContext(Dispatchers.IO) {
        sendMessage(botToken, chatId, formatSignalMessage(result))
    }

    suspend fun testConnection(botToken: String, chatId: String): Result<String> =
        withContext(Dispatchers.IO) {
            sendMessage(
                botToken,
                chatId,
                "✅ <b>Quotex AI Scanner connected!</b>\nA/A+ grade signals will be posted here automatically."
            )
        }

    private fun sendMessage(botToken: String, chatId: String, htmlText: String): Result<String> {
        val token = botToken.trim()
        val target = chatId.trim()
        if (token.isBlank()) return Result.failure(IllegalArgumentException("❌ Please enter your Telegram bot token."))
        if (target.isBlank()) return Result.failure(IllegalArgumentException("❌ Please enter your Telegram channel/chat ID."))

        return try {
            val payload = JSONObject().apply {
                put("chat_id", target)
                put("text", htmlText)
                put("parse_mode", "HTML")
                put("disable_web_page_preview", true)
            }
            val request = Request.Builder()
                .url("https://api.telegram.org/bot$token/sendMessage")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.code == 200 && body.contains("\"ok\":true")) {
                    Result.success("✅ Sent to Telegram!")
                } else {
                    val desc = try {
                        JSONObject(body).optJSONObject("result")
                            ?.optString("description")
                            ?: JSONObject(body).optString("description", "Telegram error (${response.code})")
                    } catch (e: Exception) {
                        "Telegram error (${response.code})"
                    }
                    Result.failure(Exception("❌ $desc"))
                }
            }
        } catch (e: IOException) {
            Result.failure(Exception("📡 No Internet. Check connection and retry."))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Telegram send failed"))
        }
    }

    private fun escape(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
