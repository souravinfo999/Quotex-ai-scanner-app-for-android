package com.example.ui.screens

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
    onTestConnection: (String) -> Unit,
    onSaveAndStart: (ScanSettings) -> Unit,
    onResetTestState: () -> Unit,
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
            text = "AI Scanner Configuration",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryTeal,
            letterSpacing = (-0.5).sp
        )
        Text(
            text = "MISTRAL-VISION-PIXTRAL • ALGORITHM SETUP",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = TextSecondary,
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
                        modifier = Modifier.testTag("test_connection_button")
                    ) {
                        if (testConnectionState is TestConnectionState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = PrimaryTeal
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Testing...", fontSize = 11.sp)
                        } else {
                            Text("Test Connection ⚡", color = PrimaryTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                    text = "CONFIDENCE & SCAN TIMING",
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
                    Text("Threshold Filter", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                    Text(
                        "${confidenceThreshold.toInt()}% min",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal
                    )
                }
                Text(
                    text = "Signals under this threshold are rejected as 'UNCERTAIN' to safeguard trading funds.",
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

        Spacer(modifier = Modifier.height(20.dp))

        // 4. Save & Launch Button
        Button(
            onClick = {
                val updated = ScanSettings(
                    apiKey = apiKey,
                    confidenceThreshold = confidenceThreshold.toInt(),
                    scanDelayMs = scanDelayMs.toLong(),
                    analysisMode = analysisMode,
                    preferredModel = "pixtral-12b-2409"
                )
                onSaveAndStart(updated)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_and_start_button"),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Save & Launch Floating Scanner 🚀",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = BackgroundDark
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
