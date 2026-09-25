package com.bober.autcsv.core.export

import com.bober.autcsv.domain.model.PersonalInfo
import com.bober.autcsv.domain.model.ProfessionalSkills
import com.bober.autcsv.domain.model.Project
import com.bober.autcsv.domain.model.Resume
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import java.io.StringReader
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Тесты DOCX-экспорта: XML-экранирование, структура OOXML-пакета (zip
 * из обязательных частей) и корректность document.xml.
 */
class DocxExporterTest {

    /** Фейковый контекст со строками: getString возвращает имена ключей R.string. */
    private val ctx = TestStringsContext.context()

    // ── escape (XML) ────────────────────────────────────────────────────

    @Test
    fun `xml special characters are escaped`() {
        assertEquals("&amp;", DocxExporter.escape("&"))
        assertEquals("&lt;", DocxExporter.escape("<"))
        assertEquals("&gt;", DocxExporter.escape(">"))
        assertEquals("&quot;", DocxExporter.escape("\""))
        assertEquals("&apos;", DocxExporter.escape("'"))
        assertEquals(
            "a &amp; b &lt;c&gt; &quot;d&quot; &apos;e&apos;",
            DocxExporter.escape("a & b <c> \"d\" 'e'")
        )
        assertEquals("без изменений", DocxExporter.escape("без изменений"))
    }

    // ── Структура пакета ────────────────────────────────────────────────

    @Test
    fun `export produces valid ooxml zip package`() {
        val output = File.createTempFile("resume-test", ".docx")
        try {
            val path = DocxExporter.export(ctx, resume(), output)
            assertEquals(output.absolutePath, path)
            assertTrue("пустой docx", output.length() > 0)

            ZipFile(output).use { zip ->
                val names = zip.entries().asSequence().map { it.name }.toSet()
                listOf(
                    "[Content_Types].xml",
                    "_rels/.rels",
                    "word/_rels/document.xml.rels",
                    "word/styles.xml",
                    "word/document.xml",
                ).forEach { part ->
                    assertTrue("в пакете нет части $part", part in names)
                }
            }
        } finally {
            output.delete()
        }
    }

    @Test
    fun `document xml is well-formed and carries content`() {
        val output = File.createTempFile("resume-test", ".docx")
        try {
            DocxExporter.export(ctx, resume(), output)
            val documentXml = ZipFile(output).use { zip ->
                zip.getInputStream(zip.getEntry("word/document.xml")).bufferedReader()
                    .use { it.readText() }
            }

            // document.xml собирается конкатенацией строк — проверяем, что
            // результат всё ещё валидный XML
            val dom = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(org.xml.sax.InputSource(StringReader(documentXml)))
            val paragraphs = dom.getElementsByTagName("w:p")
            assertTrue("нет ни одного абзаца", paragraphs.length > 0)

            val text = documentXml
            assertTrue(text.contains("Иванова Анна Сергеевна"))
            assertTrue(text.contains("DOC_PROJECT_EXPERIENCE")) // секции — имена ресурсов в верхнем регистре
            assertTrue(text.contains("doc_project_fmt(1, Кошелёк)"))
            assertTrue(text.contains("anna@example.com"))
            assertTrue(text.contains("Kotlin, Ktor"))
        } finally {
            output.delete()
        }
    }

    @Test
    fun `user text with markup stays escaped in document xml`() {
        val tricky = resume().copy(
            personalInfo = PersonalInfo(
                fullName = "Иванова <Анна> & «Кошелёк\"",
                specialization = "Android-разработчик",
                totalExperience = "7",
            )
        )
        val output = File.createTempFile("resume-test", ".docx")
        try {
            DocxExporter.export(ctx, tricky, output)
            val documentXml = ZipFile(output).use { zip ->
                zip.getInputStream(zip.getEntry("word/document.xml")).bufferedReader()
                    .use { it.readText() }
            }
            assertTrue(
                documentXml.contains("Иванова &lt;Анна&gt; &amp; «Кошелёк&quot;")
            )
            // Сырых <Анна> в тексте быть не должно
            assertNotNull(documentXml)
            assertTrue(!documentXml.contains("<Анна>"))
        } finally {
            output.delete()
        }
    }

    @Test
    fun `empty optional sections do not break document`() {
        val minimal = Resume(
            personalInfo = PersonalInfo(
                fullName = "",
                specialization = "",
                totalExperience = "",
            ),
            professionalSkills = ProfessionalSkills(professionalAchievements = emptyList()),
            projects = emptyList(),
        )
        val output = File.createTempFile("resume-test", ".docx")
        try {
            DocxExporter.export(ctx, minimal, output)
            ZipFile(output).use { zip ->
                assertNotNull(zip.getEntry("word/document.xml"))
            }
        } finally {
            output.delete()
        }
    }

    // ── Вспомогательное ─────────────────────────────────────────────────

    private fun resume(): Resume = Resume(
        id = "docx-test",
        personalInfo = PersonalInfo(
            fullName = "Иванова Анна Сергеевна",
            specialization = "Android-разработчик",
            totalExperience = "7",
            specializationExperience = "5",
            email = "anna@example.com",
            phone = "+7 921 123-45-67",
            location = "Санкт-Петербург",
            aboutMe = "Люблю чистый код",
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
                technologies = listOf("Kotlin", "Ktor"),
                responsibilities = listOf("Спроектировала архитектуру"),
            )
        ),
    )
}
