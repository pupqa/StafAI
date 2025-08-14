package com.bober.autcsv.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.bober.autcsv.presentation.screens.analysis.AutAnalysisScreen
import com.bober.autcsv.presentation.screens.dashboard.AutDashboardScreen
import com.bober.autcsv.presentation.screens.form.ResumeFormScreen
import com.bober.autcsv.presentation.screens.list.ResumeListScreen
import com.bober.autcsv.presentation.screens.preview.ResumePreviewScreen

/**
 * Центральная конфигурация навигации Compose: объявляет все экраны, аргументы
 * и переходы между ними.
 */
@Composable
fun Navigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.ResumeList.route
    ) {
        composable(Screen.ResumeList.route) {
            ResumeListScreen(
                onNavigateToForm = {
                    navController.navigate(Screen.ResumeForm.route)
                },
                onNavigateToPreview = { resumeId ->
                    navController.navigate(Screen.ResumePreview.createRoute(resumeId))
                },
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route)
                },
                onNavigateToFormEdit = { resumeId ->
                    navController.navigate(Screen.ResumeFormEdit.createRoute(resumeId))
                }
            )
        }
        composable(Screen.ResumeForm.route) {
            ResumeFormScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPreview = { resumeId ->
                    navController.navigate(Screen.ResumePreview.createRoute(resumeId)) {
                        popUpTo(Screen.ResumeList.route)
                    }
                }
            )
        }
        composable(
            route = Screen.ResumeFormEdit.route,
            arguments = listOf(
                navArgument("resumeId") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val resumeId = backStackEntry.arguments?.getString("resumeId")
            if (resumeId == null) {
                navController.popBackStack()
                return@composable
            }
            ResumeFormScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPreview = { id ->
                    navController.navigate(Screen.ResumePreview.createRoute(id)) {
                        popUpTo(Screen.ResumeList.route)
                    }
                }
            )
        }
        composable(
            route = Screen.ResumePreview.route,
            arguments = listOf(
                navArgument("resumeId") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val resumeId = backStackEntry.arguments?.getString("resumeId")
            if (resumeId == null) {
                navController.popBackStack()
                return@composable
            }
            ResumePreviewScreen(
                resumeId = resumeId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAnalysis = { resumeId ->
                    navController.navigate(Screen.Analysis.createRoute(resumeId))
                }
            )
        }
        composable(
            route = Screen.Analysis.route,
            arguments = listOf(
                navArgument("resumeId") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val resumeId = backStackEntry.arguments?.getString("resumeId")
            if (resumeId == null) {
                navController.popBackStack()
                return@composable
            }
            AutAnalysisScreen(
                resumeId = resumeId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEdit = { id ->
                    navController.navigate(Screen.ResumeFormEdit.createRoute(id))
                }
            )
        }
        composable(Screen.Dashboard.route) {
            AutDashboardScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToForm = {
                    navController.navigate(Screen.ResumeForm.route)
                },
                onNavigateToPreview = { resumeId ->
                    navController.navigate(Screen.ResumePreview.createRoute(resumeId))
                }
            )
        }
    }
} 