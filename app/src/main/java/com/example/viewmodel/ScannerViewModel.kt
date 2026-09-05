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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
        preferredModel: String
    ) {
        repository.saveSettings(apiKey, confidenceThreshold, scanDelayMs, analysisMode, preferredModel)
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

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
