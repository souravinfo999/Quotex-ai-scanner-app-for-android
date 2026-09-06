package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.example.data.model.PredictionResult
import com.example.data.repository.ScannerRepository
import com.example.ui.overlay.FloatingButtonView
import com.example.ui.overlay.PredictionPopupView
import com.example.ui.overlay.ScanningLaserOverlayView
import com.example.service.NotificationHelper
import com.example.utils.PreferenceManager
import com.example.utils.ScreenshotHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OverlayService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var windowManager: WindowManager
    private lateinit var repository: ScannerRepository
    private lateinit var preferenceManager: PreferenceManager

    private var floatingButton: FloatingButtonView? = null
    private var predictionPopup: PredictionPopupView? = null
    private var laserOverlay: ScanningLaserOverlayView? = null
    private var mediaProjection: MediaProjection? = null

    private var isAnalyzing = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            ScreenshotHelper.stopSession()
            mediaProjection = null
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        repository = ScannerRepository.getInstance(this)
        preferenceManager = PreferenceManager.getInstance(this)
        laserOverlay = ScanningLaserOverlayView(this)

        NotificationHelper.createNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == NotificationHelper.ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Start Foreground Service
        val notification = NotificationHelper.buildForegroundNotification(this)
        startForeground(NotificationHelper.NOTIFICATION_ID, notification)
        isRunning = true
        preferenceManager.setOverlayActive(true)

        // Initialize MediaProjection if data is available
        initMediaProjection()

        // Show Floating Button if overlay permission is granted
        if (Settings.canDrawOverlays(this)) {
            showFloatingButton()
        } else {
            Toast.makeText(this, "⚠️ Please allow 'Display over other apps' permission", Toast.LENGTH_LONG).show()
        }

        return START_STICKY
    }

    private fun initMediaProjection() {
        if (mediaProjection != null && ScreenshotHelper.isReady) return

        val data = mediaProjectionIntentData
        val code = mediaProjectionResultCode
        if (data != null && code != 0 && mediaProjection == null) {
            val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            try {
                val mp = mpManager.getMediaProjection(code, data)
                // Clear out stored intent so it is never reused
                mediaProjectionIntentData = null
                mediaProjectionResultCode = 0
                mediaProjection = mp
                if (mp != null) {
                    ScreenshotHelper.initSession(this, mp)
                }
            } catch (e: Exception) {
                Log.e("OverlayService", "Failed to init MediaProjection: ${e.message}")
            }
        }
    }

    private fun showFloatingButton() {
        if (floatingButton != null) return

        val buttonSize = (64 * resources.displayMetrics.density).toInt()
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            buttonSize,
            buttonSize,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (resources.displayMetrics.widthPixels - buttonSize - 24)
            y = (resources.displayMetrics.heightPixels / 3)
        }

        val btn = FloatingButtonView(
            context = this,
            windowManager = windowManager,
            layoutParams = params,
            onClick = { triggerScan() },
            onLongClick = {
                Toast.makeText(this, "Quotex AI Scanner Active", Toast.LENGTH_SHORT).show()
            }
        )

        try {
            windowManager.addView(btn, params)
            floatingButton = btn
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot show overlay: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun triggerScan() {
        if (isAnalyzing) return

        val settings = repository.getSettings()
        predictionPopup?.let { removePredictionPopup() }
        isAnalyzing = true

        serviceScope.launch {
            try {
                // Apply configured Scan Delay
                if (settings.scanDelayMs > 0) {
                    delay(settings.scanDelayMs)
                }

                // Ensure capture session is initialized
                if (!ScreenshotHelper.isReady && mediaProjection != null) {
                    ScreenshotHelper.initSession(this@OverlayService, mediaProjection!!)
                }

                if (ScreenshotHelper.isReady) {
                    // Temporarily hide floating button to capture 100% clean, unobstructed chart
                    withContext(Dispatchers.Main) {
                        floatingButton?.visibility = View.INVISIBLE
                        laserOverlay?.dismiss()
                    }
                    delay(80) // Allow window compositor to flush the frame

                    val captureResult = ScreenshotHelper.captureScreen(this@OverlayService)

                    // Immediately restore floating button and activate scanning HUD animation
                    withContext(Dispatchers.Main) {
                        floatingButton?.visibility = View.VISIBLE
                        floatingButton?.setScanningState(true)
                        laserOverlay?.show()
                    }

                    if (captureResult.isSuccess) {
                        val bitmap = captureResult.getOrThrow()
                        val analysisResult = repository.analyzeBitmap(bitmap)

                        withContext(Dispatchers.Main) {
                            floatingButton?.setScanningState(false)
                            laserOverlay?.dismiss()
                            isAnalyzing = false
                            ScreenshotHelper.cleanTempScreenshots(this@OverlayService)

                            if (analysisResult.isSuccess) {
                                showPredictionPopup(analysisResult.getOrThrow())
                            } else {
                                val errorMsg = analysisResult.exceptionOrNull()?.message ?: "Analysis failed"
                                Toast.makeText(this@OverlayService, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            floatingButton?.visibility = View.VISIBLE
                            floatingButton?.setScanningState(false)
                            laserOverlay?.dismiss()
                            isAnalyzing = false
                            val errorMsg = captureResult.exceptionOrNull()?.message ?: "📸 Screenshot capture failed. Try again."
                            Toast.makeText(this@OverlayService, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        floatingButton?.visibility = View.VISIBLE
                        floatingButton?.setScanningState(false)
                        laserOverlay?.dismiss()
                        isAnalyzing = false
                        Toast.makeText(this@OverlayService, "📸 Screen capture session ended. Open Quotex AI Pro app to restart.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    floatingButton?.visibility = View.VISIBLE
                    floatingButton?.setScanningState(false)
                    laserOverlay?.dismiss()
                    isAnalyzing = false
                    Toast.makeText(this@OverlayService, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showPredictionPopup(result: PredictionResult) {
        removePredictionPopup()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val popupParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val popup = PredictionPopupView(this) {
            removePredictionPopup()
        }
        popup.showResult(result)

        try {
            windowManager.addView(popup, popupParams)
            predictionPopup = popup
        } catch (e: Exception) {
            Toast.makeText(this, "Prediction: ${result.prediction} (${result.confidence}%)", Toast.LENGTH_LONG).show()
        }
    }

    private fun removePredictionPopup() {
        predictionPopup?.let {
            try {
                windowManager.removeView(it)
            } catch (ignored: Exception) {
            }
            predictionPopup = null
        }
    }

    private fun removeFloatingButton() {
        floatingButton?.let {
            try {
                windowManager.removeView(it)
            } catch (ignored: Exception) {
            }
            floatingButton = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        preferenceManager.setOverlayActive(false)
        laserOverlay?.dismiss()
        removePredictionPopup()
        removeFloatingButton()

        ScreenshotHelper.stopSession()

        try {
            mediaProjection?.stop()
            mediaProjection = null
        } catch (ignored: Exception) {
        }

        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        var isRunning: Boolean = false
        var mediaProjectionIntentData: Intent? = null
        var mediaProjectionResultCode: Int = 0

        fun start(context: Context, resultCode: Int = 0, data: Intent? = null) {
            if (resultCode != 0 && data != null) {
                mediaProjectionResultCode = resultCode
                mediaProjectionIntentData = data
            }

            val intent = Intent(context, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = NotificationHelper.ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
