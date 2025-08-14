package com.bober.autcsv.core.pdf

import com.bober.autcsv.domain.model.*
import java.io.File

object PdfGeneratorTest {

    fun testAllTemplates(outputDirectory: String = "test_output") {
        val testResume = createTestResume()
        val outputDir = File(outputDirectory)
        outputDir.mkdirs()

        PdfTemplateType.values().forEach { templateType ->
            try {
                val template = templateType.getTemplate()
                val outputFile = File(outputDir, "test_resume_${templateType.name.lowercase()}.pdf")
                
                println("Testing ${templateType.name} template...")
                val result = template.generate(testResume, outputFile)
                println("✅ ${templateType.name} template generated successfully: $result")
                
            } catch (e: Exception) {
                println("❌ Error generating ${templateType.name} template: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun testSingleTemplate(templateType: PdfTemplateType, outputDirectory: String = "test_output") {
        val testResume = createTestResume()
        val outputDir = File(outputDirectory)
        outputDir.mkdirs()

        try {
            val template = templateType.getTemplate()
            val outputFile = File(outputDir, "test_resume_${templateType.name.lowercase()}.pdf")
            
            println("Testing ${templateType.name} template...")
            val result = template.generate(testResume, outputFile)
            println("✅ ${templateType.name} template generated successfully: $result")
            
        } catch (e: Exception) {
            println("❌ Error generating ${templateType.name} template: ${e.message}")
            e.printStackTrace()
        }
    }

    fun testEmptyResume(templateType: PdfTemplateType = PdfTemplateType.PROFESSIONAL) {
        val emptyResume = createEmptyResume()
        val outputDir = File("test_output")
        outputDir.mkdirs()

        try {
            val template = templateType.getTemplate()
            val outputFile = File(outputDir, "test_empty_resume_${templateType.name.lowercase()}.pdf")
            
            println("Testing ${templateType.name} template with empty resume...")
            val result = template.generate(emptyResume, outputFile)
            println("✅ Empty resume test completed: $result")
            
        } catch (e: Exception) {
            println("❌ Error testing empty resume: ${e.message}")
            e.printStackTrace()
        }
    }

    fun testResumeValidation() {
        val testResume = createTestResume()
        val emptyResume = createEmptyResume()

        println("=== Resume Validation Test ===")
        
        // Тест полного резюме
        val fullResult = com.bober.autcsv.core.utils.ResumeValidator.validateResume(testResume)
        println("Full Resume:")
        println("  Completeness: ${fullResult.completenessScore}%")
        println("  Is Valid: ${fullResult.isValid}")
        println("  Missing Fields: ${fullResult.missingFields}")
        println("  Suggestions: ${fullResult.suggestions}")
        println("  Warnings: ${fullResult.warnings}")

        // Тест пустого резюме
        val emptyResult = com.bober.autcsv.core.utils.ResumeValidator.validateResume(emptyResume)
        println("\nEmpty Resume:")
        println("  Completeness: ${emptyResult.completenessScore}%")
        println("  Is Valid: ${emptyResult.isValid}")
        println("  Missing Fields: ${emptyResult.missingFields}")
        println("  Suggestions: ${emptyResult.suggestions}")
        println("  Warnings: ${emptyResult.warnings}")

        // Тест уровней качества
        println("\nQuality Levels:")
        println("  Full Resume: ${com.bober.autcsv.core.utils.ResumeValidator.getResumeQualityLevel(testResume)}")
        println("  Empty Resume: ${com.bober.autcsv.core.utils.ResumeValidator.getResumeQualityLevel(emptyResume)}")

        // Тест рекомендаций
        println("\nTips for Full Resume:")
        com.bober.autcsv.core.utils.ResumeValidator.generateResumeTips(testResume).forEach { tip ->
            println("  $tip")
        }

        println("\nTips for Empty Resume:")
        com.bober.autcsv.core.utils.ResumeValidator.generateResumeTips(emptyResume).forEach { tip ->
            println("  $tip")
        }
    }

    private fun createTestResume(): Resume {
        return Resume(
            id = "test-resume-1",
            personalInfo = PersonalInfo(
                fullName = "Иван Петров",
                specialization = "Senior Android Developer",
                totalExperience = "8 лет",
                specializationExperience = "6 лет в Android разработке",
                education = "Московский технический университет, Информационные технологии",
                languages = listOf(
                    Language("Русский", "Родной"),
                    Language("Английский", "B2"),
                    Language("Немецкий", "A2")
                ),
                email = "ivan.petrov@example.com",
                phone = "+7 (999) 123-45-67",
                location = "Москва, Россия"
            ),
            professionalSkills = ProfessionalSkills(
                operatingSystems = listOf("Android", "Linux", "Windows", "macOS"),
                programmingLanguages = listOf("Kotlin", "Java"),
                frameworks = listOf("Android SDK", "Jetpack Compose"),
                libraries = listOf("Room Database", "Retrofit", "Dagger Hilt", "Coroutines"),
                databases = listOf(),
                otherTechnologies = listOf("MVVM", "Clean Architecture", "Git", "Gradle"),
                professionalAchievements = listOf(
                    "Разработал приложение с 1M+ загрузок в Google Play",
                    "Оптимизировал производительность приложения на 40%",
                    "Вел команду из 5 разработчиков в течение 2 лет",
                    "Получил 3 сертификации Google Developer"
                ),
                certifications = listOf(
                    "Google Associate Android Developer",
                    "Kotlin Certified Developer",
                    "Android Architecture Components"
                ),
                softSkills = listOf(
                    "Лидерство", "Коммуникация", "Решение проблем",
                    "Работа в команде", "Управление проектами"
                )
            ),
            projects = listOf(
                Project(
                    name = "E-commerce Mobile App",
                    role = "Lead Android Developer",
                    duration = "18 месяцев",
                    description = "Разработка мобильного приложения для крупного интернет-магазина с интеграцией платежных систем и аналитики",
                    technologies = listOf("Kotlin", "Jetpack Compose", "Room", "Retrofit", "Stripe API"),
                    responsibilities = listOf(
                        "Архитектура приложения",
                        "Руководство командой разработчиков",
                        "Интеграция с backend API",
                        "Оптимизация производительности"
                    ),
                    teamSize = "8 человек"
                ),
                Project(
                    name = "Banking App",
                    role = "Senior Android Developer",
                    duration = "12 месяцев",
                    description = "Разработка защищенного банковского приложения с биометрической аутентификацией",
                    technologies = listOf("Java", "Android SDK", "Biometric API", "Encryption"),
                    responsibilities = listOf(
                        "Реализация биометрической аутентификации",
                        "Шифрование данных",
                        "Интеграция с банковскими API",
                        "Тестирование безопасности"
                    ),
                    teamSize = "12 человек"
                )
            ),
            summary = "Опытный Android разработчик с 8-летним стажем в создании высоконагруженных мобильных приложений. Специализируюсь на Kotlin, Jetpack Compose и современных архитектурных паттернах. Имею опыт руководства командами и работы с крупными проектами в сферах e-commerce и banking.",
            aiAnalysis = AiAnalysis(
                completenessScore = 95,
                suggestedImprovements = listOf(
                    "Добавить больше метрик в достижения",
                    "Указать конкретные технологии для каждого проекта"
                ),
                strengthAreas = listOf(
                    "Богатый опыт в Android разработке",
                    "Лидерские качества",
                    "Разнообразный проектный опыт"
                ),
                weakAreas = listOf(
                    "Можно добавить больше сертификаций",
                    "Расширить языковые навыки"
                ),
                enhancedDescriptions = mapOf(
                    "summary" to "Улучшенное резюме с акцентом на лидерство и технические достижения"
                ),
                keywordSuggestions = listOf(
                    "Kotlin", "Jetpack Compose", "Android Architecture", "Team Leadership"
                ),
                lastAnalyzed = System.currentTimeMillis()
            ),
            lastModified = System.currentTimeMillis()
        )
    }

    private fun createEmptyResume(): Resume {
        return Resume(
            id = "empty-resume",
            personalInfo = PersonalInfo(
                fullName = "",
                specialization = "",
                totalExperience = "",
                specializationExperience = "",
                education = "",
                languages = emptyList(),
                email = "",
                phone = "",
                location = ""
            ),
            professionalSkills = ProfessionalSkills(
                operatingSystems = emptyList(),
                programmingLanguages = emptyList(),
                frameworks = emptyList(),
                libraries = emptyList(),
                databases = emptyList(),
                otherTechnologies = emptyList(),
                professionalAchievements = emptyList(),
                certifications = emptyList(),
                softSkills = emptyList()
            ),
            projects = emptyList(),
            summary = "",
            aiAnalysis = AiAnalysis(),
            lastModified = System.currentTimeMillis()
        )
    }
} 