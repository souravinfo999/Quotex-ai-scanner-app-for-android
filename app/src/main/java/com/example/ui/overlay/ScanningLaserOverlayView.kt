package com.example.ui.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator

@SuppressLint("ViewConstructor")
class ScanningLaserOverlayView(context: Context) : View(context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val density = context.resources.displayMetrics.density
    private var scanProgress = 0.2f // 0f to 1f
    private var laserAnimator: ValueAnimator? = null
    var isAttached = false
        private set

    private val laserLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00D4AA")
        strokeWidth = 3f * density
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val laserGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val hudBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E60A0A0F")
        style = Paint.Style.FILL
    }

    private val hudBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8000D4AA")
        strokeWidth = 1.2f * density
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00D4AA")
        textSize = 13f * density
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        letterSpacing = 0.05f
    }

    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 10f * density
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.08f
    }

    private val hudRect = RectF()

    fun show() {
        if (isAttached) return

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        try {
            windowManager.addView(this, params)
            isAttached = true

            laserAnimator?.cancel()
            laserAnimator = ValueAnimator.ofFloat(0.18f, 0.82f).apply {
                duration = 1100
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener {
                    scanProgress = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } catch (ignored: Exception) {
        }
    }

    fun dismiss() {
        laserAnimator?.cancel()
        laserAnimator = null
        if (isAttached) {
            try {
                windowManager.removeView(this)
            } catch (ignored: Exception) {}
            isAttached = false
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val currentY = h * scanProgress

        // 1. Draw glowing laser curtain above beam
        val glowHeight = 60f * density
        val glowShader = LinearGradient(
            0f, currentY - glowHeight,
            0f, currentY,
            Color.TRANSPARENT,
            Color.argb(70, 0, 212, 170),
            Shader.TileMode.CLAMP
        )
        laserGlowPaint.shader = glowShader
        canvas.drawRect(0f, currentY - glowHeight, w, currentY, laserGlowPaint)

        // 2. Draw sharp laser scanning line
        canvas.drawLine(0f, currentY, w, currentY, laserLinePaint)

        // 3. Draw HUD Badge at the top
        val badgeW = 260f * density
        val badgeH = 52f * density
        val badgeLeft = (w - badgeW) / 2f
        val badgeTop = 75f * density

        hudRect.set(badgeLeft, badgeTop, badgeLeft + badgeW, badgeTop + badgeH)
        canvas.drawRoundRect(hudRect, 16f * density, 16f * density, hudBgPaint)
        canvas.drawRoundRect(hudRect, 16f * density, 16f * density, hudBorderPaint)

        // Text inside HUD badge
        canvas.drawText("⚡ MISTRAL VISION AI SCANNING", w / 2f, badgeTop + 24f * density, textPaint)
        canvas.drawText("READING CANDLESTICK PATTERN & SMC...", w / 2f, badgeTop + 40f * density, subTextPaint)
    }
}
