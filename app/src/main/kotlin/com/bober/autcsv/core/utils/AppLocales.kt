package com.bober.autcsv.core.utils

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

/**
 * Локаль, выбранная в приложении (AppCompatDelegate.setApplicationLocales).
 *
 * Application-контекст на API < 33 может не отражать смену языка — он нужен
 * экспортёрам (PDF/DOCX/CSV), парсеру импорта и ViewModel, которые формируют
 * пользовательские строки вне композиции.
 */
object AppLocales {

    /** Тег текущей локали приложения («ru», «en»), по умолчанию — системная. */
    fun currentTag(context: Context): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (!locales.isEmpty) {
            locales[0]?.toLanguageTag()?.let { return it }
        }
        return context.resources.configuration.locales[0].toLanguageTag()
    }

    /**
     * Контекст, переопределённый под выбранную локаль приложения.
     * Если локаль уже применена к ресурсам — возвращает контекст как есть.
     */
    fun localizedContext(context: Context): Context {
        val locale = Locale.forLanguageTag(currentTag(context))
        if (context.resources.configuration.locales[0] == locale) return context
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        return context.createConfigurationContext(config)
    }
}

/** Строка ресурса с учётом выбранного в приложении языка. */
fun Context.localizedString(resId: Int, vararg args: Any?): String =
    AppLocales.localizedContext(this).getString(resId, *args)
