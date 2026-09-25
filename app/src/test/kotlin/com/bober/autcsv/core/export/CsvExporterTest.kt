package com.bober.autcsv.core.export

import com.bober.autcsv.domain.model.CandidateStatus
import com.bober.autcsv.domain.model.Language
import com.bober.autcsv.domain.model.PersonalInfo
import com.bober.autcsv.domain.model.ProfessionalSkills
import com.bober.autcsv.domain.model.Project
import com.bober.autcsv.domain.model.Resume
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты CSV-экспорта: RFC 4180 (разделитель «;», CRLF, BOM UTF-8),
 * экранирование спецсимволов, плоские колонки для списковых полей.
 */
class CsvExporterTest {

    /** Фейковый контекст со строками: getString возвращает имена ключей R.string. */
    private val ctx = TestStringsContext.context()

    // ── escape (RFC 4180) ───────────────────────────────────────────────

    @Test
    fun `plain value stays unquoted`() {
        assertEquals("Kotlin", CsvExporter.escape("Kotlin"))
        assertEquals("Москва", CsvExporter.escape("Москва"))
        assertEquals("", CsvExporter.escape(""))
    }

    @Test
    fun `semicolon forces quoting`() {
        assertEquals("\"a;b\"", CsvExporter.escape("a;b"))
    }

    @Test
    fun `quotes are doubled inside quoted cell`() {
        assertEquals("\"он сказал \"\"привет\"\"\"", CsvExporter.escape("он сказал \"привет\""))
    }

    @Test
    fun `line breaks force quoting and preserved as-is`() {
        assertEquals("\"строка1\nстрока2\"", CsvExporter.escape("строка1\nстрока2"))
        assertEquals("\"a\r\nb\"", CsvExporter.escape("a\r\nb"))
    }

    // ── formula injection ───────────────────────────────────────────────

    @Test
    fun `formula prefixes are neutralized with apostrophe`() {
        // Excel/LibreOffice исполняют ячейки, начинающиеся с =,+,-,@
        assertEquals("'=cmd|' /C calc!", CsvExporter.escape("=cmd|' /C calc!"))
        assertEquals("'+1", CsvExporter.escape("+1"))
        assertEquals("'@sum", CsvExporter.escape("@sum"))
    }

    @Test
    fun `negative number cell is neutralized too`() {
        assertEquals("'-5000", CsvExporter.escape("-5000"))
    }

    @Test
    fun `digits and plus in middle are untouched`() {
        // «+» в середине ячейки безопасен и не экранируется
        assertEquals("телефон +7 999", CsvExporter.escape("телефон +7 999"))
    }

    @Test
    fun `leading plus is neutralized even in phone-like values`() {
        // Осознанный трейдофф: ячейка, начинающаяся с «+», исполняема в Excel,
        // поэтому получает апостроф-префикс
        assertEquals("'+7 (999) 123-45-67", CsvExporter.escape("+7 (999) 123-45-67"))
    }

    // ── export одного резюме ────────────────────────────────────────────

    @Test
    fun `export starts with bom and uses crlf`() {
        val csv = CsvExporter.export(ctx, resume())
        assertTrue("нет BOM UTF-8", csv.startsWith("\uFEFF"))
        assertTrue("нет CRLF", csv.contains("\r\n"))
        assertFalse(csv.endsWith("\n\n"))
    }

    @Test
    fun `header and data row have matching column counts`() {
        val csv = CsvExporter.export(ctx, resume())
        val lines = csv.removePrefix("\uFEFF").trimEnd('\r', '\n').split("\r\n")
        assertEquals(2, lines.size)
        // Значения без ;/«/переносов — можно наивно резать по «;»
        val header = lines[0].split(';')
        val data = lines[1].split(';')
        assertEquals(header.size, data.size)
        // Заголовки локализуются через ресурсы; фейковый контекст отдаёт имена ключей
        assertEquals("csv_col_full_name", header.first())
        assertEquals("csv_col_modified", header.last())
    }

    @Test
    fun `data row carries flattened values`() {
        val csv = CsvExporter.export(ctx, resume())
        val data = csv.removePrefix("\uFEFF").trimEnd('\r', '\n')
            .split("\r\n")[1].split(';')

        assertEquals("Иванова Анна Сергеевна", data[0])
        assertEquals("Android-разработчик", data[1])
        assertEquals("Русский: Родной | Английский: B2", data[14])
        assertEquals("Kotlin | Java", data[16])
        assertEquals("Кошелёк / Lead / 2023", data[24])
        assertEquals("", data[25]) // ссылки проектов не заданы
        assertEquals("status_offer", data[27])
        assertEquals("csv_yes", data[28])
        assertTrue(
            "дата не в ISO: ${data[29]}",
            Regex("\\d{4}-\\d{2}-\\d{2}").matches(data[29]),
        )
    }

    // ── export всей базы ────────────────────────────────────────────────

    @Test
    fun `exportAll writes row per resume`() {
        val csv = CsvExporter.exportAll(ctx, listOf(resume(), resume()))
        val lines = csv.removePrefix("\uFEFF").trimEnd('\r', '\n').split("\r\n")
        assertEquals(3, lines.size) // заголовок + 2 строки
        assertEquals(lines[1], lines[2])
    }

    @Test
    fun `special characters land in a single quoted cell`() {
        val tricky = resume().copy(
            personalInfo = PersonalInfo(
                fullName = "Иванова; Анна",
                specialization = "Android-разработчик",
                totalExperience = "7",
                aboutMe = "строка1\nстрока2; и \"кавычки\"",
            )
        )
        val csv = CsvExporter.export(ctx, tricky)
        val dataLine = csv.removePrefix("\uFEFF").trimEnd('\r', '\n').split("\r\n")[1]

        assertTrue(dataLine.contains("\"Иванова; Анна\""))
        assertTrue(dataLine.contains("\"строка1\nстрока2; и \"\"кавычки\"\"\""))
        // Строка данных одна, несмотря на перенос внутри ячейки
        val fullLines = csv.removePrefix("\uFEFF").trimEnd('\r', '\n').split("\r\n")
        assertEquals(2, fullLines.size)
    }

    // ── Вспомогательное ─────────────────────────────────────────────────

    private fun resume(): Resume = Resume(
        id = "csv-test",
        personalInfo = PersonalInfo(
            fullName = "Иванова Анна Сергеевна",
            specialization = "Android-разработчик",
            totalExperience = "7",
            specializationExperience = "5",
            email = "anna@example.com",
            phone = "+7 921 123-45-67",
            location = "Санкт-Петербург",
            aboutMe = "Люблю чистый код",
            salaryMin = "180000",
            salaryMax = "250000",
            readyToRelocate = "Не готова к переезду",
            languages = listOf(
                Language("Русский", "Родной"),
                Language("Английский", "B2"),
                Language("", ""), // пустые отфильтровываются
            ),
        ),
        professionalSkills = ProfessionalSkills(
            programmingLanguages = listOf("Kotlin", "Java"),
            professionalAchievements = listOf("Снизила время старта"),
        ),
        projects = listOf(
            Project(
                name = "Кошелёк",
                role = "Lead",
                duration = "2023",
                description = "Финтех-приложение",
            )
        ),
        lastModified = 1717171717000L,
        status = CandidateStatus.OFFER,
        isFavorite = true,
    )
}
