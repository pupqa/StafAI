package com.bober.autcsv.core.utils

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Память настройки биометрической блокировки (№36): простой флаг
 * «запрашивать биометрию при входе». Как и стиль PDF, это preference,
 * а не секрет — EncryptedSharedPreferences не требуется.
 */
@Singleton
class BiometricLockStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("biometric_prefs", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    private companion object {
        const val KEY_ENABLED = "enabled"
    }
}
