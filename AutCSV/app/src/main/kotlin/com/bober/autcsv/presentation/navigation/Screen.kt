package com.bober.autcsv.presentation.navigation

sealed class Screen(val route: String) {
    object ResumeList : Screen("resume_list")
    object ResumeForm : Screen("resume_form")
    object ResumePreview : Screen("resume_preview/{resumeId}") {
        fun createRoute(resumeId: String) = "resume_preview/$resumeId"
    }
    object Analysis : Screen("analysis/{resumeId}") {
        fun createRoute(resumeId: String) = "analysis/$resumeId"
    }
    object Dashboard : Screen("dashboard")
} 