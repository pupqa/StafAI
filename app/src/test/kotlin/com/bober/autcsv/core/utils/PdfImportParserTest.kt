package com.bober.autcsv.core.utils

import com.bober.autcsv.domain.model.AiAnalysis
import com.bober.autcsv.domain.model.CandidateStatus
import com.bober.autcsv.domain.model.EducationEntry
import com.bober.autcsv.domain.model.Language
import com.bober.autcsv.domain.model.PersonalInfo
import com.bober.autcsv.domain.model.ProfessionalSkills
import com.bober.autcsv.domain.model.Project
import com.bober.autcsv.domain.model.Resume
import com.bober.autcsv.domain.model.SocialLink
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты текстового разбора PDF-резюме: собственный шаблон, заголовки hh.ru,
 * встроенные JSON-метаданные, эвристики навыков/проектов и нормализация лет.
 */
class PdfImportParserTest {

    // ── Нормализация значения опыта ─────────────────────────────────────

    @Test
    fun `normalizeYearsValue strips word years`() {
        assertEquals("5", PdfImportParser.normalizeYearsValue("5 лет"))
        assertEquals("3", PdfImportParser.normalizeYearsValue("3 года"))
        assertEquals("1", PdfImportParser.normalizeYearsValue("1 год"))
        assertEquals("7", PdfImportParser.normalizeYearsValue("7"))
        assertEquals("7", PdfImportParser.normalizeYearsValue(" 7 "))
        // Фолбэк: первая пара цифр внутри строки
        assertEquals("10", PdfImportParser.normalizeYearsValue("около 10 лет"))
        // Совсем не число — возвращаем как есть
        assertEquals("нет данных", PdfImportParser.normalizeYearsValue("нет данных"))
    }

    // ── Разбор собственного PDF-шаблона ─────────────────────────────────

    @Test
    fun `parses own template sections`() {
        val text = """
            Иванова Анна Сергеевна
            Senior Android-разработчик
            Опыт: 7 лет | Опыт по специализации: 5 лет
            anna@example.com • +7 921 123-45-67 • Санкт-Петербург
            Профессиональное резюме
            Android-разработчик с 7-летним опытом.
            О себе
            Люблю чистый код и код-ревью.
            Образование
            СПбГУТ, Программная инженерия, Высшее (бакалавриат)
            Языки
            Русский — Родной
            Английский — B2
            Технические навыки
            Операционные системы
            Android • Linux
            Языки программирования
            Kotlin • Java • SQL
            Фреймворки
            Jetpack Compose • Ktor
            Библиотеки
            Room • Retrofit
            Базы данных
            PostgreSQL • SQLite
            Другие технологии
            Docker • Git
            Сертификации
            - Kotlin Certified Professional
            Гибкие навыки
            Коммуникабельность • Наставничество
            Ключевые достижения
            • Снизила время холодного старта с 4,2 с до 1,1 с
            • Выстроила процесс код-ревью
            Проектный опыт
            Проект 1: Кошелёк
            Роль: Lead • Срок: 2023 — наст. время • Команда: 8
            Технологии: Kotlin, Jetpack Compose, Room
            Приложение для управления личными финансами.
            • Спроектировала многомодульную архитектуру
        """.trimIndent()

        val resume = PdfImportParser.parseResumeFromText(text)
        val p = resume.personalInfo

        assertEquals("Иванова Анна Сергеевна", p.fullName)
        assertEquals("Senior Android-разработчик", p.specialization)
        assertEquals("7", p.totalExperience)
        assertEquals("5", p.specializationExperience)
        assertEquals("anna@example.com", p.email)
        assertEquals("+7 921 123-45-67", p.phone)
        assertEquals("Санкт-Петербург", p.location)
        assertEquals("Люблю чистый код и код-ревью.", p.aboutMe)
        assertEquals("СПбГУТ, Программная инженерия, Высшее (бакалавриат)", p.education)
        assertEquals("Android-разработчик с 7-летним опытом.", resume.summary)

        assertEquals(
            listOf(Language("Русский", "Родной"), Language("Английский", "B2")),
            p.languages
        )

        val skills = resume.professionalSkills
        assertEquals(listOf("Android", "Linux"), skills.operatingSystems)
        assertEquals(listOf("Kotlin", "Java", "SQL"), skills.programmingLanguages)
        assertEquals(listOf("Jetpack Compose", "Ktor"), skills.frameworks)
        assertEquals(listOf("Room", "Retrofit"), skills.libraries)
        assertEquals(listOf("PostgreSQL", "SQLite"), skills.databases)
        assertEquals(listOf("Docker", "Git"), skills.otherTechnologies)
        assertEquals(listOf("Kotlin Certified Professional"), skills.certifications)
        assertEquals(listOf("Коммуникабельность", "Наставничество"), skills.softSkills)
        assertEquals(
            listOf(
                "Снизила время холодного старта с 4,2 с до 1,1 с",
                "Выстроила процесс код-ревью"
            ),
            skills.professionalAchievements
        )

        assertEquals(1, resume.projects.size)
        val project = resume.projects.first()
        assertEquals("Кошелёк", project.name)
        assertEquals("Lead", project.role)
        assertEquals("2023 — наст. время", project.duration)
        assertEquals("8", project.teamSize)
        assertEquals(listOf("Kotlin", "Jetpack Compose", "Room"), project.technologies)
        assertEquals(
            "Приложение для управления личными финансами.",
            project.description
        )
        assertEquals(
            listOf("Спроектировала многомодульную архитектуру"),
            project.responsibilities
        )
    }

    // ── Заголовки hh.ru ─────────────────────────────────────────────────

    @Test
    fun `hh headings are normalized to own template`() {
        val text = """
            Иванов Иван
            Android-разработчик
            ivan@example.com • Москва
            Опыт работы — 7 лет
            Знание языков
            Русский — Родной
            Ключевые навыки
            Kotlin • Docker
        """.trimIndent()

        val resume = PdfImportParser.parseResumeFromText(text)
        val p = resume.personalInfo

        // «Опыт работы — 7 лет» → «Опыт: 7 лет» → чистое число
        assertEquals("7", p.totalExperience)
        assertEquals("Иванов Иван", p.fullName)
        assertEquals("Android-разработчик", p.specialization)
        assertEquals("ivan@example.com", p.email)
        // «Знание языков» → «Языки»: секция распозналась
        assertEquals(listOf(Language("Русский", "Родной")), p.languages)
    }

    // ── Встроенные JSON-метаданные ──────────────────────────────────────

    @Test
    fun `embedded json metadata wins over text heuristics`() {
        val original = sampleResume()
        val text = """
            Какой-то посторонний текст резюме, который не должен помешать.
            AUTCSV_JSON_START${Gson().toJson(original)}
            AUTCSV_JSON_END
            Иванов Пустой Пустынин
        """.trimIndent()

        val parsed = PdfImportParser.parseResumeFromText(text)

        assertEquals(original.personalInfo, parsed.personalInfo)
        assertEquals(original.professionalSkills, parsed.professionalSkills)
        assertEquals(original.projects, parsed.projects)
        assertEquals(original.summary, parsed.summary)
        assertEquals(original.status, parsed.status)
        assertEquals(original.isFavorite, parsed.isFavorite)
        // Идентификатор всегда новый — импорт не конфликтует с существующим резюме
        assertNotEquals(original.id, parsed.id)
        assertTrue(parsed.id.isNotBlank())
    }

    @Test
    fun `base64 embedded metadata survives line breaks between chunks`() {
        val original = sampleResume()
        val json = Gson().toJson(original)
        val encoded = java.util.Base64.getEncoder().encodeToString(json.toByteArray(Charsets.UTF_8))
        // Извлекатель текста вставляет переносы между кусками отрисовки
        val chunked = "AUTCSV_JSON_START" + encoded.chunked(160).joinToString("\n") + "AUTCSV_JSON_END"
        val text = "Мусорный заголовок\n$chunked\nХвост"

        val parsed = PdfImportParser.parseResumeFromText(text)

        assertEquals(original.personalInfo, parsed.personalInfo)
        assertEquals(original.professionalSkills, parsed.professionalSkills)
        assertNotEquals(original.id, parsed.id)
    }

    @Test
    fun `legacy plain json with injected newlines is recovered`() {
        val original = sampleResume()
        val json = Gson().toJson(original)
        val chunked = "AUTCSV_JSON_START" + json.chunked(350).joinToString("\n") + "AUTCSV_JSON_END"

        val parsed = PdfImportParser.parseResumeFromText("Мусор\n$chunked\nХвост")

        assertEquals(original.personalInfo, parsed.personalInfo)
    }

    @Test
    fun `framed base64 metadata survives interleaved visible content`() {
        val original = sampleResume()
        val json = Gson().toJson(original)
        val encoded = java.util.Base64.getEncoder().encodeToString(json.toByteArray(Charsets.UTF_8))
        val frames = encoded.chunked(160)
            .mapIndexed { index, chunk -> "#A#${index.toString().padStart(3, '0')}#$chunk" }
        // Строки метаданных перемежаются видимым контентом страницы,
        // часть мусора приклеивается к строке с тегом слева
        val noisy = frames.flatMapIndexed { index, frame ->
            listOf("Проект 1: Кошелёк • СтафИИ • стр. 2", frame, "• Спроектировала архитектуру")
        }.joinToString("\n")

        val parsed = PdfImportParser.parseResumeFromText(noisy)

        assertEquals(original.personalInfo, parsed.personalInfo)
        assertEquals(original.projects, parsed.projects)
    }

    @Test
    fun `metadata layer lines are stripped from sections on fallback`() {
        // Метаданные не декодируются (пропущен кусок) — текстовый разбор
        // не должен видеть base64-строки в контенте секций
        val encoded = java.util.Base64.getEncoder()
            .encodeToString("{\"fullName\":\"Тест\"}".toByteArray(Charsets.UTF_8))
        val text = """
            Иванова Анна
            Разработчик
            Проектный опыт
            Проект 1: Кошелёк
            Роль: Lead
            Приложение для управления финансами.
            #A#000#$encoded
            #A#002#${encoded.take(50)}
        """.trimIndent()

        val resume = PdfImportParser.parseResumeFromText(text)
        val project = resume.projects.single()
        assertTrue(!project.description.contains("#A#"))
        assertTrue(project.description.contains("Приложение для управления финансами."))
    }

    @Test
    fun `broken json metadata falls back to text parsing`() {
        val text = """
            Иванова Анна Сергеевна
            Android-разработчик
            anna@example.com
            AUTCSV_JSON_START{это не json
            AUTCSV_JSON_END
            О себе
            Текст после битых метаданных.
        """.trimIndent()

        val resume = PdfImportParser.parseResumeFromText(text)
        assertEquals("Иванова Анна Сергеевна", resume.personalInfo.fullName)
        assertEquals("Текст после битых метаданных.", resume.personalInfo.aboutMe)
    }

    // ── Эвристики навыков ───────────────────────────────────────────────

    @Test
    fun `uppercase words without separators split and rejoin compounds`() {
        val text = """
            Иванова Анна
            Разработчик
            Технические навыки
            Языки программирования
            Kotlin REST API Java
            Библиотеки
            Python Celery Beat Django
        """.trimIndent()

        val skills = PdfImportParser.parseResumeFromText(text).professionalSkills
        assertEquals(listOf("Kotlin", "REST API", "Java"), skills.programmingLanguages)
        assertEquals(listOf("Python", "Celery Beat", "Django"), skills.libraries)
    }

    @Test
    fun `spread letters are collapsed back into words`() {
        val text = """
            Иванова Анна
            Разработчик
            Технические навыки
            Языки программирования
            K o t l i n
        """.trimIndent()

        val skills = PdfImportParser.parseResumeFromText(text).professionalSkills
        assertEquals(listOf("Kotlin"), skills.programmingLanguages)
    }

    @Test
    fun `chips are deduplicated case-insensitively`() {
        val text = """
            Иванова Анна
            Разработчик
            Технические навыки
            Фреймворки
            Ktor • ktor • KTor
        """.trimIndent()

        val skills = PdfImportParser.parseResumeFromText(text).professionalSkills
        assertEquals(listOf("Ktor"), skills.frameworks)
    }

    @Test
    fun `parenthesized chip content expands into two tokens`() {
        val text = """
            Иванова Анна
            Разработчик
            Технические навыки
            Базы данных
            PostgreSQL (AlloyDB)
        """.trimIndent()

        val skills = PdfImportParser.parseResumeFromText(text).professionalSkills
        assertEquals(listOf("PostgreSQL", "AlloyDB"), skills.databases)
    }

    // ── Опыт из свободного текста ───────────────────────────────────────

    @Test
    fun `experience extracted from running text when header silent`() {
        val text = """
            Иванова Анна
            Android-разработчик
            anna@example.com
            О себе
            Работаю 5 лет в мобильной разработке, из них 3 года в Android.
        """.trimIndent()

        val p = PdfImportParser.parseResumeFromText(text).personalInfo
        assertEquals("5", p.totalExperience)
        assertEquals("3", p.specializationExperience)
    }

    // ── Иностранные языки: только языковая информация ───────────────────

    @Test
    fun `non-language lines never reach the languages field`() {
        val text = """
            Иванова Анна
            Разработчик
            Опыт: 5 лет
            Языки
            Русский — Родной
            Желаемая зарплата: от 100 000 до 150 000 ₽/мес
            Релокация: Готов к переезду — города: Казань
            +7 900 123-45-67
            https://github.com/anna
        """.trimIndent()

        val languages = PdfImportParser.parseResumeFromText(text).personalInfo.languages
        // Только настоящие языки, остальное отфильтровано
        assertEquals(listOf(Language("Русский", "Родной")), languages)
    }

    @Test
    fun `skills subsection heading does not open the languages section`() {
        val text = """
            Иванова Анна
            Разработчик
            Технические навыки
            Языки программирования
            Kotlin • Java
            Языки
            Английский — B2
        """.trimIndent()

        val resume = PdfImportParser.parseResumeFromText(text)
        // «Языки программирования» не должен распознаться как заголовок секции языков,
        // а навыки не должны утечь в поле «Иностранные языки»
        assertEquals(listOf("Kotlin", "Java"), resume.professionalSkills.programmingLanguages)
        assertEquals(listOf(Language("Английский", "B2")), resume.personalInfo.languages)
    }

    @Test
    fun `reordered sections do not leak into languages`() {
        val text = """
            Иванова Анна
            Разработчик
            Языки
            Русский — Родной
            Образование
            СПбГУ, Программная инженерия, Высшее (бакалавриат)
            Технические навыки
            Kotlin • Docker
        """.trimIndent()

        val p = PdfImportParser.parseResumeFromText(text).personalInfo
        assertEquals(listOf(Language("Русский", "Родной")), p.languages)
    }

    // ── Несколько образований ───────────────────────────────────────────

    @Test
    fun `education section parses into multiple entries`() {
        val text = """
            Иванова Анна
            Разработчик
            Образование
            ИТМО, Программная инженерия, Высшее (бакалавриат)
            НИУ ВШЭ, Data Science и анализ данных, Дополнительное
        """.trimIndent()

        val educations = PdfImportParser.parseResumeFromText(text).personalInfo.educations
        assertEquals(2, educations.size)
        assertEquals(
            EducationEntry(level = "Высшее (бакалавриат)", specialty = "Программная инженерия", institution = "ИТМО"),
            educations[0]
        )
        assertEquals(
            EducationEntry(level = "Дополнительное", specialty = "Data Science и анализ данных", institution = "НИУ ВШЭ"),
            educations[1]
        )
    }

    // ── Шапка: телефон и возраст ────────────────────────────────────────

    @Test
    fun `salary amount is never taken as phone`() {
        val text = """
            Иванова Анна Сергеевна
            Senior Android-разработчик
            Опыт: 7 лет | Опыт по специализации: 5 лет
            anna@example.com • Санкт-Петербург
            Желаемая зарплата: от 200 000 до 300 000 ₽/мес
            Языки
            Русский — Родной
        """.trimIndent()

        val p = PdfImportParser.parseResumeFromText(text).personalInfo
        assertEquals("", p.phone)
        assertEquals("anna@example.com", p.email)
    }

    @Test
    fun `real phone numbers are still recognized`() {
        val text = """
            Иванова Анна
            Разработчик
            +7 921 123-45-67 • 8 900 752-26-33 • Москва
            Языки
            Русский — Родной
        """.trimIndent()

        val p = PdfImportParser.parseResumeFromText(text).personalInfo
        assertEquals("+7 921 123-45-67", p.phone)
    }

    @Test
    fun `age line is not taken as experience`() {
        val text = """
            Петров Пётр
            Backend-разработчик
            Мужчина, 34 года, родился 1 мая 1990
            petr@example.com • Москва
            Опыт работы — 6 лет
        """.trimIndent()

        val p = PdfImportParser.parseResumeFromText(text).personalInfo
        assertEquals("6", p.totalExperience)
    }

    // ── Внешние резюме: алиасы заголовков и плоские навыки ──────────────

    @Test
    fun `obo mne heading maps to about me`() {
        val text = """
            Иванов Иван
            Разработчик
            Обо мне
            Люблю чистый код.
            Образование
            МГУ, 2019
        """.trimIndent()

        val resume = PdfImportParser.parseResumeFromText(text)
        assertEquals("Люблю чистый код.", resume.personalInfo.aboutMe)
    }

    @Test
    fun `flat skill chips without categories land in other technologies`() {
        val text = """
            Иванов Иван
            Разработчик
            Ключевые навыки
            Kotlin
            Docker
            Jetpack Compose
        """.trimIndent()

        val skills = PdfImportParser.parseResumeFromText(text).professionalSkills
        assertEquals(listOf("Kotlin", "Docker", "Jetpack Compose"), skills.otherTechnologies)
    }

    @Test
    fun `education junk lines and bare years are filtered`() {
        val text = """
            Анна
            Разработчик
            Образование
            ИТМО, Программная инженерия, Высшее (бакалавриат)
            МГУ, 2019
            СтафИИ • 19.08.2026 • страница 1, сформировано приложением
        """.trimIndent()

        val educations = PdfImportParser.parseResumeFromText(text).personalInfo.educations
        assertEquals(2, educations.size)
        assertEquals("МГУ", educations[1].institution)
        assertEquals("", educations[1].specialty) // голый год не становится специальностью
    }

    @Test
    fun `language level in parentheses is recognized`() {
        val text = """
            Анна
            Разработчик
            Языки
            Русский (Родной)
            Английский (B2)
        """.trimIndent()

        val languages = PdfImportParser.parseResumeFromText(text).personalInfo.languages
        assertEquals(
            listOf(Language("Русский", "Родной"), Language("Английский", "B2")),
            languages
        )
    }

    // ── Подписанные строки шапки: условия и соцсети ─────────────────────

    @Test
    fun `labeled header lines fill salary relocation employment socials`() {
        val text = """
            Иванова Анна Сергеевна
            Senior Android-разработчик
            Опыт: 7 лет | Опыт по специализации: 5 лет
            anna@example.com • +7 921 123-45-67 • Санкт-Петербург
            GitHub: github.com/anna • Telegram: @anna_dev
            Желаемая зарплата: от 200 000 до 300 000 ₽/мес
            Релокация: Готова к переезду · города: Казань
            Занятость: Полная занятость • График: Удалённая работа
            Языки
            Русский — Родной
        """.trimIndent()

        val p = PdfImportParser.parseResumeFromText(text).personalInfo
        assertEquals("200000", p.salaryMin)
        assertEquals("300000", p.salaryMax)
        assertEquals("Готова к переезду", p.readyToRelocate)
        assertEquals("Казань", p.relocationCities)
        assertEquals("Полная занятость", p.employment)
        assertEquals("Удалённая работа", p.workSchedule)
        assertTrue(p.socialLinks.any { it.platform == "GitHub" && it.url.contains("github.com/anna") })
        assertTrue(p.socialLinks.any { it.platform == "Telegram" && it.url == "@anna_dev" })
    }

    @Test
    fun `email local part is not mistaken for telegram nick`() {
        val text = """
            Иванова Анна
            Разработчик
            contact: example@outlook.com
            Языки
            Русский — Родной
        """.trimIndent()

        val p = PdfImportParser.parseResumeFromText(text).personalInfo
        assertTrue(p.socialLinks.isEmpty())
        assertEquals("example@outlook.com", p.email)
    }

    // ── Вспомогательное ─────────────────────────────────────────────────

    private fun sampleResume(): Resume = Resume(
        id = "original-id",
        personalInfo = PersonalInfo(
            fullName = "Петров Пётр Петрович",
            specialization = "Backend-разработчик",
            totalExperience = "6",
            specializationExperience = "4",
            education = "МФТИ, Прикладная математика",
            languages = listOf(Language("Английский", "B2")),
            email = "petr@example.com",
            phone = "+7 900 000-00-00",
            location = "Москва",
            aboutMe = "Пишу бэкенд на Go.",
            salaryMin = "200000",
            salaryMax = "300000",
            readyToRelocate = "Готов к переезду",
            relocationCities = "Казань",
            employment = "Полная занятость",
            workSchedule = "Удалённая работа",
            socialLinks = listOf(SocialLink("GitHub", "github.com/petr")),
        ),
        professionalSkills = ProfessionalSkills(
            operatingSystems = listOf("Linux"),
            programmingLanguages = listOf("Go", "Python"),
            frameworks = listOf("FastAPI"),
            libraries = listOf("sqlalchemy"),
            databases = listOf("PostgreSQL"),
            otherTechnologies = listOf("Docker"),
            certifications = listOf("AWS Certified Solutions Architect"),
            softSkills = listOf("Работа в команде"),
            professionalAchievements = listOf("Снизил p95 до 40 мс"),
        ),
        projects = listOf(
            Project(
                name = "Платёжный шлюз",
                role = "Senior Go-разработчик",
                duration = "2022 — 2024",
                description = "Высоконагруженный сервис.",
                technologies = listOf("Go", "Kafka"),
                responsibilities = listOf("Спроектировал архитектуру"),
                teamSize = "5",
            )
        ),
        summary = "Backend-разработчик с 6-летним опытом.",
        aiAnalysis = AiAnalysis(completenessScore = 90),
        lastModified = 1717171717000L,
        status = CandidateStatus.OFFER,
        isFavorite = true,
    )
}
