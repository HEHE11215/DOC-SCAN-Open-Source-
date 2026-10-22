package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppThemeMode
import com.example.domain.model.OcrScript
import com.example.domain.model.PdfPageSize
import com.example.domain.model.PdfQuality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "docuscan_settings")

data class UserSettings(
    val themeMode: AppThemeMode = AppThemeMode.LIGHT,
    val defaultOcrScript: OcrScript = OcrScript.LATIN,
    val defaultPdfQuality: PdfQuality = PdfQuality.HIGH,
    val defaultPageSize: PdfPageSize = PdfPageSize.A4,
    val defaultSearchablePdf: Boolean = true,
    val biometricLockEnabled: Boolean = false,
    val appLockPin: String = "",
    val autoCloudSync: Boolean = true,
    val isGridView: Boolean = true,
    val isGuestMode: Boolean = false
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_OCR_SCRIPT = stringPreferencesKey("default_ocr_script")
        val DEFAULT_PDF_QUALITY = stringPreferencesKey("default_pdf_quality")
        val DEFAULT_PAGE_SIZE = stringPreferencesKey("default_page_size")
        val DEFAULT_SEARCHABLE_PDF = booleanPreferencesKey("default_searchable_pdf")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
        val APP_LOCK_PIN = stringPreferencesKey("app_lock_pin")
        val AUTO_CLOUD_SYNC = booleanPreferencesKey("auto_cloud_sync")
        val IS_GRID_VIEW = booleanPreferencesKey("is_grid_view")
        val IS_GUEST_MODE = booleanPreferencesKey("is_guest_mode")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let {
                runCatching { AppThemeMode.valueOf(it) }.getOrNull()
            } ?: AppThemeMode.LIGHT,
            defaultOcrScript = prefs[Keys.DEFAULT_OCR_SCRIPT]?.let {
                runCatching { OcrScript.valueOf(it) }.getOrNull()
            } ?: OcrScript.LATIN,
            defaultPdfQuality = prefs[Keys.DEFAULT_PDF_QUALITY]?.let {
                runCatching { PdfQuality.valueOf(it) }.getOrNull()
            } ?: PdfQuality.HIGH,
            defaultPageSize = prefs[Keys.DEFAULT_PAGE_SIZE]?.let {
                runCatching { PdfPageSize.valueOf(it) }.getOrNull()
            } ?: PdfPageSize.A4,
            defaultSearchablePdf = prefs[Keys.DEFAULT_SEARCHABLE_PDF] ?: true,
            biometricLockEnabled = prefs[Keys.BIOMETRIC_LOCK] ?: false,
            appLockPin = prefs[Keys.APP_LOCK_PIN] ?: "",
            autoCloudSync = prefs[Keys.AUTO_CLOUD_SYNC] ?: true,
            isGridView = prefs[Keys.IS_GRID_VIEW] ?: true,
            isGuestMode = prefs[Keys.IS_GUEST_MODE] ?: false
        )
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDefaultOcrScript(script: OcrScript) {
        context.dataStore.edit { it[Keys.DEFAULT_OCR_SCRIPT] = script.name }
    }

    suspend fun setDefaultPdfQuality(quality: PdfQuality) {
        context.dataStore.edit { it[Keys.DEFAULT_PDF_QUALITY] = quality.name }
    }

    suspend fun setDefaultPageSize(pageSize: PdfPageSize) {
        context.dataStore.edit { it[Keys.DEFAULT_PAGE_SIZE] = pageSize.name }
    }

    suspend fun setDefaultSearchablePdf(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DEFAULT_SEARCHABLE_PDF] = enabled }
    }

    suspend fun setBiometricLock(enabled: Boolean, pin: String = "") {
        context.dataStore.edit {
            it[Keys.BIOMETRIC_LOCK] = enabled
            if (pin.isNotEmpty() || !enabled) {
                it[Keys.APP_LOCK_PIN] = pin
            }
        }
    }

    suspend fun setAutoCloudSync(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_CLOUD_SYNC] = enabled }
    }

    suspend fun setGridView(isGrid: Boolean) {
        context.dataStore.edit { it[Keys.IS_GRID_VIEW] = isGrid }
    }

    suspend fun setGuestMode(isGuest: Boolean) {
        context.dataStore.edit { it[Keys.IS_GUEST_MODE] = isGuest }
    }
}
