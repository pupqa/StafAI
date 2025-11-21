package com.bober.autcsv.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.toRoute
import com.bober.autcsv.presentation.screens.analysis.AutAnalysisScreen
import com.bober.autcsv.presentation.screens.dashboard.AutDashboardScreen
import com.bober.autcsv.presentation.screens.form.ResumeFormScreen
import com.bober.autcsv.presentation.screens.list.ResumeListScreen
import com.bober.autcsv.presentation.screens.preview.ResumePreviewScreen
import com.bober.autcsv.presentation.screens.splash.SplashScreen

/**
 * Центральная конфигурация навигации Compose: объявляет все экраны, аргументы
 * и переходы между ними.
 */
@Composable
fun Navigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = SplashRoute
    ) {
        composable<SplashRoute> {
            SplashScreen(
                onNavigateToMain = {
                    navController.navigate(ResumeListRoute)
                }
            )
        }

        composable<ResumeListRoute> {
            ResumeListScreen(
                onNavigateToForm = {
                    navController.navigate(ResumeFormRoute)
                },
                onNavigateToPreview = { resumeId ->
                    navController.navigate(ResumePreviewRoute(resumeId))
                },
                onNavigateToDashboard = {
                    navController.navigate(DashboardRoute)
                },
                onNavigateToFormEdit = { resumeId ->
                    navController.navigate(ResumeFormEditRoute(resumeId))
                }
            )
        }

        composable<ResumeFormRoute> {
            ResumeFormScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPreview = { resumeId ->
                    navController.navigate(ResumePreviewRoute(resumeId)) {
                        popUpTo<ResumeListRoute>()
                    }
                }
            )
        }

        composable<ResumeFormEditRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ResumeFormEditRoute>()
            ResumeFormScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPreview = { id ->
                    navController.navigate(ResumePreviewRoute(id)) {
                        popUpTo<ResumeListRoute>()
                    }
                }
            )
        }

        composable<ResumePreviewRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ResumePreviewRoute>()
            ResumePreviewScreen(
                resumeId = route.resumeId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAnalysis = { resumeId ->
                    navController.navigate(AnalysisRoute(resumeId))
                }
            )
        }

        composable<AnalysisRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<AnalysisRoute>()
            AutAnalysisScreen(
                resumeId = route.resumeId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEdit = { id ->
                    navController.navigate(ResumeFormEditRoute(id))
                }
            )
        }

        composable<DashboardRoute> {
            AutDashboardScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToForm = {
                    navController.navigate(ResumeFormRoute)
                },
                onNavigateToPreview = { resumeId ->
                    navController.navigate(ResumePreviewRoute(resumeId))
                }
            )
        }
    }
}