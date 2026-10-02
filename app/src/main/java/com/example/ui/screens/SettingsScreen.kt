package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScanSettings
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.ElectricGreenBorder
import com.example.ui.theme.ElectricGreenTransparent
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.PrimaryTealAlpha20
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceBorderSubtle
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.TestConnectionState

@Composable
fun SettingsScreen(
    currentSettings: ScanSettings,
    testConnectionState: TestConnectionState,
    telegramTestState: TestConnectionState,
    onTestConnection: (String) -> Unit,
    onTestTelegram: (String, String) -> Unit,
    onSaveAndStart: (ScanSettings) -> Unit,
    onResetTestState: () -> Unit,
    onResetTelegramTestState: () -> Unit,
    modifier: Modifier = Modifier
) {
    var apiKey by remember(currentSettings.apiKey) { mutableStateOf(currentSettings.apiKey) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confidenceThreshold by remember(currentSettings.confidenceThreshold) {
        mutableFloatStateOf(currentSettings.confidenceThreshold.toFloat())
    }
    var scanDelayMs by remember(currentSettings.scanDelayMs) {
        mutableFloatStateOf(currentSettings.scanDelayMs.toFloat())
    }
    var analysisMode by remember(currentSettings.analysisMode) {
        mutableStateOf(currentSettings.analysisMode)
    }
    var telegramBotToken by remember(currentSettings.telegramBotToken) {
        mutableStateOf(currentSettings.telegramBotToken)
    }
    var telegramChatId by remember(currentSettings.telegramChatId) {
        mutableStateOf(currentSettings.telegramChatId)
    }
    var telegramEnabled by remember(currentSettings.telegramEnabled) {
        mutableStateOf(currentSettings.telegramEnabled)
    }
    var telegramTokenVisible by remember { mutableStateOf(false) }
    var cropTopPct by remember(currentSettings.cropTopPct) {
        mutableFloatStateOf(currentSettings.cropTopPct.toFloat())
    }
    var cropBottomPct by remember(currentSettings.cropBottomPct) {
        mutableFloatStateOf(currentSettings.cropBottomPct.toFloat())
    }
    var cropLeftPct by remember(currentSettings.cropLeftPct) {
        mutableFloatStateOf(currentSettings.cropLeftPct.toFloat())
    }
    var cropRightPct by remember(currentSettings.cropRightPct) {
        mutableFloatStateOf(currentSettings.cropRightPct.toFloat())
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Professional Header
        Text(
            text = "AI Scanner Engine Setup",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = (-0.5).sp
        )
        Text(
            text = "MISTRAL-VISION-PIXTRAL • PARAMETERS & OTC ENGINE",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryTeal,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
        )

        // 1. Mistral API Key Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "MISTRAL API AUTHENTICATION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Requires Mistral Vision key (Pixtral 12B). Get your API key at console.mistral.ai",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        onResetTestState()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input"),
                    placeholder = { Text("Enter Mistral API Key", color = TextSecondary, fontSize = 13.sp) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = TextSecondary
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryTeal,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = PrimaryTeal
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Test Connection Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onTestConnection(apiKey) },
                        enabled = apiKey.isNotBlank() && testConnectionState !is TestConnectionState.Loading,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (apiKey.isNotBlank()) ElectricGreenBorder else SurfaceBorderSubtle),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            containerColor = ElectricGreenTransparent
                        ),
                        modifier = Modifier.testTag("test_connection_button")
                    ) {
                        if (testConnectionState is TestConnectionState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = ElectricGreen
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Testing...", fontSize = 11.sp, color = ElectricGreen)
                        } else {
                            Text("Test Connection ⚡", color = ElectricGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Test Status Indicator
                    when (testConnectionState) {
                        is TestConnectionState.Success -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = BullishGreen,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Key Valid & Active", color = BullishGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        is TestConnectionState.Error -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = BearishRed,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = testConnectionState.error.take(24),
                                    color = BearishRed,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        else -> {}
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Algorithm Parameters Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SETUP QUALITY SCORE & TIMING",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Confidence Threshold Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Setup Score Threshold", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                    Text(
                        "${confidenceThreshold.toInt()}/100 min",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal
                    )
                }
                Text(
                    text = "Signals under this confluence score are filtered as 'WAIT / NO TRADE' to enforce trading discipline.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Slider(
                    value = confidenceThreshold,
                    onValueChange = { confidenceThreshold = it },
                    valueRange = 50f..95f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryTeal,
                        activeTrackColor = PrimaryTeal,
                        inactiveTrackColor = SurfaceBorder
                    ),
                    modifier = Modifier.testTag("confidence_slider")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Scan Delay Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Scan Frame Delay", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                    Text(
                        "${scanDelayMs.toInt()} ms",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentPurple
                    )
                }
                Text(
                    text = "Delay after tap allowing broker candlestick rendering to settle before taking frame.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Slider(
                    value = scanDelayMs,
                    onValueChange = { scanDelayMs = it },
                    valueRange = 300f..2000f,
                    steps = 16,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentPurple,
                        activeTrackColor = AccentPurple,
                        inactiveTrackColor = SurfaceBorder
                    ),
                    modifier = Modifier.testTag("delay_slider")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Analysis Mode Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ANALYSIS ENGINE DEPTH",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = analysisMode == "fast",
                        onClick = { analysisMode = "fast" },
                        label = { Text("⚡ Fast Mode (1M)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryTealAlpha20,
                            selectedLabelColor = PrimaryTeal
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = analysisMode == "deep",
                        onClick = { analysisMode = "deep" },
                        label = { Text("🧠 Deep (8-Point)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentPurple.copy(alpha = 0.2f),
                            selectedLabelColor = AccentPurple
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = if (analysisMode == "fast")
                        "Fast Mode: Ideal for 1-minute binary turbo candles focusing on immediate wick exhaustion and engulfing patterns."
                    else
                        "Deep Mode: Institutional 8-point analysis checking Fair Value Gaps (FVG), order blocks, and multi-timeframe trend alignment.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Telegram Signal Alerts Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TELEGRAM SIGNAL ALERTS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Switch(
                        checked = telegramEnabled,
                        onCheckedChange = { telegramEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PrimaryTeal,
                            checkedTrackColor = PrimaryTealAlpha20
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "A / A+ grade signals are auto-posted to your Telegram channel. Create a bot with @BotFather, add it as admin to your channel, then paste the token + channel ID (e.g. @yourchannel).",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                OutlinedTextField(
                    value = telegramBotToken,
                    onValueChange = {
                        telegramBotToken = it
                        onResetTelegramTestState()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Bot token (from @BotFather)", color = TextSecondary, fontSize = 13.sp) },
                    singleLine = true,
                    visualTransformation = if (telegramTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { telegramTokenVisible = !telegramTokenVisible }) {
                            Icon(
                                imageVector = if (telegramTokenVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (telegramTokenVisible) "Hide token" else "Show token",
                                tint = TextSecondary
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryTeal,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = PrimaryTeal
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = telegramChatId,
                    onValueChange = {
                        telegramChatId = it
                        onResetTelegramTestState()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Channel ID (e.g. @mychannel)", color = TextSecondary, fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryTeal,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = PrimaryTeal
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onTestTelegram(telegramBotToken, telegramChatId) },
                        enabled = telegramBotToken.isNotBlank() && telegramChatId.isNotBlank()
                                && telegramTestState !is TestConnectionState.Loading,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (telegramBotToken.isNotBlank()) ElectricGreenBorder else SurfaceBorderSubtle),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            containerColor = ElectricGreenTransparent
                        )
                    ) {
                        if (telegramTestState is TestConnectionState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = ElectricGreen
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sending...", fontSize = 11.sp, color = ElectricGreen)
                        } else {
                            Text("Test Telegram ✈️", color = ElectricGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    when (telegramTestState) {
                        is TestConnectionState.Success -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = BullishGreen,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test message sent!", color = BullishGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        is TestConnectionState.Error -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = BearishRed,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = telegramTestState.error.take(24),
                                    color = BearishRed,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        else -> {}
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Chart Crop Card — cut broker UI chrome so the AI only sees candles
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "CHART-AREA CROP",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Cuts broker UI (header bar, Up/Down buttons, side panels) from the screenshot before analysis, so every signal comes from pure price action.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                @Composable
                fun CropSlider(label: String, value: Float, onChange: (Float) -> Unit) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                        Text(
                            "${value.toInt()}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal
                        )
                    }
                    Slider(
                        value = value,
                        onValueChange = onChange,
                        valueRange = 0f..40f,
                        steps = 7,
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryTeal,
                            activeTrackColor = PrimaryTeal,
                            inactiveTrackColor = SurfaceBorder
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                CropSlider("Top (header bar)", cropTopPct) { cropTopPct = it }
                CropSlider("Bottom (Up/Down buttons)", cropBottomPct) { cropBottomPct = it }
                CropSlider("Left", cropLeftPct) { cropLeftPct = it }
                CropSlider("Right (price axis)", cropRightPct) { cropRightPct = it }

                Text(
                    text = "Tip: keep the right-side price axis visible — the AI uses it for S/R levels. Defaults fit Quotex mobile layout.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 6. Save & Launch Button
        Button(
            onClick = {
                val updated = ScanSettings(
                    apiKey = apiKey,
                    confidenceThreshold = confidenceThreshold.toInt(),
                    scanDelayMs = scanDelayMs.toLong(),
                    analysisMode = analysisMode,
                    preferredModel = "pixtral-12b-2409",
                    telegramBotToken = telegramBotToken,
                    telegramChatId = telegramChatId,
                    telegramEnabled = telegramEnabled,
                    cropTopPct = cropTopPct.toInt(),
                    cropBottomPct = cropBottomPct.toInt(),
                    cropLeftPct = cropLeftPct.toInt(),
                    cropRightPct = cropRightPct.toInt()
                )
                onSaveAndStart(updated)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_and_start_button"),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricGreenTransparent),
            border = BorderStroke(1.2.dp, ElectricGreenBorder),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "Save & Launch Floating Scanner 🚀",
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp,
                color = ElectricGreen
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
