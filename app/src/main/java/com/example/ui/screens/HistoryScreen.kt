package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.engine.QuantSignalEngine
import com.example.data.model.PredictionResult
import com.example.ui.components.ExportTradeHistoryDialog
import com.example.ui.components.PredictionResultCard
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BearishRedDark
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.BullishGreenDark
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.ElectricGreenBorder
import com.example.ui.theme.ElectricGreenTransparent
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceBorderGlow
import com.example.ui.theme.SurfaceBorderSubtle
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfacePill
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UncertainYellow

@Composable
fun HistoryScreen(
    scans: List<PredictionResult>,
    onMarkOutcome: (Long, String) -> Unit,
    onClearHistory: () -> Unit,
    onLoadBenchmark: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf("LOGS") } // "LOGS" or "AUDIT"
    var selectedFilter by remember { mutableStateOf("ALL") }
    var showExportDialog by remember { mutableStateOf(false) }

    val filteredScans = when (selectedFilter) {
        "CALLS" -> scans.filter { it.isUp }
        "PUTS" -> scans.filter { it.isDown }
        "WINS" -> scans.filter { it.userOutcome == "WIN" }
        "LOSSES" -> scans.filter { it.userOutcome == "LOSS" }
        else -> scans
    }

    val totalScans = scans.size
    val upScans = scans.count { it.isUp }
    val downScans = scans.count { it.isDown }
    val wins = scans.count { it.userOutcome == "WIN" }
    val losses = scans.count { it.userOutcome == "LOSS" }
    val markedTrades = wins + losses
    val winRate = if (markedTrades > 0) ((wins.toFloat() / markedTrades) * 100).toInt() else 0

    val auditReport = remember(scans) {
        QuantSignalEngine.calculateHistoricalAudit(scans)
    }

    if (showExportDialog) {
        ExportTradeHistoryDialog(
            scans = scans,
            onDismiss = { showExportDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp)
    ) {
        // Professional Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Performance Ledger",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "QUANT ENGINE • SMC & OTC AUDIT",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryTeal,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Cyber Export Button
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricGreenTransparent)
                        .border(1.2.dp, ElectricGreenBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            if (scans.isNotEmpty()) {
                                showExportDialog = true
                            } else {
                                Toast.makeText(context, "No trade history to export", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(horizontal = 12.dp)
                        .testTag("export_history_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export Trade History",
                            tint = ElectricGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "EXPORT",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            color = ElectricGreen
                        )
                    }
                }

                if (scans.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BearishRed.copy(alpha = 0.15f))
                            .border(1.dp, BearishRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .clickable { onClearHistory() }
                            .testTag("clear_history_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear History",
                            tint = BearishRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Mode Navigation Bar: [SIGNALS LEDGER] vs [QUANT AUDIT & BACKTEST]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceCard)
                .border(1.dp, SurfaceBorderSubtle, RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selectedTab == "LOGS") PrimaryTeal.copy(alpha = 0.25f) else Color.Transparent)
                    .clickable { selectedTab = "LOGS" }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SIGNALS LEDGER (${scans.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = if (selectedTab == "LOGS") PrimaryTeal else TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selectedTab == "AUDIT") ElectricGreen.copy(alpha = 0.2f) else Color.Transparent)
                    .clickable { selectedTab = "AUDIT" }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "QUANT AUDIT & STATS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = if (selectedTab == "AUDIT") ElectricGreen else TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == "AUDIT") {
            // =========================================================================
            // QUANTITATIVE AUDIT & BACKTEST REPORT DASHBOARD (Requirements 18, 21, 22)
            // =========================================================================
            QuantAuditDashboardView(
                report = auditReport,
                onLoadBenchmark = onLoadBenchmark
            )
        } else {
            // =========================================================================
            // TRADE SIGNALS LOGS VIEW
            // =========================================================================
            // Performance Metrics Hero Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.2.dp, SurfaceBorderGlow, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(
                    modifier = Modifier
                        .background(Brush.verticalGradient(listOf(Color(0x2400E5B9), SurfaceCard)))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SESSION SUMMARY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "$markedTrades VERIFIED TRADES",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = PrimaryTeal
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatItem(label = "TOTAL SCANS", value = "$totalScans", color = TextPrimary)
                        StatDivider()
                        StatItem(label = "CALL (UP)", value = "$upScans", color = BullishGreen)
                        StatDivider()
                        StatItem(label = "PUT (DOWN)", value = "$downScans", color = BearishRed)
                        StatDivider()
                        StatItem(
                            label = "WIN RATE",
                            value = if (markedTrades > 0) "$winRate%" else "—",
                            color = if (winRate >= 70) BullishGreen else PrimaryTeal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterBadge(
                    text = "ALL (${scans.size})",
                    isSelected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" }
                )
                FilterBadge(
                    text = "CALLS ($upScans)",
                    isSelected = selectedFilter == "CALLS",
                    color = BullishGreen,
                    onClick = { selectedFilter = "CALLS" }
                )
                FilterBadge(
                    text = "PUTS ($downScans)",
                    isSelected = selectedFilter == "PUTS",
                    color = BearishRed,
                    onClick = { selectedFilter = "PUTS" }
                )
                FilterBadge(
                    text = "WINS ($wins)",
                    isSelected = selectedFilter == "WINS",
                    color = ElectricGreen,
                    onClick = { selectedFilter = "WINS" }
                )
                FilterBadge(
                    text = "LOSSES ($losses)",
                    isSelected = selectedFilter == "LOSSES",
                    color = BearishRed,
                    onClick = { selectedFilter = "LOSSES" }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredScans.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(SurfaceElevated)
                                .border(1.dp, SurfaceBorderSubtle, RoundedCornerShape(22.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (scans.isEmpty()) "No Scans Recorded Yet" else "No matching signals found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = if (scans.isEmpty())
                                "Activate the floating overlay button to scan Quotex or binary broker charts in real-time."
                            else
                                "Try selecting a different filter above to view recorded signals.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp),
                            lineHeight = 16.sp
                        )

                        if (scans.isEmpty() && onLoadBenchmark != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = { onLoadBenchmark() },
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryTeal)
                            ) {
                                Text("LOAD 12-TRADE BENCHMARK AUDIT DATA", color = PrimaryTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("scans_list"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredScans, key = { it.id }) { scan ->
                        PredictionResultCard(
                            result = scan,
                            onMarkOutcome = { outcome ->
                                onMarkOutcome(scan.id, outcome)
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuantAuditDashboardView(
    report: QuantSignalEngine.QuantAuditReport,
    onLoadBenchmark: (() -> Unit)? = null
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Benchmark quick load button banner
        if (onLoadBenchmark != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x1F00E5B9))
                    .border(1.dp, PrimaryTeal.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .clickable { onLoadBenchmark() }
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📊 CSV AUDIT BENCHMARK DATASET",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = PrimaryTeal
                        )
                        Text(
                            text = "Load historical 12-trade benchmark (6 Win / 3 Loss / 3 Unverified) to audit scoring & calibration.",
                            fontSize = 11.sp,
                            color = TextPrimary.copy(alpha = 0.9f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryTeal)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("LOAD", fontSize = 10.sp, fontWeight = FontWeight.Black, color = BackgroundDark)
                    }
                }
            }
        }

        // 1. Audit KPI Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "HISTORICAL AUDIT STATS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = PrimaryTeal,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(label = "TOTAL SIGNALS", value = "${report.totalSignals}", color = TextPrimary)
                    StatDivider()
                    StatItem(label = "VERIFIED", value = "${report.verifiedSignals}", color = TextSecondary)
                    StatDivider()
                    StatItem(label = "WINS / LOSSES", value = "${report.wins} / ${report.losses}", color = BullishGreen)
                    StatDivider()
                    StatItem(
                        label = "WIN RATE",
                        value = if (report.verifiedSignals > 0) String.format("%.1f%%", report.winRatePct) else "—",
                        color = if (report.winRatePct >= 70) BullishGreen else UncertainYellow
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Score Calibration: Win vs Loss Score comparison
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (report.avgWinScore > 0) String.format("%.0f/100", report.avgWinScore) else "—",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = BullishGreen
                        )
                        Text("Avg Winning Score", fontSize = 9.sp, color = TextSecondary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (report.avgLossScore > 0) String.format("%.0f/100", report.avgLossScore) else "—",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = BearishRed
                        )
                        Text("Avg Losing Score", fontSize = 9.sp, color = TextSecondary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (report.avgSignalScore > 0) String.format("%.0f/100", report.avgSignalScore) else "—",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                        Text("Avg Signal Score", fontSize = 9.sp, color = TextSecondary)
                    }
                }
            }
        }

        // 2. Feature & Confluence Audit Breakdown (Requirement 18 & 21)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "FEATURE-LEVEL WIN RATE PERFORMANCE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = PrimaryTeal,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                AuditFeatureRow(
                    label = "CALL (UP) Signals",
                    rate = report.callWinRate,
                    detail = "${report.callCount} verified trades"
                )
                AuditFeatureRow(
                    label = "PUT (DOWN) Signals",
                    rate = report.putWinRate,
                    detail = "${report.putCount} verified trades"
                )
                AuditFeatureRow(
                    label = "With-Trend Alignment",
                    rate = report.withTrendWinRate,
                    detail = "Trading in direction of market order flow"
                )
                AuditFeatureRow(
                    label = "Counter-Trend Setups",
                    rate = report.counterTrendWinRate,
                    detail = "Reversals against prevailing momentum"
                )
                AuditFeatureRow(
                    label = "Confirmed Liquidity Sweep (SSL/BSL)",
                    rate = report.sweepConfirmedWinRate,
                    detail = "Institutional high/low grab before reaction"
                )
                AuditFeatureRow(
                    label = "No Liquidity Sweep",
                    rate = report.noSweepWinRate,
                    detail = "Entries without prior liquidity grab"
                )
                AuditFeatureRow(
                    label = "Grade A & A+ Setups (Score 80–100)",
                    rate = report.gradeAWinRate,
                    detail = "High confluence institutional grade"
                )
                AuditFeatureRow(
                    label = "Grade B Setups (Score 70–79)",
                    rate = report.gradeBWinRate,
                    detail = "Moderate confluence setups"
                )
            }
        }

        // 3. False Signal Diagnosis Section (Requirement 22)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = BearishRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FALSE SIGNAL ANALYSIS (WHY DID TRADES FAIL?)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = BearishRed,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                if (report.topFailureReasons.isEmpty()) {
                    Text(
                        text = "No losing trades recorded in this session. All verified signals won or no losses tagged.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                } else {
                    report.topFailureReasons.forEach { (reason, count) ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.3f))
                                .border(1.dp, BearishRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = reason,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary.copy(alpha = 0.9f),
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$count ${if (count == 1) "trade" else "trades"}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = BearishRed
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AuditFeatureRow(label: String, rate: Float, detail: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = detail,
                    fontSize = 9.sp,
                    color = TextSecondary
                )
            }
            Text(
                text = if (rate > 0f) String.format("%.1f%%", rate) else "0.0%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = if (rate >= 70f) BullishGreen else if (rate >= 50f) UncertainYellow else BearishRed
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (rate / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = if (rate >= 70f) BullishGreen else if (rate >= 50f) UncertainYellow else BearishRed,
            trackColor = Color.Black.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = color
        )
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(28.dp)
            .background(SurfaceBorderSubtle)
    )
}

@Composable
private fun FilterBadge(
    text: String,
    isSelected: Boolean,
    color: Color = PrimaryTeal,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) color.copy(alpha = 0.2f) else SurfacePill)
            .border(
                1.dp,
                if (isSelected) color else SurfaceBorderSubtle,
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp,
            color = if (isSelected) color else TextSecondary
        )
    }
}
