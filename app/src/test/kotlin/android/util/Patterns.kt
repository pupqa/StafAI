package android.util

import java.util.regex.Pattern

/**
 * JVM-заглушка android.util.Patterns: mockable android.jar из SDK содержит
 * поля без инициализации (null), а FormValidators полагается на реальные
 * regex. Аналогично стабам android.graphics — класс из тестового source set
 * перекрывает класс из android.jar на classpath unit-тестов.
 */
class Patterns private constructor() {

    companion object {
        /** Близко к AOSP: локальная часть @ домен из ≥2 букв. */
        @JvmField
        val EMAIL_ADDRESS: Pattern = Pattern.compile(
            "[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}"
        )

        /** Схема опциональна (FormValidators дополняет https://), хост — ≥2 меток. */
        @JvmField
        val WEB_URL: Pattern = Pattern.compile(
            "((https?|ftp)://)?(([\\p{L}\\p{N}\\-]+\\.)+[\\p{L}\\p{N}\\-]+(:\\d+)?)" +
                    "(/[\\p{L}\\p{N}\\-._~:/?#\\[\\]@!$&'()*+,;=%]*)?"
        )
    }
}
