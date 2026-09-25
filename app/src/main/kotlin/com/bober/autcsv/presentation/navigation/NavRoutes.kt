package com.bober.autcsv.presentation.navigation

/**
 * Маршруты экранов приложения и хелперы для формирования путей с параметрами.
 */
import kotlinx.serialization.Serializable

@Serializable
object SplashRoute

@Serializable
object ResumeListRoute

@Serializable
object ResumeFormRoute

@Serializable
data class ResumeFormEditRoute(val resumeId: String)

@Serializable
data class ResumePreviewRoute(val resumeId: String)

@Serializable
data class AnalysisRoute(val resumeId: String)

@Serializable
object DashboardRoute

@Serializable
object SettingsRoute

@Serializable
object HelpRoute

@Serializable
object AboutRoute

@Serializable
object PrivacyPolicyRoute

@Serializable
data class CoverLetterRoute(val resumeId: String)

@Serializable
object TrashRoute