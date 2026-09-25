package com.bober.autcsv.core.export

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import io.mockk.every
import io.mockk.mockk
import java.util.Locale

/**
 * Фейковый Context для JVM-тестов экспортёров.
 *
 * getString возвращает имя ресурса из R.string (через reflection), при
 * наличии аргументов форматирования — «имя(арг1, арг2)». Это даёт тестам
 * детерминированные, осмысленные маркеры вместо реальных переводов:
 * заголовок CSV-колонки становится «csv_col_full_name», строка документа —
 * «doc_project_fmt(1, Кошелёк)».
 */
object TestStringsContext {

    /** Локаль тестового контекста — русский, как default-набор ресурсов. */
    val testLocale: Locale = Locale("ru")

    private val namesById: Map<Int, String> by lazy {
        com.bober.autcsv.R.string::class.java.declaredFields
            .filter { it.type == java.lang.Integer.TYPE && java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .onEach { it.isAccessible = true }
            .associate { field -> field.getInt(null) to field.name }
    }

    fun context(): Context {
        val resourcesMock = mockk<Resources> {
            every { configuration } returns Configuration().apply {
                setLocales(LocaleList(testLocale))
            }
            // Плюралы (год/года/лет в PDF): возвращаем просто числа-аргументы
            every {
                getQuantityString(any(), any(), *anyVararg<Any>())
            } answers { args.drop(2).filterNotNull().joinToString(", ") }
        }

        return mockk {
            every { getString(any<Int>()) } answers { nameOf(firstArg()) }

            // anyVararg обязателен: вызовы без аргументов формата приходят
            // как пустой массив и не совпадают с обычным any()
            every { getString(any<Int>(), *anyVararg<Any>()) } answers {
                val name = nameOf(firstArg())
                val formatArgs = (args.getOrNull(1) as? Array<out Any?>)
                    ?.filterNotNull().orEmpty()
                if (formatArgs.isEmpty()) name else "$name(${formatArgs.joinToString(", ")})"
            }

            every { resources } returns resourcesMock
            every { createConfigurationContext(any()) } answers { this@mockk }
        }
    }

    private fun nameOf(id: Int): String = namesById[id] ?: "res$id"
}
