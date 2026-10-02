package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.PredictionResult
import com.example.data.model.ScanSettings
import com.example.data.repository.ScannerRepository
import com.example.service.OverlayService
import com.example.utils.PreferenceManager
import com.example.utils.TelegramNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class TestConnectionState {
    object Idle : TestConnectionState()
    object Loading : TestConnectionState()
    data class Success(val message: String) : TestConnectionState()
    data class Error(val error: String) : TestConnectionState()
}

sealed class AnalysisUiState {
    object Idle : AnalysisUiState()
    object Analyzing : AnalysisUiState()
    data class Success(val result: PredictionResult) : AnalysisUiState()
    data class Error(val message: String) : AnalysisUiState()
}

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ScannerRepository.getInstance(application)
    private val preferenceManager = PreferenceManager.getInstance(application)

    val settings: StateFlow<ScanSettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.getSettings())

    val recentScans: StateFlow<List<PredictionResult>> = repository.getRecentScans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isServiceRunning = MutableStateFlow(OverlayService.isRunning)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _testConnectionState = MutableStateFlow<TestConnectionState>(TestConnectionState.Idle)
    val testConnectionState: StateFlow<TestConnectionState> = _testConnectionState.asStateFlow()

    private val _telegramTestState = MutableStateFlow<TestConnectionState>(TestConnectionState.Idle)
    val telegramTestState: StateFlow<TestConnectionState> = _telegramTestState.asStateFlow()

    private val _analysisState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val analysisState: StateFlow<AnalysisUiState> = _analysisState.asStateFlow()

    fun updateServiceRunningState(running: Boolean) {
        _isServiceRunning.value = running
    }

    fun saveSettings(
        apiKey: String,
        confidenceThreshold: Int,
        scanDelayMs: Long,
        analysisMode: String,
        preferredModel: String,
        telegramBotToken: String,
        telegramChatId: String,
        telegramEnabled: Boolean,
        cropTopPct: Int,
        cropBottomPct: Int,
        cropLeftPct: Int,
        cropRightPct: Int
    ) {
        repository.saveSettings(
            apiKey, confidenceThreshold, scanDelayMs, analysisMode, preferredModel,
            telegramBotToken, telegramChatId, telegramEnabled,
            cropTopPct, cropBottomPct, cropLeftPct, cropRightPct
        )
    }

    fun testConnection(apiKey: String) {
        viewModelScope.launch {
            _testConnectionState.value = TestConnectionState.Loading
            val result = repository.testConnection(apiKey)
            if (result.isSuccess) {
                _testConnectionState.value = TestConnectionState.Success(result.getOrThrow())
            } else {
                _testConnectionState.value = TestConnectionState.Error(
                    result.exceptionOrNull()?.message ?: "Connection test failed"
                )
            }
        }
    }

    fun resetTestState() {
        _testConnectionState.value = TestConnectionState.Idle
    }

    fun testTelegram(botToken: String, chatId: String) {
        viewModelScope.launch {
            _telegramTestState.value = TestConnectionState.Loading
            val result = withContext(Dispatchers.IO) {
                TelegramNotifier.testConnection(botToken, chatId)
            }
            _telegramTestState.value = if (result.isSuccess) {
                TestConnectionState.Success(result.getOrThrow())
            } else {
                TestConnectionState.Error(
                    result.exceptionOrNull()?.message ?: "Telegram test failed"
                )
            }
        }
    }

    fun resetTelegramTestState() {
        _telegramTestState.value = TestConnectionState.Idle
    }

    fun analyzeBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            _analysisState.value = AnalysisUiState.Analyzing
            val result = repository.analyzeBitmap(bitmap)
            if (result.isSuccess) {
                _analysisState.value = AnalysisUiState.Success(result.getOrThrow())
            } else {
                _analysisState.value = AnalysisUiState.Error(
                    result.exceptionOrNull()?.message ?: "Analysis failed"
                )
            }
        }
    }

    fun runSampleAnalysis(isBullish: Boolean) {
        viewModelScope.launch {
            _analysisState.value = AnalysisUiState.Analyzing
            val result = repository.runSampleAnalysis(isBullish)
            _analysisState.value = AnalysisUiState.Success(result)
        }
    }

    fun resetAnalysisState() {
        _analysisState.value = AnalysisUiState.Idle
    }

    fun updateOutcome(scanId: Long, outcome: String) {
        viewModelScope.launch {
            repository.updateOutcome(scanId, outcome)
        }
    }

    fun loadAuditBenchmark() {
        viewModelScope.launch {
            repository.loadAuditBenchmarkTrades()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
