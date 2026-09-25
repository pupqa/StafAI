package com.bober.autcsv.presentation.screens.settings

import android.content.Context
import android.net.Uri
import androidx.biometric.BiometricManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bober.autcsv.BuildConfig
import com.bober.autcsv.R
import com.bober.autcsv.core.utils.ApiKeyStore
import com.bober.autcsv.core.utils.BiometricLockStore
import com.bober.autcsv.core.utils.LanguageStore
import com.bober.autcsv.core.utils.LlmLogger
import com.bober.autcsv.core.utils.localizedString
import com.bober.autcsv.domain.repository.ResumeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject

/** Состояние операций с данными (бэкап/восстановление/CSV, №12/№33/№34). */
data class DataOpsState(
    val isWorking: Boolean = false,
    val message: String? = null,
)

/**
 * ViewModel настроек: хранение API-ключа OpenRouter в шифрованном
 * хранилище (пользовательский ключ приоритетнее ключа сборки) и операции
 * с данными — JSON-бэкап, восстановление, экспорт всей базы в CSV.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val apiKeyStore: ApiKeyStore,
    private val repository: ResumeRepository,
    private val biometricLock: BiometricLockStore,
    private val languageStore: LanguageStore,
    private val pdfStyleStore: com.bober.autcsv.core.pdf.PdfStyleStore,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    fun setLanguage(localeTag: String) {
        languageStore.setLanguage(localeTag)
    }

    /** Ключ для отображения в поле: сохранённый пользователем или ключ сборки. */
    fun initialKey(): String = apiKeyStore.getUserKey().ifBlank { BuildConfig.OPENROUTER_API_KEY }

    fun saveApiKey(value: String) {
        if (value.isNotBlank()) apiKeyStore.saveUserKey(value)
    }

    // ── Встраивание данных резюме в PDF (приватность экспорта) ─────────

    private val _embedResumeData = MutableStateFlow(pdfStyleStore.embedResumeData)
    val embedResumeData: StateFlow<Boolean> = _embedResumeData

    fun setEmbedResumeData(enabled: Boolean) {
        pdfStyleStore.embedResumeData = enabled
        _embedResumeData.value = enabled
    }

    private val _dataOps = MutableStateFlow(DataOpsState())
    val dataOps: StateFlow<DataOpsState> = _dataOps

    /** Экспорт всей базы в JSON-бэкап (№33); файл попадает в системные загрузки. */
    fun createBackup() {
        viewModelScope.launch {
            _dataOps.update { it.copy(isWorking = true, message = null) }
            runCatching { repository.exportBackup() }
                .onSuccess { name ->
                    _dataOps.update {
                        it.copy(
                            isWorking = false,
                            message = context.getString(R.string.backup_saved, name)
                        )
                    }
                }
                .onFailure { e ->
                    LlmLogger.logError(context.getString(R.string.backup_create_failed), e)
                    _dataOps.update {
                        it.copy(
                            isWorking = false,
                            message = context.getString(
                                R.string.backup_create_failed_fmt,
                                e.localizedMessage
                            )
                        )
                    }
                }
        }
    }

    /** Восстановление из выбранного пользователем JSON-файла (№34). */
    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            _dataOps.update { it.copy(isWorking = true, message = null) }
            runCatching {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        input.readBytes().toString(Charsets.UTF_8)
                    } ?: throw IOException(context.getString(R.string.backup_open_failed))
                }
                repository.importBackup(json)
            }.onSuccess { count ->
                _dataOps.update {
                    it.copy(
                        isWorking = false,
                        message = context.localizedString(R.string.backup_restore_count, count)
                    )
                }
            }.onFailure { e ->
                LlmLogger.logError("Не удалось восстановить бэкап", e)
                _dataOps.update {
                    it.copy(
                        isWorking = false,
                        message = context.localizedString(
                            R.string.backup_restore_failed_fmt,
                            e.localizedMessage ?: ""
                        )
                    )
                }
            }
        }
    }

    /** Экспорт всех резюме в один CSV (№12, «база целиком»). */
    fun exportAllCsv() {
        viewModelScope.launch {
            _dataOps.update { it.copy(isWorking = true, message = null) }
            runCatching {
                val path = repository.exportCsv(null)
                withContext(Dispatchers.IO) {
                    val source = File(path)
                    copyToDownloads(source.name, "text/csv", source)
                }
            }.onSuccess { name ->
                _dataOps.update {
                    it.copy(
                        isWorking = false,
                        message = context.localizedString(R.string.csv_export_saved, name)
                    )
                }
            }.onFailure { e ->
                LlmLogger.logError("Не удалось экспортировать CSV", e)
                _dataOps.update {
                    it.copy(
                        isWorking = false,
                        message = context.localizedString(
                            R.string.csv_export_failed_fmt,
                            e.localizedMessage ?: ""
                        )
                    )
                }
            }
        }
    }

    /** Гасит одноразовое сообщение после показа в snackbar. */
    fun consumeMessage() {
        _dataOps.update { it.copy(message = null) }
    }

    private val _biometricEnabled = MutableStateFlow(biometricLock.enabled)
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled

    /**
     * Включение биометрической блокировки (№36). Включить можно только
     * при зарегистрированной на устройстве биометрии; выключение — всегда.
     */
    fun setBiometricEnabled(enabled: Boolean) {
        if (enabled) {
            val canAuthenticate = BiometricManager.from(context).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK
            ) == BiometricManager.BIOMETRIC_SUCCESS
            if (!canAuthenticate) {
                _dataOps.update {
                    it.copy(message = context.localizedString(R.string.biometric_not_configured))
                }
                return
            }
        }
        biometricLock.enabled = enabled
        _biometricEnabled.value = enabled
        _dataOps.update {
            it.copy(
                message = if (enabled) context.localizedString(R.string.biometric_lock_on)
                else context.localizedString(R.string.biometric_lock_off)
            )
        }
    }

    /** Копирует файл в общедоступные загрузки через MediaStore. */
    private fun copyToDownloads(displayName: String, mime: String, source: File) {
        com.bober.autcsv.core.utils.DownloadsSaver.saveFile(context, displayName, mime, source)
    }
}
