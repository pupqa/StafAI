package com.bober.autcsv.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.bober.autcsv.presentation.screens.analysis.AutAnalysisScreen
import com.bober.autcsv.presentation.screens.coverletter.CoverLetterScreen
import com.bober.autcsv.presentation.screens.dashboard.AutDashboardScreen
import com.bober.autcsv.presentation.screens.form.ResumeFormScreen
import com.bober.autcsv.presentation.screens.list.ResumeListScreen
import com.bober.autcsv.presentation.screens.preview.ResumePreviewScreen
import com.bober.autcsv.presentation.screens.settings.AboutScreen
import com.bober.autcsv.presentation.screens.settings.HelpScreen
import com.bober.autcsv.presentation.screens.settings.PrivacyPolicyScreen
import com.bober.autcsv.presentation.screens.settings.SettingsScreen
import com.bober.autcsv.presentation.screens.splash.SplashScreen
import com.bober.autcsv.presentation.screens.trash.TrashScreen

@Composable
fun Navigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = SplashRoute,
        // Единая схема переходов: вперёд — выезд справа, назад — зеркально;
        // дефолтный кроссфейд воспринимается как «дёрганье» между экранами
        enterTransition = {
            androidx.compose.animation.slideInHorizontally(
                animationSpec = androidx.compose.animation.core.tween(240)
            ) { it / 4 } + androidx.compose.animation.fadeIn(
                androidx.compose.animation.core.tween(200)
            )
        },
        exitTransition = {
            androidx.compose.animation.slideOutHorizontally(
                animationSpec = androidx.compose.animation.core.tween(200)
            ) { -it / 6 } + androidx.compose.animation.fadeOut(
                androidx.compose.animation.core.tween(160)
            )
        },
        popEnterTransition = {
            androidx.compose.animation.slideInHorizontally(
                animationSpec = androidx.compose.animation.core.tween(240)
            ) { -it / 6 } + androidx.compose.animation.fadeIn(
                androidx.compose.animation.core.tween(200)
            )
        },
        popExitTransition = {
            androidx.compose.animation.slideOutHorizontally(
                animationSpec = androidx.compose.animation.core.tween(200)
            ) { it / 4 } + androidx.compose.animation.fadeOut(
                androidx.compose.animation.core.tween(160)
            )
        },
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
                },
                onNavigateToSettings = {
                    navController.navigate(SettingsRoute)
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
                },
                onNavigateToCoverLetter = {
                    navController.navigate(CoverLetterRoute(route.resumeId))
                }
            )
        }

        composable<CoverLetterRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<CoverLetterRoute>()
            CoverLetterScreen(
                onNavigateBack = {
                    navController.popBackStack()
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

        composable<SettingsRoute> {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToTrash = {
                    navController.navigate(TrashRoute)
                },
                onNavigateToHelp = {
                    navController.navigate(HelpRoute)
                },
                onNavigateToAbout = {
                    navController.navigate(AboutRoute)
                },
                onNavigateToPrivacyPolicy = {
                    navController.navigate(PrivacyPolicyRoute)
                }
            )
        }

        composable<HelpRoute> {
            HelpScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<AboutRoute> {
            AboutScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<PrivacyPolicyRoute> {
            PrivacyPolicyScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<TrashRoute> {
            TrashScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}