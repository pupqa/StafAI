package android.content.res

import android.os.LocaleList
import java.util.Locale

/**
 * Минимальный JVM-стаб android.content.res.Configuration: покрывает только
 * locales/locale, которые трогает продакшн-код в тестах.
 *
 * Методы объявлены явно с именами java-байткода (getLocales/setLocales):
 * продакшн-классы скомпилированы против android.jar и линкуются по ним.
 */
open class Configuration(other: Configuration? = null) {

    private var _locales: LocaleList = other?.getLocales() ?: LocaleList(Locale.getDefault())

    private var _locale: Locale? = other?.getLocale()

    fun getLocales(): LocaleList = _locales

    fun getLocale(): Locale? = _locale

    fun setLocale(value: Locale?) {
        _locale = value
        if (value != null) _locales = LocaleList(value)
    }

    fun setLocales(value: LocaleList?) {
        if (value != null) _locales = value
    }
}
