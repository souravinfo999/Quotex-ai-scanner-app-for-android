package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileOutputStream

object ScreenshotHelper {

    suspend fun captureScreen(
        context: Context,
        mediaProjection: MediaProjection
    ): Result<Bitmap> = withContext(Dispatchers.Default) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)

        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        var virtualDisplay: VirtualDisplay? = null

        // Android 14+ (API 34+) strictly requires registering a MediaProjection.Callback
        // BEFORE calling createVirtualDisplay!
        val callback = object : MediaProjection.Callback() {
            override fun onStop() {
                // MediaProjection stopped
            }
        }
        val mainHandler = Handler(Looper.getMainLooper())

        try {
            try {
                mediaProjection.registerCallback(callback, mainHandler)
            } catch (ignored: Exception) {
            }

            virtualDisplay = mediaProjection.createVirtualDisplay(
                "QuotexScreenCapture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.surface,
                null,
                null
            )

            // Wait for first image frame with timeout of 2.5 seconds
            val bitmap = withTimeoutOrNull(2500) {
                var capturedBitmap: Bitmap? = null
                // Allow a brief settling moment for screen buffer
                delay(200)

                for (attempt in 1..15) {
                    val image = imageReader.acquireLatestImage()
                    if (image != null) {
                        try {
                            capturedBitmap = convertImageToBitmap(image, width, height)
                            if (capturedBitmap != null) {
                                break
                            }
                        } finally {
                            image.close()
                        }
                    }
                    delay(100)
                }
                capturedBitmap
            }

            if (bitmap != null) {
                // Save temp file in cache as required
                saveTempScreenshot(context, bitmap)
                Result.success(bitmap)
            } else {
                Result.failure(Exception("📸 Screenshot capture failed. Try again."))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Screenshot failed"))
        } finally {
            virtualDisplay?.release()
            imageReader.close()
        }
    }

    private fun convertImageToBitmap(image: Image, width: Int, height: Int): Bitmap? {
        val planes = image.planes
        if (planes.isEmpty()) return null

        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * width

        val bitmapWidth = width + (rowPadding / pixelStride)
        val bitmap = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)

        return if (bitmapWidth != width) {
            // Crop out stride padding
            val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)
            bitmap.recycle()
            cropped
        } else {
            bitmap
        }
    }

    private fun saveTempScreenshot(context: Context, bitmap: Bitmap): File? {
        return try {
            val file = File(context.cacheDir, "temp_chart_scan_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
            file
        } catch (e: Exception) {
            null
        }
    }

    fun cleanTempScreenshots(context: Context) {
        try {
            val cacheDir = context.cacheDir
            val files = cacheDir.listFiles { _, name -> name.startsWith("temp_chart_scan_") }
            files?.forEach { it.delete() }
        } catch (ignored: Exception) {
        }
    }
}
