package com.example.ui.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.CountDownTimer
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.example.data.model.PredictionResult

@SuppressLint("ViewConstructor")
class PredictionPopupView(
    context: Context,
    private val onDismiss: () -> Unit
) : FrameLayout(context) {

    private var countdownTimer: CountDownTimer? = null
    private val containerLayout: LinearLayout

    init {
        // Darkened backdrop with subtle blur feel
        setBackgroundColor(Color.parseColor("#99000000"))
        setOnClickListener { dismissWithAnimation() }

        // Main Card Container
        containerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dpToPx(20)
            setPadding(pad, pad, pad, pad)
            setOnClickListener { /* prevent dismissal when clicking inside card */ }
        }

        val cardParams = LayoutParams(
            dpToPx(350),
            LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
            marginStart = dpToPx(16)
            marginEnd = dpToPx(16)
        }

        addView(containerLayout, cardParams)
    }

    fun showResult(result: PredictionResult) {
        containerLayout.removeAllViews()

        // Exact Professional Polish gradients
        val (bgStart, bgEnd, icon, titleText, pulseColor) = when {
            result.isNoChart -> Quintuple("#FF8F00", "#FF6F00", "📊❌", "NO CHART FOUND", "#FF6F00")
            result.isUp -> Quintuple("#00C853", "#00E676", "📈", "NEXT: UP", "#00E676")
            result.isDown -> Quintuple("#FF3D00", "#FF6E40", "📉", "NEXT: DOWN", "#FF3D00")
            else -> Quintuple("#FFB300", "#FFD54F", "⚠️", "NEXT: UNCERTAIN", "#FFB300")
        }

        // Card Container Background (rounded-3xl 24dp, border white/20)
        val cardDrawable = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.parseColor(bgStart), Color.parseColor(bgEnd))
        ).apply {
            cornerRadius = dpToPx(24).toFloat()
            setStroke(dpToPx(1), Color.parseColor("#33FFFFFF"))
        }
        containerLayout.background = cardDrawable

        // 1. Header with "AI SIGNAL #SCAN-XXXX" and Close Button "✕"
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
        }

        val titleColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        // Tag row
        val tagRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val aiSignalBadge = TextView(context).apply {
            text = if (result.isNoChart) "CHART NOTICE" else "AI SIGNAL"
            textSize = 9f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            val pX = dpToPx(6)
            val pY = dpToPx(2)
            setPadding(pX, pY, pX, pY)
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(4).toFloat()
                setColor(Color.BLACK)
            }
        }
        tagRow.addView(aiSignalBadge)

        val scanIdTv = TextView(context).apply {
            val scanIdNum = (result.timestamp % 9000 + 1000)
            text = " #SCAN-$scanIdNum"
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#CC000000"))
            setPadding(dpToPx(4), 0, 0, 0)
        }
        tagRow.addView(scanIdTv)

        val timerTv = TextView(context).apply {
            text = if (result.isNoChart) " • 6s" else " • 5s"
            textSize = 10f
            typeface = Typeface.MONOSPACE
            setTextColor(Color.parseColor("#AA000000"))
        }
        tagRow.addView(timerTv)

        titleColumn.addView(tagRow)

        // Large 3xl font-black italic tracking-tighter title
        val titleTv = TextView(context).apply {
            text = if (result.isNoChart) "📊 NO CHART FOUND" else "$icon $titleText"
            textSize = if (result.isNoChart) 23f else 28f
            setTypeface(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
            setTextColor(Color.BLACK)
            setPadding(0, dpToPx(4), 0, 0)
        }
        titleColumn.addView(titleTv)

        headerRow.addView(titleColumn)

        // Circular Close Button: bg-black/10 w-8 h-8 rounded-full
        val closeBtn = TextView(context).apply {
            text = "✕"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            val size = dpToPx(32)
            layoutParams = LinearLayout.LayoutParams(size, size)
            background = GradientDrawable().apply {
                cornerRadius = size / 2f
                setColor(Color.parseColor("#26000000"))
            }
            setOnClickListener { dismissWithAnimation() }
        }
        headerRow.addView(closeBtn)
        containerLayout.addView(headerRow)

        addSpace(12)

        // 2. Accuracy Confidence Bar
        val confRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val confLabel = TextView(context).apply {
            text = if (result.isNoChart) "CHART STATUS" else "ACCURACY CONFIDENCE"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.BLACK)
            letterSpacing = 0.05f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val confBadge = TextView(context).apply {
            text = if (result.isNoChart) "NOT FOUND" else "${result.confidence}%"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.BLACK)
            val pX = dpToPx(6)
            val pY = dpToPx(2)
            setPadding(pX, pY, pX, pY)
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(4).toFloat()
                setColor(Color.parseColor("#33000000"))
            }
        }
        confRow.addView(confLabel)
        confRow.addView(confBadge)
        containerLayout.addView(confRow)

        // Confidence progress track (h-3 bg-black/10 rounded-full p-0.5 with black fill)
        if (!result.isNoChart) {
            val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100
                progress = result.confidence
                val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(10)).apply {
                    topMargin = dpToPx(4)
                }
                layoutParams = lp
                progressDrawable = GradientDrawable().apply {
                    cornerRadius = dpToPx(5).toFloat()
                    setColor(Color.BLACK)
                }
                background = GradientDrawable().apply {
                    cornerRadius = dpToPx(5).toFloat()
                    setColor(Color.parseColor("#26000000"))
                }
            }
            containerLayout.addView(progressBar)
        }

        addSpace(12)

        // 3. Primary Signal Box: bg-black/5 rounded-xl p-3 border border-black/10
        val signalBox = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dpToPx(12)
            setPadding(pad, pad, pad, pad)
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(12).toFloat()
                setColor(Color.parseColor("#15000000"))
                setStroke(dpToPx(1), Color.parseColor("#26000000"))
            }
        }
        val signalTv = TextView(context).apply {
            text = if (result.isNoChart) {
                "⚠️ Chart nothing founded!\n\nPlease open Quotex or your trading candlestick chart screen and try again."
            } else {
                "🎯 Primary Signal: ${result.primarySignal}"
            }
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.BLACK)
            setLineSpacing(0f, 1.2f)
        }
        signalBox.addView(signalTv)
        containerLayout.addView(signalBox)

        addSpace(10)

        // 4. Badges Grid: 2 columns
        val gridLayout = GridLayout(context).apply {
            columnCount = 2
            alignmentMode = GridLayout.ALIGN_BOUNDS
        }

        val badgeItems = if (result.isNoChart) {
            listOf(
                "❌ No Candles Found",
                "📱 Non-Trading Screen",
                "🎯 Open Quotex / OTC",
                "🔄 Ready to Re-scan"
            )
        } else {
            val items = mutableListOf<String>()
            if (result.fvgDetected) items.add("FVG Detected")
            items.add(if (result.isUp) "HH-HL Trend" else if (result.isDown) "LH-LL Trend" else "Sideways")
            items.add(if (result.candlePatternFound != "None") result.candlePatternFound else "Wick Reject")
            items.add("${result.riskLevel} Risk")
            items
        }

        for (item in badgeItems.take(4)) {
            val badgeView = TextView(context).apply {
                val prefix = if (item.startsWith("❌") || item.startsWith("📱") || item.startsWith("🎯") || item.startsWith("🔄")) "" else "✅ "
                text = "$prefix$item"
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.BLACK)
                val pad = dpToPx(8)
                setPadding(pad, pad, pad, pad)
                background = GradientDrawable().apply {
                    cornerRadius = dpToPx(8).toFloat()
                    setColor(Color.parseColor("#1A000000"))
                }
                val lp = GridLayout.LayoutParams().apply {
                    width = 0
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))
                }
                layoutParams = lp
            }
            gridLayout.addView(badgeView)
        }
        containerLayout.addView(gridLayout)

        addSpace(14)

        // 5. Action Button: w-full bg-black text-white py-4 rounded-2xl font-black text-sm shadow-xl
        val actionButton = TextView(context).apply {
            val actionText = if (result.isNoChart) {
                "⚠️ OPEN CHART & TRY AGAIN"
            } else if (result.isUp) {
                "ENTER NOW (CALL / UP)"
            } else if (result.isDown) {
                "ENTER NOW (PUT / DOWN)"
            } else {
                "AVOID / WAIT FOR SIGNAL"
            }
            text = "● $actionText"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            val pad = dpToPx(14)
            setPadding(pad, pad, pad, pad)
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(16).toFloat()
                setColor(Color.BLACK)
            }
            setOnClickListener { dismissWithAnimation() }
        }
        containerLayout.addView(actionButton)

        // Countdown timer (6s for no-chart, 5s for trading signals)
        val countdownMillis = if (result.isNoChart) 6000L else 5000L
        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(countdownMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val sec = (millisUntilFinished / 1000) + 1
                timerTv.text = " • ${sec}s"
            }

            override fun onFinish() {
                dismissWithAnimation()
            }
        }.start()

        // Entry animation
        alpha = 0f
        containerLayout.scaleX = 0.9f
        containerLayout.scaleY = 0.9f
        animate().alpha(1f).setDuration(220).start()
        containerLayout.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(220)
            .start()
    }

    private fun addSpace(dp: Int) {
        val space = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(dp))
        }
        containerLayout.addView(space)
    }

    fun dismissWithAnimation() {
        countdownTimer?.cancel()
        countdownTimer = null
        animate()
            .alpha(0f)
            .setDuration(180)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onDismiss()
                }
            })
            .start()
        containerLayout.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(180)
            .start()
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private data class Quintuple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
}
