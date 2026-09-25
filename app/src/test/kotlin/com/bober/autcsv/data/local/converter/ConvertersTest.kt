package com.bober.autcsv.data.local.converter

import com.bober.autcsv.domain.model.AiAnalysis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты TypeConverter'ов Room: битые/легаси-данные в колонке должны давать
 * безопасные дефолты, а не крашить запрос или возвращать null в non-null тип.
 */
class ConvertersTest {

    private val converters = Converters()

    // ── строки ──────────────────────────────────────────────────────────

    @Test
    fun `string list round-trips`() {
        val value = listOf("Kotlin", "Python")
        assertEquals(value, converters.toStringList(converters.fromStringList(value)))
    }

    @Test
    fun `corrupted string list json yields empty list instead of crash`() {
        assertTrue(converters.toStringList("{not json").isEmpty())
        assertTrue(converters.toStringList("\"just a string\"").isEmpty())
        assertTrue(converters.toStringList("null").isEmpty())
        assertTrue(converters.toStringList("").isEmpty())
    }

    // ── проекты ─────────────────────────────────────────────────────────

    @Test
    fun `corrupted project list json yields empty list`() {
        assertTrue(converters.toProjectList("[{broken").isEmpty())
    }

    // ── AiAnalysis ──────────────────────────────────────────────────────

    @Test
    fun `valid ai analysis round-trips score and lists`() {
        val analysis = AiAnalysis(
            completenessScore = 80,
            strengthAreas = listOf("сильная сторона"),
        )
        val restored = converters.toAiAnalysis(converters.fromAiAnalysis(analysis))
        assertEquals(80, restored.completenessScore)
        assertEquals(listOf("сильная сторона"), restored.strengthAreas)
    }

    @Test
    fun `corrupted ai analysis json yields default object`() {
        val fallback = converters.toAiAnalysis("<<<garbage>>>")
        assertEquals(AiAnalysis(), fallback)
    }
}
