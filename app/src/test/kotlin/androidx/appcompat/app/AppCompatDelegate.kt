package androidx.appcompat.app

import androidx.core.os.LocaleListCompat

/**
 * JVM-стаб AppCompatDelegate для unit-тестов: хранит локали в статике,
 * как делегат на API < 33 без зарегистрированных Activity. Реальный класс
 * тянет Android-фреймворк и в JVM-окружении непредсказуем.
 *
 * Внимание: стаб затеняет настоящий класс во всём test-sourceset.
 */
class AppCompatDelegate {

    companion object {
        private var storedLocales: LocaleListCompat = LocaleListCompat.getEmptyLocaleList()

        @JvmStatic
        fun getApplicationLocales(): LocaleListCompat = storedLocales

        @JvmStatic
        fun setApplicationLocales(locales: LocaleListCompat) {
            storedLocales = locales
        }
    }
}
