package com.bober.autcsv.presentation.navigation

/**
 * Маршруты экранов приложения и хелперы для формирования путей с параметрами.
 */
sealed class Screen(val route: String) {
    object ResumeList : Screen("resume_list")
    object ResumeForm : Screen("resume_form")
    object ResumeFormEdit : Screen("resume_form/{resumeId}") {
        /** Формирует маршрут для редактирования резюме по ID. */
        fun createRoute(resumeId: String) = "resume_form/$resumeId"
    }

    object ResumePreview : Screen("resume_preview/{resumeId}") {
        /** Формирует маршрут предпросмотра резюме по ID. */
        fun createRoute(resumeId: String) = "resume_preview/$resumeId"
    }

    object Analysis : Screen("analysis/{resumeId}") {
        /** Формирует маршрут экрана анализа по ID резюме. */
        fun createRoute(resumeId: String) = "analysis/$resumeId"
    }

    object Dashboard : Screen("dashboard")
} 