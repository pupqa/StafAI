package com.bober.autcsv.core.pdf

import android.graphics.pdf.PdfDocument
import com.bober.autcsv.domain.model.AiAnalysis
import com.bober.autcsv.domain.model.Language
import com.bober.autcsv.domain.model.PersonalInfo
import com.bober.autcsv.domain.model.ProfessionalSkills
import com.bober.autcsv.domain.model.Project
import com.bober.autcsv.domain.model.Resume
import com.bober.autcsv.domain.model.SocialLink
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

/**
 * Превью-харнесс: генерирует PNG-слепки страниц для всех четырёх стилей
 * на насыщенном тестовом резюме. Это НЕ тест корректности — инструмент
 * визуальной проверки вёрстки без запуска эмулятора.
 *
 * PNG кладутся в app/build/pdf-preview/.
 */
class PdfPreviewHarness {

    @Test
    fun renderAllStyles() {
        val outDir = File("build/pdf-preview").apply { mkdirs() }
        val resume = sampleResume()

        AndroidPdfTemplate.Style.entries.forEach { style ->
            val tmpPdf = File(outDir, "preview-${style.name}.pdf")
            AndroidPdfTemplate(
                com.bober.autcsv.core.export.TestStringsContext.context(),
                style,
            ).generate(resume, tmpPdf)
            PdfDocument.lastPages.forEachIndexed { index, image ->
                val png = File(outDir, "${style.name}-${index + 1}.png")
                ImageIO.write(image, "png", png)
                println("rendered ${png.absolutePath}")
            }
        }
    }

    private fun sampleResume(): Resume = Resume(
        id = "preview-sample",
        personalInfo = PersonalInfo(
            fullName = "Иванова Анна Сергеевна",
            specialization = "Senior Android-разработчик",
            totalExperience = "7",
            specializationExperience = "5",
            education = "СПбГУТ им. Бонч-Бруевича, Программная инженерия, Высшее (бакалавриат)",
            languages = listOf(
                Language("Русский", "Родной"),
                Language("Английский", "B2 — Средне-продвинутый"),
            ),
            email = "anna.ivanova@example.com",
            phone = "+7 921 123-45-67",
            location = "Санкт-Петербург",
            aboutMe = "Android-разработчик с фокусом на производительность и архитектуру. " +
                    "Люблю чистый код, код-ревью и наставничество джунов. В свободное время " +
                    "пишу технические статьи и выступаю на митапах.",
            salaryMin = "180000",
            salaryMax = "250000",
            readyToRelocate = "Готова к переезду",
            relocationCities = "Москва, Казань",
            socialLinks = listOf(
                SocialLink("Telegram", "@anna_dev"),
                SocialLink("GitHub", "github.com/anna-dev"),
                SocialLink("LinkedIn", "linkedin.com/in/anna-dev"),
            ),
        ),
        professionalSkills = ProfessionalSkills(
            operatingSystems = listOf("Android", "Linux", "macOS"),
            programmingLanguages = listOf("Kotlin", "Java", "Python", "SQL"),
            frameworks = listOf("Jetpack Compose", "Ktor", "Spring Boot"),
            libraries = listOf("Room", "Retrofit", "OkHttp", "Coil", "Hilt", "Coroutines"),
            databases = listOf("PostgreSQL", "SQLite", "Redis"),
            otherTechnologies = listOf("Docker", "Kubernetes", "Git", "CI/CD", "Gradle"),
            certifications = listOf("Google Associate Android Developer", "Kotlin Certified Professional"),
            softSkills = listOf("Коммуникабельность", "Работа в команде", "Наставничество", "Тайм-менеджмент"),
            professionalAchievements = listOf(
                "Снизила время холодного старта приложения с 4,2 с до 1,1 с",
                "Выстроила процесс код-ревью в команде из 6 разработчиков",
                "Спроектировала модульную архитектуру, ускорившую сборку в 3 раза",
            ),
        ),
        projects = listOf(
            Project(
                name = "Финтех-приложение «Кошелёк»",
                role = "Lead Android-разработчик",
                duration = "2023 — наст. время",
                description = "Приложение для управления личными финансами: 500 000 MAU, " +
                        "рейтинг 4,7 в Google Play. Отвечала за архитектуру, производительность " +
                        "и релизный цикл.",
                technologies = listOf("Kotlin", "Jetpack Compose", "Room", "Hilt", "Retrofit"),
                responsibilities = listOf(
                    "Спроектировала многомодульную архитектуру на Gradle",
                    "Перевела UI с XML на Compose без остановки разработки фич",
                    "Настроила CI/CD и автоматизировала распределённые сборки",
                ),
                teamSize = "8",
            ),
            Project(
                name = "SDK аналитики для мобильных приложений",
                role = "Android-разработчик",
                duration = "2021 — 2023",
                description = "Белое лейбл SDK для сбора продуктовой аналитики с офлайн-очередью, " +
                        "используется в 40+ приложениях компании.",
                technologies = listOf("Kotlin", "Coroutines", "SQLite", "gRPC"),
                responsibilities = listOf(
                    "Реализовала надёжную офлайн-очередь событий на SQLite",
                    "Снизила потребление батареи SDK на 35%",
                ),
                teamSize = "4",
            ),
        ),
        summary = "Android-разработчик с 7-летним опытом. Строю архитектуру продуктов " +
                "с миллионной аудиторией, вывожу метрики производительности, менторю команду. " +
                "Ищу продукт с сильной инженерной культурой.",
        aiAnalysis = AiAnalysis(),
    )
}
