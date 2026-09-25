package com.bober.autcsv.core.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Хранение и применение выбранного языка интерфейса.
 *
 * Использует AppCompatDelegate.setApplicationLocales(): MainActivity —
 * AppCompatActivity, поэтому смена языка пересоздаёт Activity мгновенно
 * на всех поддерживаемых API (32+). Персистентность обеспечивает сервис
 * AppLocalesMetadataHolderService (autoStoreLocales в манифесте) на API < 33
 * и системный механизм per-app locales на API 33+.
 */
@Singleton
class LanguageStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)

    companion object {
        const val LANG_RU = "ru"
        const val LANG_EN = "en"
        private const val KEY_LOCALE = "locale"
    }

    /** Текущий выбранный язык (по умолчанию — ru). */
    val currentLocale: Locale
        get() = Locale(getSavedLocaleTag() ?: LANG_RU)

    private fun getSavedLocaleTag(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = AppCompatDelegate.getApplicationLocales()
            if (!locales.isEmpty) {
                locales[0]?.toLanguageTag()?.let { return it }
            }
        }
        return prefs.getString(KEY_LOCALE, LANG_RU)
    }

    /** Установить язык и перезапустить Activity для применения. */
    fun setLanguage(localeTag: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            prefs.edit().putString(KEY_LOCALE, localeTag).apply()
        }
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(localeTag)
        )
    }
}