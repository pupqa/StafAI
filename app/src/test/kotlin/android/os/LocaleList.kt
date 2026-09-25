package android.os

import java.util.Locale

/**
 * Минимальный JVM-стаб android.os.LocaleList для unit-тестов: реальный класс
 * из android.jar бросает «not mocked». Поддерживает только поверхность,
 * используемую продакшн-кодом (конструктор, [0], isEmpty).
 */
class LocaleList(private val list: Array<out Locale>) {

    constructor(locale: Locale) : this(arrayOf(locale))

    operator fun get(index: Int): Locale =
        list.getOrElse(index) { error("LocaleList: index $index из ${list.size}") }

    val size: Int get() = list.size

    fun isEmpty(): Boolean = list.isEmpty()

    override fun equals(other: Any?): Boolean =
        other is LocaleList && list.contentEquals(other.list)

    override fun hashCode(): Int = list.contentHashCode()
}
