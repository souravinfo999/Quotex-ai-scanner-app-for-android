package com.example.ui.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import com.example.R
import kotlin.math.hypot

@SuppressLint("ViewConstructor")
class FloatingButtonView(
    context: Context,
    private val windowManager: WindowManager,
    val layoutParams: WindowManager.LayoutParams,
    private val onClick: () -> Unit,
    private val onLongClick: () -> Unit
) : View(context) {

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private val touchSlop = 15

    private val handler = Handler(Looper.getMainLooper())
    private var isLongPressed = false
    private val longPressRunnable = Runnable {
        isLongPressed = true
        performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        onLongClick()
    }

    // Drawing Paints
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1.5f)
        color = Color.parseColor("#4DFFFFFF") // border-white/30
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = dpToPx(2.5f)
        color = Color.WHITE
    }
    private val fillIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = dpToPx(3.5f)
        color = Color.WHITE
    }

    private var isScanning = false
    private var scanRotation = 0f
    private var scanAnimator: ValueAnimator? = null
    private var touchScale = 1.0f

    // Cached Logo Bitmap
    private var logoBitmap: Bitmap? = null

    init {
        setWillNotDraw(false)
        try {
            logoBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_quotex_logo)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setScanningState(scanning: Boolean) {
        isScanning = scanning
        if (scanning) {
            scanAnimator?.cancel()
            scanAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
                duration = 900
                repeatCount = ValueAnimator.INFINITE
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    scanRotation = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else {
            scanAnimator?.cancel()
            scanRotation = 0f
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f

        canvas.save()
        canvas.scale(touchScale, touchScale, cx, cy)

        val pad = dpToPx(3f)
        val radius = (w / 2f) - pad

        // 1. Dark circular shadow base
        bgPaint.color = Color.BLACK
        canvas.drawCircle(cx, cy, radius, bgPaint)

        // 2. Draw user's round logo if available
        val bm = logoBitmap
        if (bm != null && !bm.isRecycled) {
            val srcRect = Rect(0, 0, bm.width, bm.height)
            val destRect = RectF(pad, pad, w - pad, h - pad)
            val path = android.graphics.Path().apply {
                addCircle(cx, cy, radius, android.graphics.Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(path)
            val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(bm, srcRect, destRect, bmpPaint)
            canvas.restore()
        } else {
            // Fallback: draw vector scanner icon
            drawScannerIcon(canvas, cx, cy)
        }

        // 3. Crisp Neon Green Border Rim (Round Logo Theme)
        borderPaint.color = Color.parseColor("#00E676")
        borderPaint.strokeWidth = dpToPx(2.2f)
        canvas.drawCircle(cx, cy, radius, borderPaint)

        // 4. Scanning rotating radar arc indicator
        if (isScanning) {
            val arcRect = RectF(pad + dpToPx(1f), pad + dpToPx(1f), w - pad - dpToPx(1f), h - pad - dpToPx(1f))
            progressPaint.color = Color.parseColor("#00E676")
            canvas.drawArc(arcRect, scanRotation, 110f, false, progressPaint)
            progressPaint.color = Color.parseColor("#00D4AA")
            canvas.drawArc(arcRect, scanRotation + 180f, 90f, false, progressPaint)
        }

        canvas.restore()
    }

    private fun drawScannerIcon(canvas: Canvas, cx: Float, cy: Float) {
        // Left Bullish Candle
        val candle1X = cx - dpToPx(7f)
        iconPaint.color = Color.parseColor("#00E676")
        fillIconPaint.color = Color.parseColor("#00E676")
        canvas.drawLine(candle1X, cy - dpToPx(11f), candle1X, cy + dpToPx(11f), iconPaint)
        canvas.drawRoundRect(
            RectF(candle1X - dpToPx(3f), cy - dpToPx(6f), candle1X + dpToPx(3f), cy + dpToPx(6f)),
            dpToPx(1.5f), dpToPx(1.5f), fillIconPaint
        )

        // Right Bearish Candle
        val candle2X = cx + dpToPx(7f)
        iconPaint.color = Color.parseColor("#FF4757")
        fillIconPaint.color = Color.parseColor("#FF4757")
        canvas.drawLine(candle2X, cy - dpToPx(13f), candle2X, cy + dpToPx(9f), iconPaint)
        canvas.drawRoundRect(
            RectF(candle2X - dpToPx(3f), cy - dpToPx(4f), candle2X + dpToPx(3f), cy + dpToPx(8f)),
            dpToPx(1.5f), dpToPx(1.5f), fillIconPaint
        )

        // Center AI reticle beam
        iconPaint.color = Color.WHITE
        iconPaint.strokeWidth = dpToPx(1.5f)
        canvas.drawLine(cx - dpToPx(12f), cy, cx + dpToPx(12f), cy, iconPaint)
        fillIconPaint.color = Color.WHITE
        canvas.drawCircle(cx, cy, dpToPx(2.5f), fillIconPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = layoutParams.x
                initialY = layoutParams.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                isLongPressed = false
                touchScale = 0.93f
                invalidate()
                handler.postDelayed(longPressRunnable, 600)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - initialTouchX).toInt()
                val dy = (event.rawY - initialTouchY).toInt()

                if (!isDragging && hypot(dx.toDouble(), dy.toDouble()) > touchSlop) {
                    isDragging = true
                    touchScale = 1.0f
                    handler.removeCallbacks(longPressRunnable)
                }

                if (isDragging) {
                    layoutParams.x = initialX + dx
                    layoutParams.y = initialY + dy
                    windowManager.updateViewLayout(this, layoutParams)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                touchScale = 1.0f
                invalidate()
                handler.removeCallbacks(longPressRunnable)
                if (!isDragging && !isLongPressed) {
                    onClick()
                } else if (isDragging) {
                    snapToEdge()
                }
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                touchScale = 1.0f
                invalidate()
                handler.removeCallbacks(longPressRunnable)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun snapToEdge() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val targetX = if (layoutParams.x + width / 2 < screenWidth / 2) {
            dpToPx(10f).toInt()
        } else {
            screenWidth - width - dpToPx(10f).toInt()
        }

        val startX = layoutParams.x
        val animator = ValueAnimator.ofInt(startX, targetX)
        animator.duration = 240
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener {
            layoutParams.x = it.animatedValue as Int
            try {
                windowManager.updateViewLayout(this, layoutParams)
            } catch (ignored: Exception) {
            }
        }
        animator.start()
    }

    private fun dpToPx(dp: Float): Float = dp * resources.displayMetrics.density
}
