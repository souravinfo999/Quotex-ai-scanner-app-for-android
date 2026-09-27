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
     * Generates standard RFC 4180 CSV string representing all trades with full quant attributes.
     */
    fun generateCsv(scans: List<PredictionResult>): String {
        val sb = StringBuilder()

        // CSV Header
        val headers = listOf(
            "Trade_ID",
            "Timestamp_Millis",
            "Date_Time",
            "Signal",
            "Signal_Score",
            "Setup_Grade",
            "Setup_Recommendation",
            "Outcome",
            "Market_Trend",
            "Market_Structure",
            "Risk_Level",
            "System_Advice",
            "Primary_Signal_Reason",
            "Candle_Pattern",
            "SR_Zone",
            "FVG_Detected",
            "Order_Block_Zone",
            "Liquidity_Sweep",
            "OTC_Trap",
            "Failure_Reason",
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
                "${scan.effectiveScore}/100",
                scan.setupGrade,
                scan.setupRecommendation,
                outcomeStr,
                scan.trend,
                scan.marketStructure,
                scan.riskLevel,
                scan.advice,
                scan.primarySignal,
                scan.candlePatternFound,
                scan.srZone,
                if (scan.fvgDetected) "YES" else "NO",
                scan.orderBlockZone,
                scan.liquiditySweep,
                scan.otcPatternTrap,
                scan.failureReason ?: "None",
                confirmationsStr,
                if (scan.isChartDetected) "YES" else "NO"
            )
            sb.append(row.joinToString(",") { escapeCsv(it) }).append("\r\n")
        }

        return sb.toString()
    }

    /**
     * Parses an RFC 4180 CSV string back into PredictionResults for backtesting/auditing.
     */
    fun parseCsvToScans(csvText: String): List<PredictionResult> {
        val lines = csvText.lines().filter { it.isNotBlank() }
        if (lines.size <= 1) return emptyList()

        val results = mutableListOf<PredictionResult>()
        // Skip header line
        for (i in 1 until lines.size) {
            val line = lines[i]
            val cols = parseCsvLine(line)
            if (cols.size >= 5) {
                try {
                    val id = cols.getOrNull(0)?.toLongOrNull() ?: i.toLong()
                    val ts = cols.getOrNull(1)?.toLongOrNull() ?: System.currentTimeMillis()
                    val signal = cols.getOrNull(3)?.trim()?.uppercase(Locale.US) ?: "UNCERTAIN"
                    val scoreRaw = cols.getOrNull(4)?.replace("%", "")?.replace("/100", "")?.trim()?.toIntOrNull() ?: 70
                    val grade = cols.getOrNull(5)?.trim() ?: (if (scoreRaw >= 80) "A" else "B")
                    val recommendation = cols.getOrNull(6)?.trim() ?: "VALID SETUP"
                    val outcomeRaw = cols.getOrNull(7)?.trim()
                    val outcome = if (outcomeRaw == "WIN" || outcomeRaw == "LOSS") outcomeRaw else null
                    val trend = cols.getOrNull(8)?.trim() ?: "Sideways"
                    val structure = cols.getOrNull(9)?.trim() ?: "None"
                    val risk = cols.getOrNull(10)?.trim() ?: "LOW"
                    val advice = cols.getOrNull(11)?.trim() ?: "ENTER NOW"
                    val primary = cols.getOrNull(12)?.trim() ?: "SMC Price Action Confluence"
                    val pattern = cols.getOrNull(13)?.trim() ?: "None"
                    val sr = cols.getOrNull(14)?.trim() ?: "None"
                    val fvg = cols.getOrNull(15)?.equals("YES", ignoreCase = true) ?: false
                    val ob = cols.getOrNull(16)?.trim() ?: "None"
                    val sweep = cols.getOrNull(17)?.trim() ?: "None"
                    val otc = cols.getOrNull(18)?.trim() ?: "None"
                    val failure = cols.getOrNull(19)?.trim()?.let { if (it == "None") null else it }
                    val confs = cols.getOrNull(20)?.split(";")?.map { it.trim() } ?: emptyList()

                    results.add(
                        PredictionResult(
                            id = id,
                            prediction = signal,
                            confidence = scoreRaw,
                            primarySignal = primary,
                            confirmations = confs,
                            candlePatternFound = pattern,
                            srZone = sr,
                            fvgDetected = fvg,
                            orderBlockZone = ob,
                            liquiditySweep = sweep,
                            otcPatternTrap = otc,
                            trend = trend,
                            riskLevel = risk,
                            advice = advice,
                            timestamp = ts,
                            userOutcome = outcome,
                            signalScore = scoreRaw,
                            setupGrade = grade,
                            setupRecommendation = recommendation,
                            marketStructure = structure,
                            liquidityStatus = sweep,
                            failureReason = failure
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return results
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var curVal = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    curVal.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(curVal.toString().trim())
                curVal = StringBuilder()
            } else {
                curVal.append(c)
            }
            i++
        }
        result.add(curVal.toString().trim())
        return result
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
