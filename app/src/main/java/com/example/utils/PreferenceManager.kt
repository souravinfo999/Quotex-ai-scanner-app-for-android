package com.example.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.ScanSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("quotex_ai_pro_prefs", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(getSettings())
    val settingsFlow: StateFlow<ScanSettings> = _settingsFlow.asStateFlow()

    fun getSettings(): ScanSettings {
        return ScanSettings(
            apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
            confidenceThreshold = prefs.getInt(KEY_THRESHOLD, 70),
            scanDelayMs = prefs.getLong(KEY_SCAN_DELAY, 500L),
            analysisMode = prefs.getString(KEY_MODE, "fast") ?: "fast",
            preferredModel = prefs.getString(KEY_MODEL, "pixtral-12b-2409") ?: "pixtral-12b-2409"
        )
    }

    fun saveSettings(
        apiKey: String,
        confidenceThreshold: Int,
        scanDelayMs: Long,
        analysisMode: String,
        preferredModel: String
    ) {
        prefs.edit()
            .putString(KEY_API_KEY, apiKey)
            .putInt(KEY_THRESHOLD, confidenceThreshold)
            .putLong(KEY_SCAN_DELAY, scanDelayMs)
            .putString(KEY_MODE, analysisMode)
            .putString(KEY_MODEL, preferredModel)
            .apply()

        _settingsFlow.value = getSettings()
    }

    fun setOverlayActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_OVERLAY_ACTIVE, active).apply()
    }

    fun isOverlayActive(): Boolean = prefs.getBoolean(KEY_OVERLAY_ACTIVE, false)

    companion object {
        private const val KEY_API_KEY = "key_mistral_api_key"
        private const val KEY_THRESHOLD = "key_confidence_threshold"
        private const val KEY_SCAN_DELAY = "key_scan_delay"
        private const val KEY_MODE = "key_analysis_mode"
        private const val KEY_MODEL = "key_preferred_model"
        private const val KEY_OVERLAY_ACTIVE = "key_overlay_active"

        @Volatile
        private var INSTANCE: PreferenceManager? = null

        fun getInstance(context: Context): PreferenceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferenceManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
