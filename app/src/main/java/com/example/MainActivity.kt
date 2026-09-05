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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.OverlayService
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.PrimaryTealAlpha20
import com.example.ui.theme.SurfaceBorder
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
                            NavigationBar(
                                containerColor = SurfaceCard,
                                contentColor = TextPrimary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, SurfaceBorder, RoundedCornerShape(0.dp))
                            ) {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    icon = {
                                        Icon(
                                            Icons.Default.BarChart,
                                            contentDescription = "Scanner",
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            "SCANNER",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.5.sp
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = PrimaryTeal,
                                        selectedTextColor = PrimaryTeal,
                                        indicatorColor = PrimaryTealAlpha20,
                                        unselectedIconColor = TextSecondary.copy(alpha = 0.5f),
                                        unselectedTextColor = TextSecondary.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.testTag("nav_dashboard")
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    icon = {
                                        Icon(
                                            Icons.Default.Tune,
                                            contentDescription = "Config",
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            "CONFIG",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.5.sp
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = PrimaryTeal,
                                        selectedTextColor = PrimaryTeal,
                                        indicatorColor = PrimaryTealAlpha20,
                                        unselectedIconColor = TextSecondary.copy(alpha = 0.5f),
                                        unselectedTextColor = TextSecondary.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.testTag("nav_settings")
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 2,
                                    onClick = { selectedTab = 2 },
                                    icon = {
                                        Icon(
                                            Icons.Default.History,
                                            contentDescription = "History",
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            "HISTORY",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.5.sp
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = PrimaryTeal,
                                        selectedTextColor = PrimaryTeal,
                                        indicatorColor = PrimaryTealAlpha20,
                                        unselectedIconColor = TextSecondary.copy(alpha = 0.5f),
                                        unselectedTextColor = TextSecondary.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.testTag("nav_history")
                                )
                            }
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
                                    onTestConnection = { key ->
                                        viewModel.testConnection(key)
                                    },
                                    onSaveAndStart = { updatedSettings ->
                                        viewModel.saveSettings(
                                            updatedSettings.apiKey,
                                            updatedSettings.confidenceThreshold,
                                            updatedSettings.scanDelayMs,
                                            updatedSettings.analysisMode,
                                            updatedSettings.preferredModel
                                        )
                                        Toast.makeText(this@MainActivity, "Settings Saved!", Toast.LENGTH_SHORT).show()
                                        startFloatingScanner()
                                    },
                                    onResetTestState = {
                                        viewModel.resetTestState()
                                    }
                                )
                                2 -> HistoryScreen(
                                    scans = recentScans,
                                    onMarkOutcome = { id, outcome ->
                                        viewModel.updateOutcome(id, outcome)
                                    },
                                    onClearHistory = {
                                        viewModel.clearHistory()
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
