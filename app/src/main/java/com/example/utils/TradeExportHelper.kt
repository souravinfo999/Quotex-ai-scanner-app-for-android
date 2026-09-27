package com.example.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.PredictionResult
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TradeExportHelper {

    enum class ExportFormat(val extension: String, val mimeType: String, val displayName: String) {
        CSV("csv", "text/csv", "CSV Spreadsheet (.csv)"),
        TXT("txt", "text/plain", "Detailed Text Report (.txt)")
    }

    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val fileTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    fun getDefaultFileName(format: ExportFormat): String {
        val timestamp = fileTimestampFormat.format(Date())
        return "quotex_trade_history_$timestamp.${format.extension}"
    }

    /**
     * Generates standard RFC 4180 CSV string representing all trades.
     */
    fun generateCsv(scans: List<PredictionResult>): String {
        val sb = StringBuilder()

        // CSV Header
        val headers = listOf(
            "Trade_ID",
            "Timestamp_Millis",
            "Date_Time",
            "Signal",
            "Confidence_Percent",
            "Outcome",
            "Market_Trend",
            "Risk_Level",
            "System_Advice",
            "Primary_Signal_Reason",
            "Candle_Pattern",
            "SR_Zone",
            "FVG_Detected",
            "Order_Block_Zone",
            "Liquidity_Sweep",
            "OTC_Trap",
            "Confirmations",
            "Chart_Detected"
        )
        sb.append(headers.joinToString(",") { escapeCsv(it) }).append("\r\n")

        // Rows
        for (scan in scans) {
            val formattedDate = dateTimeFormat.format(Date(scan.timestamp))
            val outcomeStr = scan.userOutcome ?: "UNVERIFIED"
            val confirmationsStr = if (scan.confirmations.isNotEmpty()) {
                scan.confirmations.joinToString("; ")
            } else {
                "None"
            }

            val row = listOf(
                scan.id.toString(),
                scan.timestamp.toString(),
                formattedDate,
                scan.prediction.uppercase(Locale.US),
                "${scan.confidence}%",
                outcomeStr,
                scan.trend,
                scan.riskLevel,
                scan.advice,
                scan.primarySignal,
                scan.candlePatternFound,
                scan.srZone,
                if (scan.fvgDetected) "YES" else "NO",
                scan.orderBlockZone,
                scan.liquiditySweep,
                scan.otcPatternTrap,
                confirmationsStr,
                if (scan.isChartDetected) "YES" else "NO"
            )
            sb.append(row.joinToString(",") { escapeCsv(it) }).append("\r\n")
        }

        return sb.toString()
    }

    /**
     * Generates a beautifully formatted terminal/audit TXT report.
     */
    fun generateTxtReport(scans: List<PredictionResult>): String {
        val sb = StringBuilder()
        val nowFormatted = dateTimeFormat.format(Date())

        val totalTrades = scans.size
        val upTrades = scans.count { it.isUp }
        val downTrades = scans.count { it.isDown }
        val uncertainTrades = scans.count { !it.isUp && !it.isDown }
        val wins = scans.count { it.userOutcome == "WIN" }
        val losses = scans.count { it.userOutcome == "LOSS" }
        val markedCount = wins + losses
        val winRate = if (markedCount > 0) ((wins.toFloat() / markedCount) * 100).toInt() else 0

        sb.append("========================================================================\n")
        sb.append("         QUOTEX AI PRO SCANNER — COMPREHENSIVE TRADE AUDIT LEDGER       \n")
        sb.append("========================================================================\n")
        sb.append("Export Timestamp : $nowFormatted\n")
        sb.append("Total Trades     : $totalTrades\n")
        sb.append("Signals Breakdown: CALL (UP): $upTrades | PUT (DOWN): $downTrades | UNCERTAIN: $uncertainTrades\n")
        sb.append("Session Results  : WINS: $wins | LOSSES: $losses | VERIFIED: $markedCount\n")
        sb.append("Win Rate         : ${if (markedCount > 0) "$winRate%" else "No verified trades yet"}\n")
        sb.append("========================================================================\n\n")

        if (scans.isEmpty()) {
            sb.append(">>> NO RECORDED TRADE SIGNALS FOUND IN DATABASE <<<\n")
            return sb.toString()
        }

        scans.forEachIndexed { index, scan ->
            val dateStr = dateTimeFormat.format(Date(scan.timestamp))
            val outcomeStr = scan.userOutcome ?: "UNVERIFIED"

            sb.append("------------------------------------------------------------------------\n")
            sb.append(String.format(Locale.US, "TRADE #%03d | ID: %d | Time: %s\n", index + 1, scan.id, dateStr))
            sb.append("------------------------------------------------------------------------\n")
            sb.append("• Prediction Signal : ${scan.prediction.uppercase(Locale.US)}\n")
            sb.append("• AI Confidence     : ${scan.confidence}%\n")
            sb.append("• Verified Outcome  : $outcomeStr\n")
            sb.append("• Market Trend      : ${scan.trend}\n")
            sb.append("• Risk Assessment   : ${scan.riskLevel}\n")
            sb.append("• Execution Advice  : ${scan.advice}\n")
            sb.append("• Primary Signal    : ${scan.primarySignal}\n")
            sb.append("• Price Action & SMC Breakdown:\n")
            sb.append("    - Candle Pattern: ${scan.candlePatternFound}\n")
            sb.append("    - S&R Key Zone  : ${scan.srZone}\n")
            sb.append("    - Fair Value Gap: ${if (scan.fvgDetected) "Detected (FVG Imbalance present)" else "None"}\n")
            sb.append("    - Order Block   : ${scan.orderBlockZone}\n")
            sb.append("    - Liquidity     : ${scan.liquiditySweep}\n")
            sb.append("    - OTC Trap Check: ${scan.otcPatternTrap}\n")

            if (scan.confirmations.isNotEmpty()) {
                sb.append("• Key Confirmations : ${scan.confirmations.joinToString(" | ")}\n")
            } else {
                sb.append("• Key Confirmations : Standard Multi-Timeframe Scan\n")
            }
            sb.append("\n")
        }

        sb.append("========================================================================\n")
        sb.append("                   END OF QUOTEX AI PRO AUDIT REPORT                    \n")
        sb.append("========================================================================\n")

        return sb.toString()
    }

    /**
     * Writes exported content to a SAF Uri obtained from CreateDocument.
     */
    fun writeToUri(context: Context, uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
                    writer.write(content)
                    writer.flush()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Shares exported text/csv content via Android Sharesheet.
     */
    fun shareContent(context: Context, content: String, format: ExportFormat) {
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_SUBJECT, "Quotex AI Trade History (${format.extension.uppercase()})")
                putExtra(Intent.EXTRA_TEXT, content)
                type = format.mimeType
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share Trade History via")
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Copies text to Android clipboard.
     */
    fun copyToClipboard(context: Context, content: String, format: ExportFormat) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Quotex Trade History", content)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "${format.displayName} copied to clipboard!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to copy: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun escapeCsv(value: String): String {
        var str = value
        val needsQuotes = str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")
        if (str.contains("\"")) {
            str = str.replace("\"", "\"\"")
        }
        return if (needsQuotes) "\"$str\"" else str
    }
}
