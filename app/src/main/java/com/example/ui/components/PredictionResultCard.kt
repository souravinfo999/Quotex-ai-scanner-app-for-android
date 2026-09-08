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

@Composable
fun PredictionResultCard(
    result: PredictionResult,
    modifier: Modifier = Modifier,
    onMarkOutcome: ((String) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }

    val (badgeColor, titleText, gradientBg) = when {
        result.isNoChart -> Triple(
            UncertainYellow,
            "📊 NO CHART FOUND",
            Brush.verticalGradient(listOf(Color(0xFFFF8F00).copy(alpha = 0.15f), SurfaceCard))
        )
        result.isUp -> Triple(
            BullishGreen,
            "📈 NEXT: UP",
            Brush.verticalGradient(listOf(Color(0xFF00C853).copy(alpha = 0.12f), SurfaceCard))
        )
        result.isDown -> Triple(
            BearishRed,
            "📉 NEXT: DOWN",
            Brush.verticalGradient(listOf(Color(0xFFFF3D00).copy(alpha = 0.12f), SurfaceCard))
        )
        else -> Triple(
            UncertainYellow,
            "⚠️ NEXT: UNCERTAIN",
            Brush.verticalGradient(listOf(Color(0xFFFFB300).copy(alpha = 0.12f), SurfaceCard))
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(18.dp))
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
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AI SIGNAL",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    val scanNum = (result.timestamp % 9000 + 1000)
                    Text(
                        text = "#SCAN-$scanNum",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }

                val formattedTime = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    .format(Date(result.timestamp))
                Text(
                    text = formattedTime,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Large Title Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = titleText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = (-0.5).sp,
                    color = badgeColor
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${result.confidence}%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Accuracy Confidence Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ACCURACY CONFIDENCE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { result.confidence / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = badgeColor,
                trackColor = Color.Black.copy(alpha = 0.4f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Primary Signal Box: bg-black/20 p-3 rounded-xl border border-white/5
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
                    .border(1.dp, SurfaceBorderSubtle, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "🎯 Primary Signal: ${result.primarySignal}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    lineHeight = 16.sp
                )
            }

            if (result.hasOtcTrap) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BullishGreen.copy(alpha = 0.12f))
                        .border(1.dp, BullishGreen.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "⚡ OTC STRATEGY: ${result.otcPatternTrap}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BullishGreen
                    )
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
                                OutlinedButton(
                                    onClick = { onMarkOutcome("WIN") },
                                    modifier = Modifier.height(28.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("WIN +92%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BullishGreen)
                                }
                                OutlinedButton(
                                    onClick = { onMarkOutcome("LOSS") },
                                    modifier = Modifier.height(28.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("LOSS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BearishRed)
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
