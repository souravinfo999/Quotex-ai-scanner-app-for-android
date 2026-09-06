package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.data.api.MistralApiClient
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
            // Save to database only if a genuine chart was detected
            if (!result.isNoChart) {
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
        val result = if (isBullish) {
            PredictionResult(
                prediction = "UP",
                confidence = 88,
                primarySignal = "Bullish Engulfing with Fair Value Gap (FVG) Fill",
                confirmations = listOf(
                    "Strong lower wick rejection at key Support zone",
                    "Bullish Fair Value Gap (FVG) filled and rejected upward",
                    "Uptrend intact (Consistent Higher Highs & Higher Lows)",
                    "Order Block reaction with momentum expansion"
                ),
                candlePatternFound = "Bullish Engulfing",
                srZone = "Key Horizontal Support (0.68500)",
                fvgDetected = true,
                trend = "Bullish",
                riskLevel = "LOW",
                advice = "ENTER NOW (Call / Up)",
                timestamp = System.currentTimeMillis(),
                isSample = true
            )
        } else {
            PredictionResult(
                prediction = "DOWN",
                confidence = 84,
                primarySignal = "Shooting Star Rejection at Round Number Resistance",
                confirmations = listOf(
                    "Prominent upper wick indicating strong seller rejection",
                    "Fakeout liquidity sweep above resistance level",
                    "Bearish Change of Character (CHoCH) structure shift",
                    "Momentum breakdown following consolidation"
                ),
                candlePatternFound = "Shooting Star / Pin Bar",
                srZone = "Major Resistance (1.09000)",
                fvgDetected = true,
                trend = "Bearish",
                riskLevel = "LOW",
                advice = "ENTER NOW (Put / Down)",
                timestamp = System.currentTimeMillis(),
                isSample = true
            )
        }
        saveScan(result)
        result
    }

    private fun encodeBitmapToBase64(bitmap: Bitmap): String {
        // Scale down if too large to ensure fast network upload & low token cost
        val maxDimension = 1024
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
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
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
