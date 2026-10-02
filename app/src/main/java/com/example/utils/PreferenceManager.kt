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
            preferredModel = prefs.getString(KEY_MODEL, "pixtral-12b-2409") ?: "pixtral-12b-2409",
            telegramBotToken = prefs.getString(KEY_TELEGRAM_BOT_TOKEN, "") ?: "",
            telegramChatId = prefs.getString(KEY_TELEGRAM_CHAT_ID, "") ?: "",
            telegramEnabled = prefs.getBoolean(KEY_TELEGRAM_ENABLED, false),
            cropTopPct = prefs.getInt(KEY_CROP_TOP, 12),
            cropBottomPct = prefs.getInt(KEY_CROP_BOTTOM, 22),
            cropLeftPct = prefs.getInt(KEY_CROP_LEFT, 3),
            cropRightPct = prefs.getInt(KEY_CROP_RIGHT, 3)
        )
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
        prefs.edit()
            .putString(KEY_API_KEY, apiKey)
            .putInt(KEY_THRESHOLD, confidenceThreshold)
            .putLong(KEY_SCAN_DELAY, scanDelayMs)
            .putString(KEY_MODE, analysisMode)
            .putString(KEY_MODEL, preferredModel)
            .putString(KEY_TELEGRAM_BOT_TOKEN, telegramBotToken.trim())
            .putString(KEY_TELEGRAM_CHAT_ID, telegramChatId.trim())
            .putBoolean(KEY_TELEGRAM_ENABLED, telegramEnabled)
            .putInt(KEY_CROP_TOP, cropTopPct.coerceIn(0, 40))
            .putInt(KEY_CROP_BOTTOM, cropBottomPct.coerceIn(0, 40))
            .putInt(KEY_CROP_LEFT, cropLeftPct.coerceIn(0, 40))
            .putInt(KEY_CROP_RIGHT, cropRightPct.coerceIn(0, 40))
            .apply()

        _settingsFlow.value = getSettings()
    }

    /**
     * Persists the last processed scan fingerprint so the duplicate-signal
     * filter keeps working even after the app process is killed/restarted.
     */
    fun saveLastScanFingerprint(direction: String, structure: String, timestamp: Long) {
        prefs.edit()
            .putString(KEY_LAST_SCAN_DIR, direction)
            .putString(KEY_LAST_SCAN_STRUCT, structure)
            .putLong(KEY_LAST_SCAN_TS, timestamp)
            .apply()
    }

    fun getLastScanFingerprint(): Triple<String, String, Long>? {
        val ts = prefs.getLong(KEY_LAST_SCAN_TS, 0L)
        if (ts == 0L) return null
        val dir = prefs.getString(KEY_LAST_SCAN_DIR, "") ?: ""
        if (dir.isBlank()) return null
        val struct = prefs.getString(KEY_LAST_SCAN_STRUCT, "") ?: ""
        return Triple(dir, struct, ts)
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
        private const val KEY_TELEGRAM_BOT_TOKEN = "key_telegram_bot_token"
        private const val KEY_TELEGRAM_CHAT_ID = "key_telegram_chat_id"
        private const val KEY_TELEGRAM_ENABLED = "key_telegram_enabled"
        private const val KEY_CROP_TOP = "key_crop_top"
        private const val KEY_CROP_BOTTOM = "key_crop_bottom"
        private const val KEY_CROP_LEFT = "key_crop_left"
        private const val KEY_CROP_RIGHT = "key_crop_right"
        private const val KEY_LAST_SCAN_DIR = "key_last_scan_dir"
        private const val KEY_LAST_SCAN_STRUCT = "key_last_scan_struct"
        private const val KEY_LAST_SCAN_TS = "key_last_scan_ts"

        @Volatile
        private var INSTANCE: PreferenceManager? = null

        fun getInstance(context: Context): PreferenceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferenceManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
