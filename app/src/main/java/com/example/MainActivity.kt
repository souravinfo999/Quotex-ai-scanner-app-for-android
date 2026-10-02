package com.example

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.OverlayService
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.ElectricGreenBorder
import com.example.ui.theme.ElectricGreenTransparent
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.PrimaryTealAlpha20
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceBorderSubtle
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.utils.PermissionHelper
import com.example.viewmodel.ScannerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ScannerViewModel by viewModels()

    private var hasOverlayPermission by mutableStateOf(false)
    private var hasNotificationPermission by mutableStateOf(false)
    private var hasMediaProjectionData by mutableStateOf(false)

    // MediaProjection permission launcher
    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            hasMediaProjectionData = true
            OverlayService.start(this, result.resultCode, result.data)
            viewModel.updateServiceRunningState(true)
            Toast.makeText(this, "🚀 Floating Scanner Launched!", Toast.LENGTH_SHORT).show()
            // Minimize app so floating button is immediately usable over trading app
            moveTaskToBack(true)
        } else {
            Toast.makeText(this, "⚠️ Screen capture permission is required to analyze charts", Toast.LENGTH_LONG).show()
        }
    }

    // Overlay Permission launcher
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkPermissions()
    }

    // Notification Permission launcher (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    // Visual media picker launcher for testing chart screenshots
    private val pickVisualMediaLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        viewModel.analyzeBitmap(bitmap)
                    } else {
                        Toast.makeText(this, "Cannot decode selected image", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkPermissions()

        setContent {
            MyApplicationTheme {
                val settings by viewModel.settings.collectAsState()
                val recentScans by viewModel.recentScans.collectAsState()
                val isServiceRunning by viewModel.isServiceRunning.collectAsState()
                val testState by viewModel.testConnectionState.collectAsState()
                val telegramTestState by viewModel.telegramTestState.collectAsState()
                val analysisState by viewModel.analysisState.collectAsState()

                var showSplash by remember {
                    mutableStateOf(!PermissionHelper.hasOverlayPermission(this))
                }
                var selectedTab by remember { mutableIntStateOf(0) }

                LaunchedEffect(Unit) {
                    viewModel.updateServiceRunningState(OverlayService.isRunning)
                }

                if (showSplash) {
                    SplashScreen(
                        hasOverlayPermission = hasOverlayPermission,
                        hasNotificationPermission = hasNotificationPermission,
                        hasMediaProjectionPermission = hasMediaProjectionData,
                        onRequestOverlay = { requestOverlayPermission() },
                        onRequestNotification = { requestNotificationPermission() },
                        onRequestMediaProjection = { startFloatingScanner() },
                        onContinue = { showSplash = false }
                    )
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = BackgroundDark,
                        bottomBar = {
                            CyberBottomNavBar(
                                selectedTab = selectedTab,
                                onTabSelected = { selectedTab = it }
                            )
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            when (selectedTab) {
                                0 -> DashboardScreen(
                                    isServiceRunning = isServiceRunning,
                                    settings = settings,
                                    analysisState = analysisState,
                                    onToggleService = {
                                        if (isServiceRunning) {
                                            stopFloatingScanner()
                                        } else {
                                            startFloatingScanner()
                                        }
                                    },
                                    onPickImage = {
                                        pickVisualMediaLauncher.launch(
                                            androidx.activity.result.PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                            )
                                        )
                                    },
                                    onTestSample = { isBullish ->
                                        viewModel.runSampleAnalysis(isBullish)
                                    },
                                    onMarkOutcome = { id, outcome ->
                                        viewModel.updateOutcome(id, outcome)
                                    },
                                    onDismissAnalysis = {
                                        viewModel.resetAnalysisState()
                                    },
                                    onNavigateToSettings = {
                                        selectedTab = 1
                                    }
                                )
                                1 -> SettingsScreen(
                                    currentSettings = settings,
                                    testConnectionState = testState,
                                    telegramTestState = telegramTestState,
                                    onTestConnection = { key ->
                                        viewModel.testConnection(key)
                                    },
                                    onTestTelegram = { token, chatId ->
                                        viewModel.testTelegram(token, chatId)
                                    },
                                    onSaveAndStart = { updatedSettings ->
                                        viewModel.saveSettings(
                                            updatedSettings.apiKey,
                                            updatedSettings.confidenceThreshold,
                                            updatedSettings.scanDelayMs,
                                            updatedSettings.analysisMode,
                                            updatedSettings.preferredModel,
                                            updatedSettings.telegramBotToken,
                                            updatedSettings.telegramChatId,
                                            updatedSettings.telegramEnabled
                                        )
                                        Toast.makeText(this@MainActivity, "Settings Saved!", Toast.LENGTH_SHORT).show()
                                        startFloatingScanner()
                                    },
                                    onResetTestState = {
                                        viewModel.resetTestState()
                                    },
                                    onResetTelegramTestState = {
                                        viewModel.resetTelegramTestState()
                                    }
                                )
                                2 -> HistoryScreen(
                                    scans = recentScans,
                                    onMarkOutcome = { id, outcome ->
                                        viewModel.updateOutcome(id, outcome)
                                    },
                                    onClearHistory = {
                                        viewModel.clearHistory()
                                    },
                                    onLoadBenchmark = {
                                        viewModel.loadAuditBenchmark()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
        viewModel.updateServiceRunningState(OverlayService.isRunning)
    }

    private fun checkPermissions() {
        hasOverlayPermission = PermissionHelper.hasOverlayPermission(this)
        hasNotificationPermission = PermissionHelper.hasNotificationPermission(this)
        hasMediaProjectionData = OverlayService.mediaProjectionIntentData != null
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = PermissionHelper.requestOverlayPermissionIntent(this)
            overlayPermissionLauncher.launch(intent)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            hasNotificationPermission = true
        }
    }

    private fun startFloatingScanner() {
        if (!PermissionHelper.hasOverlayPermission(this)) {
            Toast.makeText(this, "Please allow 'Display over other apps' permission first", Toast.LENGTH_LONG).show()
            requestOverlayPermission()
            return
        }

        val mpManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(mpManager.createScreenCaptureIntent())
    }

    private fun stopFloatingScanner() {
        OverlayService.stop(this)
        viewModel.updateServiceRunningState(false)
        Toast.makeText(this, "Floating Scanner Stopped", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun CyberBottomNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xF20E111B), // Translucent frosted cyber glass
            border = BorderStroke(
                1.2.dp,
                Brush.horizontalGradient(
                    listOf(
                        SurfaceBorderSubtle,
                        ElectricGreenBorder,
                        SurfaceBorderSubtle
                    )
                )
            ),
            shadowElevation = 16.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CyberNavItem(
                    selected = selectedTab == 0,
                    icon = Icons.Default.BarChart,
                    label = "SCANNER",
                    testTag = "nav_dashboard",
                    onClick = { onTabSelected(0) }
                )
                CyberNavItem(
                    selected = selectedTab == 1,
                    icon = Icons.Default.Tune,
                    label = "CONFIG",
                    testTag = "nav_settings",
                    onClick = { onTabSelected(1) }
                )
                CyberNavItem(
                    selected = selectedTab == 2,
                    icon = Icons.Default.History,
                    label = "HISTORY",
                    testTag = "nav_history",
                    onClick = { onTabSelected(2) }
                )
            }
        }
    }
}

@Composable
private fun RowScope.CyberNavItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    val animatedBg by animateColorAsState(
        targetValue = if (selected) ElectricGreenTransparent else Color.Transparent,
        animationSpec = tween(220),
        label = "nav_item_bg"
    )
    val animatedBorder by animateColorAsState(
        targetValue = if (selected) ElectricGreenBorder else Color.Transparent,
        animationSpec = tween(220),
        label = "nav_item_border"
    )
    val iconAndTextColor by animateColorAsState(
        targetValue = if (selected) ElectricGreen else TextSecondary.copy(alpha = 0.55f),
        animationSpec = tween(180),
        label = "nav_item_color"
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(animatedBg)
            .border(1.dp, animatedBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconAndTextColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.6.sp,
                color = iconAndTextColor
            )
        }
    }
}
