package com.bober.autcsv.presentation.screens.form

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import com.bober.autcsv.R
import com.bober.autcsv.presentation.screens.form.SuggestionDictionary.EducationLevel
import com.bober.autcsv.presentation.screens.form.SuggestionDictionary.RelocationOption
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * Тесты справочника автодополнения: фильтрация с нормализацией, кэш,
 * соответствие специальностей уровню образования, двуязычное распознавание
 * сохранённых меток.
 *
 * Локализуемые списки читаются из строковых ресурсов, поэтому для них
 * используется фейковый Context c getStringArray, возвращающим эталонное
 * русское содержимое (дубликат values/strings.xml — фиксирует привязку
 * «перечисление → массив ресурса» и само содержимое).
 */
class SuggestionDictionaryTest {

    // ── filter ──────────────────────────────────────────────────────────

    @Test
    fun `blank query returns first items up to limit`() {
        val result = SuggestionDictionary.filter(SuggestionDictionary.institutions, "")
        assertEquals(SuggestionDictionary.institutions.take(6), result)

        val limited = SuggestionDictionary.filter(SuggestionDictionary.companies, "   ", limit = 3)
        assertEquals(3, limited.size)
    }

    @Test
    fun `filter matches substring case-insensitively`() {
        val cities = listOf("Москва", "Санкт-Петербург", "Новосибирск")
        val result = SuggestionDictionary.filter(cities, "мос")
        assertEquals(listOf("Москва"), result)

        val byCyrillicLowercase = SuggestionDictionary.filter(SuggestionDictionary.institutions, "спбг")
        assertTrue(byCyrillicLowercase.contains("СПбГУ"))
    }

    @Test
    fun `filter replaces yo letter in query and dictionary`() {
        // «Ёлкина» находится по запросу без ё — нормализация ё→е симметрична
        val dictionary = listOf("Порт Ёлкина", "Порт Северный")
        val result = SuggestionDictionary.filter(dictionary, "елкина")
        assertEquals(listOf("Порт Ёлкина"), result)
    }

    @Test
    fun `filter respects limit`() {
        val many = SuggestionDictionary.programmingLanguages
        val result = SuggestionDictionary.filter(many, "", limit = 100)
        assertTrue(result.size <= 100)
        val matched = SuggestionDictionary.filter(many, "s", limit = 2)
        assertTrue(matched.size <= 2)
    }

    @Test
    fun `filter caches result per dictionary and query`() {
        val dictionary = listOf("Alpha", "Beta")
        val first = SuggestionDictionary.filter(dictionary, "a")
        val second = SuggestionDictionary.filter(dictionary, "a")
        assertSame(first, second)
    }

    @Test
    fun `no query text yields empty result for absent matches`() {
        val cities = listOf("Москва", "Санкт-Петербург")
        val result = SuggestionDictionary.filter(cities, "Вашингтон")
        assertTrue(result.isEmpty())
    }

    // ── уровни образования и специальности ──────────────────────────────

    @Test
    fun `education level from label roundtrip in both locales`() {
        // Русские подписи (default/values-ru)
        assertEquals(EducationLevel.BACHELOR, EducationLevel.fromLabel("Высшее (бакалавриат)"))
        assertEquals(EducationLevel.SECONDARY_VOCATIONAL, EducationLevel.fromLabel("СПО"))
        assertEquals(EducationLevel.ADDITIONAL, EducationLevel.fromLabel("Дополнительное"))
        // Английские: резюме, созданное до смены языка, остаётся читаемым
        assertEquals(
            EducationLevel.BACHELOR,
            EducationLevel.fromLabel("Higher education (Bachelor’s)")
        )
        assertEquals(EducationLevel.MASTER, EducationLevel.fromLabel("Higher education (Master’s)"))
        assertNull(EducationLevel.fromLabel("Нет такого уровня"))
        assertNull(EducationLevel.fromLabel(""))
    }

    @Test
    fun `every education level has non-empty specialty list`() {
        val ctx = fakeContext()
        EducationLevel.entries.forEach { level ->
            assertTrue(
                "пустые специальности для ${level.name}",
                SuggestionDictionary.specialtiesFor(ctx, level).isNotEmpty()
            )
        }
    }

    @Test
    fun `specialties match the chosen level`() {
        val ctx = fakeContext()

        val master = SuggestionDictionary.specialtiesFor(ctx, EducationLevel.MASTER)
        assertTrue(master.contains("Искусственный интеллект и машинное обучение"))
        assertTrue(!master.contains("Лечебное дело")) // это специалитет

        val vocational = SuggestionDictionary.specialtiesFor(ctx, EducationLevel.SECONDARY_VOCATIONAL)
        assertTrue(vocational.contains("Компьютерные системы и комплексы"))
    }

    // ── релокация ───────────────────────────────────────────────────────

    @Test
    fun `relocation option from label in both locales`() {
        assertEquals(RelocationOption.NOT_READY, RelocationOption.fromLabel("Не готов к переезду"))
        assertEquals(RelocationOption.READY, RelocationOption.fromLabel("Готов к переезду"))
        assertEquals(
            RelocationOption.CONDITIONAL,
            RelocationOption.fromLabel("Готов при определенных условиях")
        )
        assertEquals(RelocationOption.READY, RelocationOption.fromLabel("Ready to relocate"))
        assertNull(RelocationOption.fromLabel("Инкогнито"))
    }

    // ── целостность справочников ────────────────────────────────────────

    @Test
    fun `dictionaries have no accidental duplicates`() {
        fun assertDistinct(name: String, list: List<String>) {
            assertEquals(
                "дубликаты в справочнике $name",
                list.size,
                list.map { it.lowercase() }.distinct().size
            )
        }
        assertDistinct("institutions", SuggestionDictionary.institutions)
        assertDistinct("companies", SuggestionDictionary.companies)
        assertDistinct("certifications", SuggestionDictionary.certifications)

        // Локализуемые массивы: отсутствие дубликатов в каждой локали
        val ctx = fakeContext()
        EducationLevel.entries.forEach { level ->
            assertDistinct("specialties/${level.name}", SuggestionDictionary.specialtiesFor(ctx, level))
        }
    }

    // ── Фейковый контекст со строковыми массивами ───────────────────────

    /** Эталонные русские списки специальностей (копия values/strings.xml). */
    private val specialtyArrays: Map<Int, List<String>> = mapOf(
        R.array.suggestion_specialties_svo to listOf(
            "Информационные системы и программирование",
            "Компьютерные системы и комплексы",
            "Сетевое и системное администрирование",
            "Программирование в компьютерных системах",
            "Многоканальные телекоммуникационные системы",
            "Техническое обслуживание и ремонт радиоэлектронной техники",
            "Экономика и бухгалтерский учёт",
            "Право и организация социального обеспечения",
            "Дизайн (по отраслям)",
            "Операционная деятельность в логистике",
        ),
        R.array.suggestion_specialties_bachelor to listOf(
            "Программная инженерия",
            "Информатика и вычислительная техника",
            "Прикладная математика и информатика",
            "Информационная безопасность",
            "Информационные системы и технологии",
            "Бизнес-информатика",
            "Прикладная информатика",
            "Радиотехника",
            "Экономика",
            "Менеджмент",
            "Дизайн",
        ),
        R.array.suggestion_specialties_specialist to listOf(
            "Прикладная математика и информатика",
            "Комплексное обеспечение информационной безопасности АС",
            "Программная инженерия",
            "Информатика и вычислительная техника",
            "Автоматизированные системы обработки информации и управления",
            "Вычислительные машины, комплексы, системы и сети",
            "Экономическая безопасность",
            "Лечебное дело",
            "Юриспруденция",
        ),
        R.array.suggestion_specialties_master to listOf(
            "Программная инженерия",
            "Прикладная математика и информатика",
            "Информационная безопасность",
            "Интеллектуальный анализ данных",
            "Искусственный интеллект и машинное обучение",
            "Управление разработкой программного обеспечения",
            "Бизнес-информатика",
        ),
        R.array.suggestion_specialties_postgraduate to listOf(
            "Информатика и вычислительная техника",
            "Математическое моделирование, численные методы и комплексы программ",
            "Теоретические основы информатики",
            "Методы и системы защиты информации",
            "Экономическая теория",
        ),
        R.array.suggestion_specialties_additional to listOf(
            "Профессиональная переподготовка",
            "Повышение квалификации",
            "Data Science и анализ данных",
            "Тестирование программного обеспечения",
            "DevOps-инженерия",
            "Управление проектами",
        ),
    )

    private fun fakeContext(): Context {
        val resourcesMock = mockk<Resources> {
            every { configuration } returns Configuration().apply {
                setLocales(LocaleList(Locale("ru")))
            }
            every { getStringArray(any()) } answers {
                specialtyArrays[firstArg<Int>()]?.toTypedArray()
                    ?: error("fakeContext: нет данных для массива id=${firstArg<Int>()}")
            }
        }
        return mockk {
            every { resources } returns resourcesMock
            every { createConfigurationContext(any()) } answers { this@mockk }
        }
    }
}
