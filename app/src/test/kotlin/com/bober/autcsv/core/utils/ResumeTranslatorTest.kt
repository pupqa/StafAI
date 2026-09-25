package com.bober.autcsv.core.utils

import com.bober.autcsv.domain.model.EducationEntry
import com.bober.autcsv.domain.model.PersonalInfo
import com.bober.autcsv.domain.model.ProfessionalSkills
import com.bober.autcsv.domain.model.Project
import com.bober.autcsv.domain.model.Resume
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты извлечения/применения переводимых полей: полный набор путей,
 * пропуск пустых значений, игнорирование неизвестных/пустых переводов
 * и неизменность оригинала.
 */
class ResumeTranslatorTest {

    private fun resume(): Resume = Resume(
        id = "tr-test",
        summary = "Краткое описание",
        personalInfo = PersonalInfo(
            fullName = "Иванова Анна",
            specialization = "Android-разработчик",
            totalExperience = "7",
            aboutMe = "О себе текст",
            education = "СПбГУ, Программная инженерия",
            location = "Санкт-Петербург",
            readyToRelocate = "Готов к переезду",
            employment = "Полная занятость",
            educations = listOf(
                EducationEntry(level = "Высшее (бакалавриат)", specialty = "ИТ", institution = "ВУЗ")
            ),
        ),
        professionalSkills = ProfessionalSkills(
            softSkills = listOf("Коммуникабельность", "Наставничество"),
            professionalAchievements = listOf("Ускорила запуск приложения"),
        ),
        projects = listOf(
            Project(
                name = "Кошелёк",
                role = "Lead",
                duration = "2023 — 2025",
                description = "Финтех",
                responsibilities = listOf("Архитектура", "Код-ревью"),
            )
        ),
    )

    @Test
    fun `extract covers all text fields and skips blanks`() {
        val fields = ResumeTranslator.extractTranslatable(resume())
        val paths = fields.map { it.path }

        assertTrue("summary" in paths)
        assertTrue("personalInfo.specialization" in paths)
        assertTrue("personalInfo.aboutMe" in paths)
        assertTrue("educations[0].level" in paths)
        assertTrue("projects[0].name" in paths)
        assertTrue("projects[0].responsibilities[1]" in paths)
        assertTrue("professionalSkills.softSkills[0]" in paths)

        // Нетекстовые поля не извлекаются
        assertTrue(paths.none { it.contains("fullName") })
        assertTrue(paths.none { it.contains("email") })
        assertTrue(paths.none { it.contains("operatingSystems") })

        // Все значения непустые
        assertTrue(fields.all { it.value.isNotBlank() })
    }

    @Test
    fun `apply translations builds translated copy without touching original`() {
        val original = resume()
        val translations = mapOf(
            "summary" to "Short summary",
            "personalInfo.specialization" to "Android Developer",
            "projects[0].role" to "Tech Lead",
            "projects[0].responsibilities[0]" to "Architecture",
            "professionalSkills.softSkills[1]" to "Mentorship",
            // Мусор: неизвестный путь и пустое значение — игнорируются
            "unknown.path" to "??",
            "personalInfo.aboutMe" to "",
        )

        val translated = ResumeTranslator.applyTranslations(original, translations)

        assertEquals("Short summary", translated.summary)
        assertEquals("Android Developer", translated.personalInfo.specialization)
        assertEquals("Tech Lead", translated.projects[0].role)
        assertEquals("Architecture", translated.projects[0].responsibilities[0])
        assertEquals("Код-ревью", translated.projects[0].responsibilities[1]) // не переведено
        assertEquals("Mentorship", translated.professionalSkills.softSkills[1])
        assertEquals("Коммуникабельность", translated.professionalSkills.softSkills[0])
        // Пустой перевод игнорируется — остаётся оригинал
        assertEquals("О себе текст", translated.personalInfo.aboutMe)

        // Оригинал не изменён
        assertEquals("Краткое описание", original.summary)
        assertEquals("Android-разработчик", original.personalInfo.specialization)
    }

    @Test
    fun `empty translations map returns equivalent resume`() {
        val original = resume()
        val result = ResumeTranslator.applyTranslations(original, emptyMap())
        assertEquals(original, result)
    }
}
