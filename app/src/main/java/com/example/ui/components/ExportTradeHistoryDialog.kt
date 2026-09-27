package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.PredictionResult
import com.example.ui.theme.BackgroundDark
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.utils.TradeExportHelper

@Composable
fun ExportTradeHistoryDialog(
    scans: List<PredictionResult>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf(TradeExportHelper.ExportFormat.CSV) }

    // Pre-calculate export text depending on selected format
    val exportContent = remember(scans, selectedFormat) {
        if (selectedFormat == TradeExportHelper.ExportFormat.CSV) {
            TradeExportHelper.generateCsv(scans)
        } else {
            TradeExportHelper.generateTxtReport(scans)
        }
    }

    val defaultFileName = remember(selectedFormat) {
        TradeExportHelper.getDefaultFileName(selectedFormat)
    }

    // SAF Document Launchers
    val csvSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            val success = TradeExportHelper.writeToUri(context, uri, exportContent)
            if (success) {
                Toast.makeText(context, "CSV file saved successfully to device!", Toast.LENGTH_LONG).show()
                onDismiss()
            } else {
                Toast.makeText(context, "Failed to write CSV file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val txtSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            val success = TradeExportHelper.writeToUri(context, uri, exportContent)
            if (success) {
                Toast.makeText(context, "TXT report saved successfully to device!", Toast.LENGTH_LONG).show()
                onDismiss()
            } else {
                Toast.makeText(context, "Failed to write report file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val totalTrades = scans.size
    val wins = scans.count { it.userOutcome == "WIN" }
    val losses = scans.count { it.userOutcome == "LOSS" }
    val winRate = if (wins + losses > 0) ((wins.toFloat() / (wins + losses)) * 100).toInt() else 0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.5.dp, SurfaceBorderGlow, RoundedCornerShape(24.dp))
                .testTag("export_dialog_card"),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                PrimaryTeal.copy(alpha = 0.12f),
                                SurfaceCard,
                                BackgroundDark
                            )
                        )
                    )
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with Cyber Glow Icon and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(ElectricGreenTransparent)
                                .border(1.2.dp, ElectricGreenBorder, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = ElectricGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Export History",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                letterSpacing = (-0.3).sp
                            )
                            Text(
                                text = "$totalTrades RECORDS • AUDIT READY",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryTeal,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceElevated)
                            .testTag("close_export_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Format Toggle (CSV vs TXT)
                Text(
                    text = "SELECT EXPORT FORMAT",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // CSV Option Card
                    FormatCard(
                        modifier = Modifier.weight(1f),
                        title = "CSV FILE",
                        subtitle = "Excel & Sheets",
                        extension = ".csv",
                        icon = Icons.Default.TableChart,
                        isSelected = selectedFormat == TradeExportHelper.ExportFormat.CSV,
                        onClick = { selectedFormat = TradeExportHelper.ExportFormat.CSV },
                        testTag = "format_csv_button"
                    )

                    // TXT Option Card
                    FormatCard(
                        modifier = Modifier.weight(1f),
                        title = "TEXT REPORT",
                        subtitle = "Detailed Audit Log",
                        extension = ".txt",
                        icon = Icons.Default.Description,
                        isSelected = selectedFormat == TradeExportHelper.ExportFormat.TXT,
                        onClick = { selectedFormat = TradeExportHelper.ExportFormat.TXT },
                        testTag = "format_txt_button"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Summary Badge & Details Included Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, SurfaceBorderSubtle, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "FILE: $defaultFileName",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ElectricGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Includes: Timestamp, Signal (Call/Put), Confidence %, Verified Outcome, S&R Zones, Order Blocks, FVGs, Liquidity Sweeps, OTC Traps, and Advice.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )

                        if (wins + losses > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Session Win Rate: $winRate% ($wins W - $losses L)",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryTeal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Live Preview Terminal Box
                Text(
                    text = "DATA PREVIEW",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BackgroundDark)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    val previewSnippet = remember(exportContent) {
                        exportContent.lines().take(7).joinToString("\n")
                    }
                    Text(
                        text = previewSnippet,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF88F8D0),
                        lineHeight = 14.sp,
                        maxLines = 7,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Action: Download / Save to Device
                ElevatedButton(
                    onClick = {
                        if (selectedFormat == TradeExportHelper.ExportFormat.CSV) {
                            csvSaveLauncher.launch(defaultFileName)
                        } else {
                            txtSaveLauncher.launch(defaultFileName)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("download_save_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = ElectricGreen,
                        contentColor = BackgroundDark
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SAVE / DOWNLOAD FILE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary Action Row: Share via Apps + Copy to Clipboard
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Share Button
                    OutlinedButton(
                        onClick = {
                            TradeExportHelper.shareContent(context, exportContent, selectedFormat)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("share_export_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PrimaryTeal
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(PrimaryTeal, ElectricGreen))
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SHARE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Copy Button
                    OutlinedButton(
                        onClick = {
                            TradeExportHelper.copyToClipboard(context, exportContent, selectedFormat)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("copy_export_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextPrimary
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(SurfaceBorder, SurfaceBorder))
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "COPY",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    extension: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val borderColor = if (isSelected) ElectricGreen else SurfaceBorderSubtle
    val bgColor = if (isSelected) ElectricGreenTransparent else SurfacePill

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.2.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(12.dp)
            .testTag(testTag)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) ElectricGreen else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )

                Text(
                    text = extension,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = if (isSelected) ElectricGreen else TextMuted
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = if (isSelected) Color.White else TextPrimary
            )

            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
