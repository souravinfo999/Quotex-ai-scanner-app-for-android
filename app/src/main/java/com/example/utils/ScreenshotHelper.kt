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
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

object ScreenshotHelper {

    private const val TAG = "ScreenshotHelper"

    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var handlerThread: HandlerThread? = null
    private var isSessionActive = false
    private var activeMediaProjection: MediaProjection? = null
    private var screenWidth = 0
    private var screenHeight = 0

    private val lock = Any()
    private var isFrameRequested = false
    private var frameContinuation: CancellableContinuation<Bitmap?>? = null

    val isReady: Boolean
        get() = isSessionActive && virtualDisplay != null

    @Synchronized
    fun initSession(context: Context, mediaProjection: MediaProjection): Boolean {
        if (isSessionActive && activeMediaProjection == mediaProjection && virtualDisplay != null) {
            return true
        }
        stopSession()

        try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)

            screenWidth = metrics.widthPixels
            screenHeight = metrics.heightPixels
            val density = metrics.densityDpi

            val thread = HandlerThread("QuotexScreenCaptureThread").apply { start() }
            handlerThread = thread
            val handler = Handler(thread.looper)

            val reader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
            imageReader = reader

            // Android 14+ requirement: Register callback before createVirtualDisplay
            val callback = object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.d(TAG, "MediaProjection session stopped by system")
                    stopSession()
                }
            }
            try {
                mediaProjection.registerCallback(callback, handler)
            } catch (e: Exception) {
                Log.w(TAG, "Callback register warning: ${e.message}")
            }

            virtualDisplay = mediaProjection.createVirtualDisplay(
                "QuotexScreenCapture",
                screenWidth,
                screenHeight,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                handler
            )

            reader.setOnImageAvailableListener({ ir ->
                try {
                    val image = ir.acquireLatestImage() ?: return@setOnImageAvailableListener
                    try {
                        val cont = synchronized(lock) {
                            if (isFrameRequested) {
                                isFrameRequested = false
                                val c = frameContinuation
                                frameContinuation = null
                                c
                            } else {
                                null
                            }
                        }

                        if (cont != null && cont.isActive) {
                            val bitmap = convertImageToBitmap(image, screenWidth, screenHeight)
                            cont.resume(bitmap)
                        }
                    } finally {
                        image.close()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in onImageAvailable: ${e.message}")
                }
            }, handler)

            activeMediaProjection = mediaProjection
            isSessionActive = true
            Log.d(TAG, "Screen capture session initialized successfully (${screenWidth}x${screenHeight})")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize capture session: ${e.message}", e)
            stopSession()
            return false
        }
    }

    suspend fun captureScreen(context: Context): Result<Bitmap> = withContext(Dispatchers.Default) {
        val reader = imageReader
        if (!isSessionActive || reader == null) {
            return@withContext Result.failure(
                Exception("Screen capture session ended. Open app to restart floating scanner.")
            )
        }

        // Fast path: try acquiring latest image directly
        try {
            val directImage = reader.acquireLatestImage()
            if (directImage != null) {
                try {
                    val directBitmap = convertImageToBitmap(directImage, screenWidth, screenHeight)
                    if (directBitmap != null) {
                        saveTempScreenshot(context, directBitmap)
                        return@withContext Result.success(directBitmap)
                    }
                } finally {
                    directImage.close()
                }
            }
        } catch (ignored: Exception) {}

        // Awaiting next frame from the ongoing VirtualDisplay stream
        val capturedBitmap = withTimeoutOrNull(2500) {
            suspendCancellableCoroutine<Bitmap?> { cont ->
                synchronized(lock) {
                    isFrameRequested = true
                    frameContinuation = cont
                }
                cont.invokeOnCancellation {
                    synchronized(lock) {
                        isFrameRequested = false
                        frameContinuation = null
                    }
                }
            }
        }

        if (capturedBitmap != null) {
            saveTempScreenshot(context, capturedBitmap)
            Result.success(capturedBitmap)
        } else {
            Result.failure(Exception("Screenshot capture timed out. Please try again."))
        }
    }

    @Synchronized
    fun stopSession() {
        isSessionActive = false
        synchronized(lock) {
            try {
                frameContinuation?.cancel()
            } catch (ignored: Exception) {}
            frameContinuation = null
            isFrameRequested = false
        }

        try {
            virtualDisplay?.release()
        } catch (ignored: Exception) {}
        virtualDisplay = null

        try {
            imageReader?.close()
        } catch (ignored: Exception) {}
        imageReader = null

        try {
            handlerThread?.quitSafely()
        } catch (ignored: Exception) {}
        handlerThread = null

        activeMediaProjection = null
        Log.d(TAG, "Screen capture session stopped")
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
