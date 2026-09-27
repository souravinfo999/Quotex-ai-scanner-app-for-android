package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PredictionResult
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.BullishGreenDark
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceBorderSubtle
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UncertainYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class CardThemeData(
    val badgeColor: Color,
    val titleText: String,
    val actionSubtitle: String,
    val gradientBg: Brush,
    val cardBorderColor: Color
)

@Composable
fun PredictionResultCard(
    result: PredictionResult,
    modifier: Modifier = Modifier,
    onMarkOutcome: ((String) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }

    val theme = when {
        result.isNoChart -> CardThemeData(
            UncertainYellow,
            "NO CHART DETECTED",
            "Open Quotex / Broker Chart",
            Brush.verticalGradient(listOf(Color(0x33FFB800), SurfaceCard)),
            Color(0x4DFFB800)
        )
        result.isNoTrade -> CardThemeData(
            UncertainYellow,
            "NO TRADE / WAIT",
            "INSUFFICIENT CONFLUENCE • WAIT",
            Brush.verticalGradient(listOf(Color(0x29FFC01E), SurfaceCard)),
            Color(0x4DFFC01E)
        )
        result.isStrongUp -> CardThemeData(
            BullishGreen,
            "NEXT CANDLE: STRONG CALL (UP)",
            "HIGH CONFLUENCE • 1-MIN CALL",
            Brush.verticalGradient(listOf(Color(0x3800F576), SurfaceCard)),
            BullishGreen
        )
        result.isUp -> CardThemeData(
            BullishGreen,
            "NEXT CANDLE: CALL (UP)",
            "BUY 1-MIN CALL",
            Brush.verticalGradient(listOf(Color(0x2E00F576), SurfaceCard)),
            Color(0x5900F576)
        )
        result.isStrongDown -> CardThemeData(
            BearishRed,
            "NEXT CANDLE: STRONG PUT (DOWN)",
            "HIGH CONFLUENCE • 1-MIN PUT",
            Brush.verticalGradient(listOf(Color(0x38FF3838), SurfaceCard)),
            BearishRed
        )
        result.isDown -> CardThemeData(
            BearishRed,
            "NEXT CANDLE: PUT (DOWN)",
            "BUY 1-MIN PUT",
            Brush.verticalGradient(listOf(Color(0x2EFF3838), SurfaceCard)),
            Color(0x59FF3838)
        )
        else -> CardThemeData(
            UncertainYellow,
            "NO TRADE / WAIT",
            "WAIT FOR EDGE FORMATION",
            Brush.verticalGradient(listOf(Color(0x29FFC01E), SurfaceCard)),
            Color(0x4DFFC01E)
        )
    }

    val (badgeColor, titleText, actionSubtitle, gradientBg, cardBorderColor) = theme

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.2.dp, cardBorderColor, RoundedCornerShape(20.dp))
            .testTag("prediction_result_card_${result.id}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(
            modifier = Modifier
                .background(gradientBg)
                .padding(16.dp)
        ) {
            // Header Row: AI SIGNAL Tag & Monospace Scan ID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (result.isNoTrade) "WAIT EDGE" else "QUANT SIGNAL",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            color = badgeColor
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    val scanNum = (result.timestamp % 9000 + 1000)
                    Text(
                        text = "#SCAN-$scanNum",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(badgeColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val formattedTime = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        .format(Date(result.timestamp))
                    Text(
                        text = formattedTime,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Large Directional Action Hero Banner & Score Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titleText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = (-0.3).sp,
                        color = badgeColor
                    )
                    Text(
                        text = actionSubtitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = TextPrimary.copy(alpha = 0.9f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Score + Grade Badge (Score out of 100, not probability)
                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${result.effectiveScore}/100",
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                color = badgeColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "GRADE ${result.setupGrade}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                        }
                    }
                    Text(
                        text = result.setupRecommendation,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp, end = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Setup Confluence Progress Bar
            LinearProgressIndicator(
                progress = { (result.effectiveScore / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = badgeColor,
                trackColor = Color.Black.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Primary Signal Trigger Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .border(1.dp, SurfaceBorderSubtle, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🎯 PRIMARY TRIGGER",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.8.sp,
                            color = PrimaryTeal
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "1-MIN EXPIRY",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = result.primarySignal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        lineHeight = 18.sp
                    )
                }
            }

            // Confluences & Reasons Block (Requirement 15 & 24)
            if (result.reasons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "CONFLUENCES (REASONS):",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = PrimaryTeal,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    result.reasons.take(4).forEach { reason ->
                        val cleanText = if (reason.startsWith("✓")) reason else "✓ $reason"
                        Text(
                            text = cleanText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = BullishGreenDark.copy(alpha = 0.95f),
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }

            // Warnings Block (Requirement 15 & 24)
            if (result.warnings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x24FFB300))
                        .border(1.dp, Color(0x4DFFB300), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    result.warnings.forEach { warning ->
                        val cleanText = if (warning.startsWith("⚠")) warning else "⚠ $warning"
                        Text(
                            text = cleanText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFFD54F),
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }

            // Loss Root Cause Analysis Section (Requirement 22)
            if (result.userOutcome == "LOSS") {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BearishRed.copy(alpha = 0.12f))
                        .border(1.dp, BearishRed.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🔍 WHY DID THIS SIGNAL FAIL?",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = BearishRed,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        val diagText = result.failureReason ?: com.example.data.engine.QuantSignalEngine.diagnoseFailure(
                            prediction = result.prediction,
                            trend = result.trend,
                            liquiditySweep = result.liquiditySweep,
                            srZone = result.srZone,
                            fvgDetected = result.fvgDetected,
                            orderBlockZone = result.orderBlockZone,
                            candlePattern = result.candlePatternFound,
                            score = result.effectiveScore
                        )
                        Text(
                            text = diagText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            if (result.hasOtcTrap) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BullishGreen.copy(alpha = 0.14f))
                        .border(1.dp, BullishGreen.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "⚡",
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "QUOTEX OTC ALGO STRATEGY",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                color = BullishGreen
                            )
                            Text(
                                text = result.otcPatternTrap,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mini badges row (wrapping FlowRow so badges never get squished vertically)
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MiniBadge(text = "Grade: ${result.setupGrade}")
                if (result.marketStructure != "None" && result.marketStructure.isNotBlank()) {
                    MiniBadge(text = "Structure: ${result.marketStructure}")
                }
                if (result.hasOtcTrap) {
                    MiniBadge(text = "⚡ OTC Trap")
                }
                if (result.hasOrderBlock) {
                    MiniBadge(text = if (result.orderBlockZone.contains("Bullish")) "🛡️ Bullish OB" else "🛡️ Bearish OB")
                }
                if (result.fvgDetected) {
                    MiniBadge(text = "⚡ FVG")
                }
                if (result.hasLiquiditySweep) {
                    MiniBadge(text = if (result.liquiditySweep.contains("SSL")) "🎯 SSL Sweep" else "🎯 BSL Sweep")
                }
                MiniBadge(text = "Trend: ${result.trend}")
                MiniBadge(text = "Risk: ${result.riskLevel}")
            }

            // Expandable details (Confirmations & Outcome tracking)
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    if (result.candlePatternFound != "None" || result.srZone != "None") {
                        Text(
                            text = "Pattern: ${result.candlePatternFound} • Zone: ${result.srZone}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryTeal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (result.hasOrderBlock || result.hasLiquiditySweep) {
                        Text(
                            text = buildString {
                                if (result.hasOrderBlock) append("OB: ${result.orderBlockZone}  ")
                                if (result.hasLiquiditySweep) append("Sweep: ${result.liquiditySweep}")
                            },
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (result.confirmations.isNotEmpty()) {
                        Text(
                            text = "Institutional Confirmations:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        result.confirmations.forEach { conf ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = badgeColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = conf,
                                    fontSize = 11.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    if (onMarkOutcome != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.2f))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (result.userOutcome == "WIN") BullishGreenDark else if (result.userOutcome == "LOSS") BearishRed else TextSecondary,
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Outcome: ${result.userOutcome ?: "Pending"}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val isWin = result.userOutcome == "WIN"
                                val isLoss = result.userOutcome == "LOSS"

                                OutlinedButton(
                                    onClick = { onMarkOutcome("WIN") },
                                    modifier = Modifier.height(30.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isWin) BullishGreen else BullishGreen.copy(alpha = 0.4f)
                                    ),
                                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isWin) BullishGreen.copy(alpha = 0.25f) else Color.Transparent
                                    )
                                ) {
                                    Text(
                                        text = if (isWin) "✓ WIN +92%" else "WIN +92%",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BullishGreen
                                    )
                                }

                                OutlinedButton(
                                    onClick = { onMarkOutcome("LOSS") },
                                    modifier = Modifier.height(30.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isLoss) BearishRed else BearishRed.copy(alpha = 0.4f)
                                    ),
                                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isLoss) BearishRed.copy(alpha = 0.25f) else Color.Transparent
                                    )
                                ) {
                                    Text(
                                        text = if (isLoss) "✓ LOSS" else "LOSS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BearishRed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Expand / Collapse Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expanded) "Hide Details" else "View Confirmations & Analysis",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun MiniBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.25f))
            .border(1.dp, SurfaceBorderSubtle, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            maxLines = 1,
            softWrap = false
        )
    }
}
