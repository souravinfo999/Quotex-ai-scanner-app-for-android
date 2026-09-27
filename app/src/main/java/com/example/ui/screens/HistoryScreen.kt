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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.model.PredictionResult
import com.example.ui.components.ExportTradeHistoryDialog
import com.example.ui.components.PredictionResultCard
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("ALL") }
    var showExportDialog by remember { mutableStateOf(false) }

    val filteredScans = when (selectedFilter) {
        "CALLS" -> scans.filter { it.isUp }
        "PUTS" -> scans.filter { it.isDown }
        "WINS" -> scans.filter { it.userOutcome == "WIN" }
        else -> scans
    }

    val totalScans = scans.size
    val upScans = scans.count { it.isUp }
    val downScans = scans.count { it.isDown }
    val wins = scans.count { it.userOutcome == "WIN" }
    val losses = scans.count { it.userOutcome == "LOSS" }
    val markedTrades = wins + losses
    val winRate = if (markedTrades > 0) ((wins.toFloat() / markedTrades) * 100).toInt() else 0

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
                    text = "DATABASE LOGS • RECENT SCANS",
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

        Spacer(modifier = Modifier.height(14.dp))

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
                        text = "$markedTrades VERIFIED",
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

        Spacer(modifier = Modifier.height(14.dp))

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
        }

        Spacer(modifier = Modifier.height(14.dp))

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

