package com.bober.autcsv.core.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Хранилище API-ключа OpenRouter на EncryptedSharedPreferences.
 *
 * Приоритет ключа: сохранённый пользователем в настройках → ключ из
 * BuildConfig (local.properties на этапе сборки). Ключ никогда не
 * хранится в открытом виде и не пишется в логи.
 */
@Singleton
class ApiKeyStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_api_keys",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (e: Exception) {
        // Повреждённое хранилище (например, после переноса данных) пересоздаём:
        // ключ можно ввести заново, это лучше чем падение приложения
        LlmLogger.logWarning("EncryptedSharedPreferences недоступен (${e.message}), ключ не будет сохраняться")
        context.getSharedPreferences("secure_api_keys_fallback", Context.MODE_PRIVATE)
    }

    fun getUserKey(): String = prefs.getString(KEY_OPENROUTER, "").orEmpty()

    fun saveUserKey(value: String) {
        prefs.edit().putString(KEY_OPENROUTER, value.trim()).apply()
    }

    fun clearUserKey() {
        prefs.edit().remove(KEY_OPENROUTER).apply()
    }

    companion object {
        private const val KEY_OPENROUTER = "openrouter_api_key"
    }
}
