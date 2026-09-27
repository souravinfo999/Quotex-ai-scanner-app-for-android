package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.PredictionResult
import com.example.data.model.ScanSettings
import com.example.ui.components.PredictionResultCard
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BearishRedDark
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.BullishGreenDark
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.ElectricGreenBorder
import com.example.ui.theme.ElectricGreenGlow
import com.example.ui.theme.ElectricGreenTransparent
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.PrimaryTealAlpha20
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceBorderGlow
import com.example.ui.theme.SurfaceBorderSubtle
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfacePill
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UncertainYellow
import com.example.viewmodel.AnalysisUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    isServiceRunning: Boolean,
    settings: ScanSettings,
    analysisState: AnalysisUiState,
    onToggleService: () -> Unit,
    onPickImage: () -> Unit,
    onTestSample: (Boolean) -> Unit,
    onMarkOutcome: (Long, String) -> Unit,
    onDismissAnalysis: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Smooth pulse animation for live engine status
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Professional Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .border(1.8.dp, if (isServiceRunning) ElectricGreen else ElectricGreenBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_logo),
                        contentDescription = "Quotex AI Pro Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Quotex AI Pro",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(PrimaryTeal.copy(alpha = 0.2f))
                                .border(0.8.dp, PrimaryTeal.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "v3.0",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryTeal
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .alpha(if (isServiceRunning) pulseAlpha else 1f)
                                .background(
                                    if (isServiceRunning) BullishGreen else PrimaryTeal,
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isServiceRunning) "MISTRAL-VISION • LIVE SCANNING" else "MISTRAL-VISION • ENGINE READY",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isServiceRunning) BullishGreen else TextSecondary,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }

            // Quick Settings Action Button
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ElectricGreenTransparent)
                    .border(1.dp, ElectricGreenBorder, RoundedCornerShape(14.dp))
                    .clickable { onNavigateToSettings() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Settings",
                    tint = ElectricGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Institutional Engine Telemetry Grid (Balanced 2x2 Layout - Clean, Symmetrical & Fully Visible)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryTile(
                    icon = Icons.Default.Bolt,
                    label = "AI ENGINE",
                    value = "OTC v3.0",
                    accentColor = ElectricGreen,
                    modifier = Modifier.weight(1f)
                )
                TelemetryTile(
                    icon = Icons.Default.Verified,
                    label = "SCORE THRESHOLD",
                    value = "≥${settings.confidenceThreshold}/100",
                    accentColor = BullishGreen,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryTile(
                    icon = Icons.Default.Speed,
                    label = "INTERVAL",
                    value = "${settings.scanDelayMs}ms",
                    accentColor = AccentPurple,
                    modifier = Modifier.weight(1f)
                )
                TelemetryTile(
                    icon = Icons.Default.Balance,
                    label = "SMC BIAS",
                    value = "NEUTRAL 50/50",
                    accentColor = UncertainYellow,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 1. Floating Overlay Scanner Master Control Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .border(
                    1.2.dp,
                    if (isServiceRunning) ElectricGreenBorder else ElectricGreenGlow,
                    RoundedCornerShape(22.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(
                modifier = Modifier
                    .background(
                        if (isServiceRunning) Brush.verticalGradient(listOf(Color(0x3300F576), SurfaceCard))
                        else Brush.verticalGradient(listOf(Color(0x2E00F576), SurfaceCard))
                    )
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .alpha(if (isServiceRunning) pulseAlpha else 1f)
                                    .background(if (isServiceRunning) BullishGreen else TextSecondary, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isServiceRunning) "FLOATING OVERLAY ACTIVE" else "FLOATING SCANNER IDLE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = if (isServiceRunning) BullishGreen else TextSecondary,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Text(
                            text = if (isServiceRunning) "Floating 64dp AI scanner is displayed over your broker" else "Displays 64dp floating AI button on top of Quotex / Pocket Option",
                            fontSize = 12.sp,
                            color = TextPrimary.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onToggleService,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("toggle_service_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isServiceRunning) BearishRed else ElectricGreenTransparent
                    ),
                    border = if (isServiceRunning) BorderStroke(1.2.dp, BearishRedDark) else BorderStroke(1.2.dp, ElectricGreenBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = if (isServiceRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isServiceRunning) Color.White else ElectricGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isServiceRunning) "STOP FLOATING SCANNER" else "LAUNCH FLOATING SCANNER (64dp)",
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp,
                        color = if (isServiceRunning) Color.White else ElectricGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Active OTC Trading Strategies Strip
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, SurfaceBorderSubtle, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE ALGO STRATEGIES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "QUOTEX OTC OPTIMIZED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StrategyChip(text = "⚡ OTC Exhaustion Trap")
                    StrategyChip(text = "🎯 Wick Sweep Fakeout")
                    StrategyChip(text = "🔄 2-1-2 Candle Pullback")
                    StrategyChip(text = "🛡️ Order Block & S/R Bounce")
                    StrategyChip(text = "⏸️ Doji Rest Continuation")
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Live Analysis Progress or Result Output
        when (analysisState) {
            is AnalysisUiState.Analyzing -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.2.dp, PrimaryTeal, RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = PrimaryTeal,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Mistral Vision Analyzing Chart...",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Evaluating candlesticks, wick rejections, FVG & OTC traps",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
            is AnalysisUiState.Success -> {
                Column(modifier = Modifier.padding(bottom = 14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "LATEST SIGNAL RESULT",
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = PrimaryTeal,
                                fontSize = 11.sp,
                                letterSpacing = 0.8.sp
                            )
                        }
                        OutlinedButton(
                            onClick = onDismissAnalysis,
                            modifier = Modifier.height(28.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Dismiss", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    PredictionResultCard(
                        result = analysisState.result,
                        onMarkOutcome = { outcome ->
                            onMarkOutcome(analysisState.result.id, outcome)
                        }
                    )
                }
            }
            is AnalysisUiState.Error -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, BearishRed, RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Analysis Failed",
                            fontWeight = FontWeight.Bold,
                            color = BearishRed,
                            fontSize = 13.sp
                        )
                        Text(
                            text = analysisState.message,
                            fontSize = 11.sp,
                            color = TextPrimary,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        )
                        OutlinedButton(
                            onClick = onDismissAnalysis,
                            modifier = Modifier.height(28.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Dismiss", fontSize = 10.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
            else -> {}
        }

        // 4. Model Configuration & Parameters Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, SurfaceBorderSubtle, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "AI ENGINE PARAMETERS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Mistral Vision API",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (settings.apiKey.isNotBlank()) "Key Configured • Ready" else "Requires API Key in Settings",
                            fontSize = 11.sp,
                            color = if (settings.apiKey.isNotBlank()) PrimaryTeal else UncertainYellow
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (settings.apiKey.isNotBlank()) BullishGreen.copy(alpha = 0.15f) else UncertainYellow.copy(alpha = 0.15f))
                            .border(
                                1.dp,
                                if (settings.apiKey.isNotBlank()) BullishGreen.copy(alpha = 0.4f) else UncertainYellow.copy(alpha = 0.4f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (settings.apiKey.isNotBlank()) "CONNECTED" else "SETUP NEEDED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (settings.apiKey.isNotBlank()) BullishGreen else UncertainYellow
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SurfaceBorderSubtle))
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Min Confidence",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${settings.confidenceThreshold}%",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "threshold",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Scan Interval",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${settings.scanDelayMs}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ms delay",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Direct Test & Screenshot Scanner Section
        Text(
            text = "MANUAL TESTING & SCREENSHOT SCANNER",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = TextSecondary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, BullishGreen.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .clickable { onTestSample(true) }
                    .testTag("test_bullish_button"),
                colors = CardDefaults.cardColors(containerColor = BullishGreen.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = BullishGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("TEST CALL", color = BullishGreen, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text("Bullish Setup", color = TextSecondary, fontSize = 9.sp)
                    }
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, BearishRed.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .clickable { onTestSample(false) }
                    .testTag("test_bearish_button"),
                colors = CardDefaults.cardColors(containerColor = BearishRed.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = BearishRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("TEST PUT", color = BearishRed, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text("Bearish Setup", color = TextSecondary, fontSize = 9.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, ElectricGreenTransparent, RoundedCornerShape(14.dp))
                .clickable { onPickImage() }
                .testTag("upload_screenshot_button"),
            colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElectricGreenTransparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = ElectricGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Analyze Chart from Gallery",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Instant 1-min candlestick & OTC trap recognition",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }

                Text(
                    text = "SELECT 📸",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = ElectricGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 6. Recent Market History Strip
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, SurfaceBorderSubtle, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard.copy(alpha = 0.7f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "SAMPLE SIGNALS SIMULATION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                LiveHistoryItem("EUR/USD OTC", "WIN +92%", isWin = true)
                Spacer(modifier = Modifier.height(8.dp))
                LiveHistoryItem("GBP/JPY OTC", "WIN +85%", isWin = true)
                Spacer(modifier = Modifier.height(8.dp))
                LiveHistoryItem("AUD/CAD OTC", "WIN +90%", isWin = true)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun TelemetryTile(
    icon: ImageVector,
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, SurfaceBorderSubtle, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.12f))
                    .border(1.dp, accentColor.copy(alpha = 0.32f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.width(9.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.6.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
                Text(
                    text = value,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.3.sp,
                    color = accentColor,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun StrategyChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .border(1.dp, SurfaceBorderSubtle, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

@Composable
private fun LiveHistoryItem(pair: String, resultText: String, isWin: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.25f))
            .border(1.dp, SurfaceBorderSubtle, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(if (isWin) BullishGreenDark else BearishRed, CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = pair,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }

        Text(
            text = resultText,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isWin) BullishGreen else BearishRed
        )
    }
}
