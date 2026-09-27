package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.data.api.MistralApiClient
import com.example.data.engine.QuantSignalEngine
import com.example.data.local.AppDatabase
import com.example.data.local.ScanEntity
import com.example.data.model.PredictionResult
import com.example.data.model.ScanSettings
import com.example.utils.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ScannerRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = PreferenceManager.getInstance(appContext)
    private val db = AppDatabase.getDatabase(appContext)
    private val scanDao = db.scanDao()
    private val apiClient = MistralApiClient()

    private var lastProcessedScan: PredictionResult? = null

    val settingsFlow = prefs.settingsFlow

    fun getSettings(): ScanSettings = prefs.getSettings()

    fun saveSettings(
        apiKey: String,
        confidenceThreshold: Int,
        scanDelayMs: Long,
        analysisMode: String,
        preferredModel: String
    ) {
        prefs.saveSettings(apiKey, confidenceThreshold, scanDelayMs, analysisMode, preferredModel)
    }

    fun getRecentScans(): Flow<List<PredictionResult>> {
        return scanDao.getRecentScans().map { entities ->
            entities.map { it.toPredictionResult() }
        }
    }

    suspend fun saveScan(result: PredictionResult): Long = withContext(Dispatchers.IO) {
        scanDao.insertScan(ScanEntity.fromPredictionResult(result))
    }

    suspend fun loadAuditBenchmarkTrades(): Int = withContext(Dispatchers.IO) {
        val benchmarkScans = QuantSignalEngine.getHistoricalAuditBenchmark()
        scanDao.clearAll()
        val entities = benchmarkScans.map { ScanEntity.fromPredictionResult(it) }
        scanDao.insertScans(entities)
        benchmarkScans.size
    }

    suspend fun updateOutcome(scanId: Long, outcome: String) = withContext(Dispatchers.IO) {
        scanDao.updateOutcome(scanId, outcome)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        scanDao.clearAll()
    }

    suspend fun testConnection(apiKey: String): Result<String> {
        return apiClient.testConnection(apiKey)
    }

    suspend fun analyzeBitmap(bitmap: Bitmap): Result<PredictionResult> = withContext(Dispatchers.Default) {
        // Validate if blank or completely black
        if (isBitmapBlankOrBlack(bitmap)) {
            return@withContext Result.failure(Exception("🖤 Invalid screenshot. Chart not visible or screen locked."))
        }

        val base64 = encodeBitmapToBase64(bitmap)
        val settings = getSettings()

        val apiResult = apiClient.analyzeChart(base64, settings)
        if (apiResult.isSuccess) {
            val result = apiResult.getOrThrow()

            // Duplicate filter check (User requirement 17)
            if (!result.isNoChart && QuantSignalEngine.isDuplicateSignal(
                    lastScan = lastProcessedScan,
                    direction = result.prediction,
                    structure = result.marketStructure,
                    timestamp = result.timestamp
                )
            ) {
                // Return current active setup without spamming duplicate database records
                return@withContext Result.success(
                    result.copy(
                        advice = "SETUP ACTIVE: ${result.advice} (Waiting for next 1M candle open)"
                    )
                )
            }

            // Save to database only if a genuine chart was detected
            if (!result.isNoChart) {
                lastProcessedScan = result
                saveScan(result)
            }
            Result.success(result)
        } else {
            apiResult
        }
    }

    /**
     * Fallback/Test scan for local demonstration or offline testing
     */
    suspend fun runSampleAnalysis(isBullish: Boolean): PredictionResult = withContext(Dispatchers.Default) {
        val quantEval = if (isBullish) {
            QuantSignalEngine.evaluate(
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
        } else {
            QuantSignalEngine.evaluate(
                directionBias = "DOWN",
                marketStructure = "BEARISH_LH_LL",
                trend = "Bearish",
                srZone = "Major Resistance (1.08700)",
                liquiditySweep = "BSL Swept",
                fvgDetected = true,
                fvgFresh = true,
                orderBlockZone = "Bearish Order Block (Supply)",
                obFresh = true,
                candlePattern = "Shooting Star / Pin Bar",
                isDisplacementCandle = true,
                momentumStrong = true,
                otcPatternTrap = "OTC Fakeout Trap"
            )
        }

        val result = if (isBullish) {
            PredictionResult(
                prediction = "UP",
                confidence = quantEval.signalScore,
                signalScore = quantEval.signalScore,
                setupGrade = quantEval.setupGrade,
                setupRecommendation = quantEval.recommendation,
                primarySignal = "Bullish Order Block Mitigation + Sell-Side Liquidity Sweep",
                confirmations = listOf(
                    "Sell-Side Liquidity (SSL) swept below previous swing low",
                    "Price mitigated fresh Bullish Order Block at Support",
                    "Bullish market structure intact (Higher Highs & Higher Lows)",
                    "Bullish Engulfing displacement candle confirmed"
                ),
                candlePatternFound = "Bullish Engulfing",
                srZone = "Key Support (1.08200)",
                fvgDetected = true,
                orderBlockZone = "Bullish Order Block (Demand)",
                liquiditySweep = "SSL Swept",
                otcPatternTrap = "None",
                marketStructure = "BULLISH_HH_HL",
                trend = "Bullish",
                riskLevel = "LOW",
                advice = "ENTER NOW (CALL / UP)",
                timestamp = System.currentTimeMillis(),
                isSample = true,
                reasons = quantEval.reasons,
                warnings = quantEval.warnings
            )
        } else {
            PredictionResult(
                prediction = "DOWN",
                confidence = quantEval.signalScore,
                signalScore = quantEval.signalScore,
                setupGrade = quantEval.setupGrade,
                setupRecommendation = quantEval.recommendation,
                primarySignal = "Bearish Supply Rejection + Buy-Side Liquidity Grab",
                confirmations = listOf(
                    "Buy-Side Liquidity (BSL) swept above session resistance",
                    "Bearish Order Block defended with upper wick rejection",
                    "Bearish Change of Character (CHoCH) displacement",
                    "OTC liquidity trap confirmed against retail breakout"
                ),
                candlePatternFound = "Shooting Star / Pin Bar",
                srZone = "Major Resistance (1.08700)",
                fvgDetected = true,
                orderBlockZone = "Bearish Order Block (Supply)",
                liquiditySweep = "BSL Swept",
                otcPatternTrap = "OTC Fakeout Trap",
                marketStructure = "BEARISH_LH_LL",
                trend = "Bearish",
                riskLevel = "LOW",
                advice = "ENTER NOW (PUT / DOWN)",
                timestamp = System.currentTimeMillis(),
                isSample = true,
                reasons = quantEval.reasons,
                warnings = quantEval.warnings
            )
        }
        lastProcessedScan = result
        saveScan(result)
        result
    }

    private fun encodeBitmapToBase64(bitmap: Bitmap): String {
        // High resolution for clear candle wick and body detection
        val maxDimension = 1600
        val scaledBitmap = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
            val ratio = Math.min(
                maxDimension.toFloat() / bitmap.width,
                maxDimension.toFloat() / bitmap.height
            )
            val newWidth = (bitmap.width * ratio).toInt()
            val newHeight = (bitmap.height * ratio).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 92, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun isBitmapBlankOrBlack(bitmap: Bitmap): Boolean {
        if (bitmap.width <= 10 || bitmap.height <= 10) return true
        // Sample a few pixels across the image
        var darkPixelCount = 0
        val samplePoints = 20
        val stepX = bitmap.width / (samplePoints + 1)
        val stepY = bitmap.height / (samplePoints + 1)

        for (i in 1..samplePoints) {
            for (j in 1..samplePoints) {
                val pixel = bitmap.getPixel(i * stepX, j * stepY)
                val r = (pixel shr 16) and 0xff
                val g = (pixel shr 8) and 0xff
                val b = pixel and 0xff
                if (r < 10 && g < 10 && b < 10) {
                    darkPixelCount++
                }
            }
        }

        val totalSamples = samplePoints * samplePoints
        return darkPixelCount >= (totalSamples * 0.98)
    }

    companion object {
        @Volatile
        private var INSTANCE: ScannerRepository? = null

        fun getInstance(context: Context): ScannerRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ScannerRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
