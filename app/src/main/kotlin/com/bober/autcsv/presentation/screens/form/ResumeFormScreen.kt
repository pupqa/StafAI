package com.bober.autcsv.presentation.screens.form

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bober.autcsv.R
import com.bober.autcsv.core.utils.LlmLogger
import com.bober.autcsv.presentation.common.components.AdaptiveContent
import com.bober.autcsv.presentation.screens.form.components.DesiredJobSection
import com.bober.autcsv.presentation.screens.form.components.EducationSection
import com.bober.autcsv.presentation.screens.form.components.LanguagesSection
import com.bober.autcsv.presentation.screens.form.components.PersonalInfoSection
import com.bober.autcsv.presentation.screens.form.components.ProfessionalSkillsSection
import com.bober.autcsv.presentation.screens.form.components.ProjectsSection
import com.bober.autcsv.presentation.screens.form.components.SocialLinksSection
import com.bober.autcsv.presentation.screens.form.components.SoftSkillsSection
import com.bober.autcsv.presentation.screens.form.components.StaffAiIconButton
import com.bober.autcsv.presentation.screens.form.components.StaffAiTabRow
import com.bober.autcsv.presentation.screens.form.components.StaffAiTextAction
import com.bober.autcsv.presentation.screens.form.components.SubmitButton
import com.bober.autcsv.ui.theme.AppColors
import com.bober.autcsv.ui.theme.MD

private enum class FormTab(val labelResId: Int) {
    Personal(R.string.personal_info),
    Education(R.string.education),
    Languages(R.string.languages),
    Skills(R.string.skills),
    Projects(R.string.projects),
}

/**
 * Экран формы резюме: персональные данные, образование, языки, навыки и
 * проекты. Секции переключаются пилюльными вкладками; в шапке — прогресс
 * заполнения и индикатор автосохранения черновика.
 *
 * Все дочерние секции stateless и получают значения/события снаружи:
 * перерисовка ограничивается изменённой вкладкой.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeFormScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPreview: (String) -> Unit,
    viewModel: ResumeFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tabs = remember { FormTab.entries.toList() }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val onEvent = remember(viewModel) { { event: ResumeFormEvent -> viewModel.onEvent(event) } }

    // Возврат на список после успешного сохранения
    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            LlmLogger.logUiEvent("Form", "Navigate back after save", "Resume ID: ${state.resumeId}")
            onNavigateBack()
            viewModel.consumeSaveSuccess()
        }
    }

    Scaffold(
        containerColor = AppColors.BackgroundSoft,
        topBar = {
            // Фон остаётся full-bleed под статус-баром, контент — ниже него
            Column(
                modifier = Modifier
                    .background(AppColors.BackgroundSoft)
                    .statusBarsPadding()
            ) {
                AdaptiveContent {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 10.dp, end = 18.dp, top = 8.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StaffAiIconButton(
                                onClick = { onNavigateBack() },
                                contentDescription = stringResource(R.string.back),
                                icon = Icons.AutoMirrored.Filled.ArrowBack
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 10.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.nav_form),
                                    style = MaterialTheme.typography.titleLarge
                                )
                                // Прогресс заполнения, как в конструкторе hh;
                                // автосохранение работает бесшумно, без текста в шапке
                                val progressDescription = stringResource(
                                    R.string.form_progress_description, state.progressPercent
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LinearProgressIndicator(
                                        progress = { state.progressPercent / 100f },
                                        modifier = Modifier
                                            .width(120.dp)
                                            .height(4.dp)
                                            .semantics {
                                                contentDescription = progressDescription
                                            },
                                        color = AppColors.Accent,
                                        trackColor = AppColors.BorderSoft
                                    )
                                    Text(
                                        text = "${state.progressPercent}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppColors.TextDim,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                            StaffAiTextAction(
                                text = stringResource(R.string.save),
                                onClick = { viewModel.onEvent(ResumeFormEvent.Submit) }
                            )
                        }
                        StaffAiTabRow(
                            // Подпись с прогрессом вкладки: «Личное 5/7» (№23)
                            tabs = tabs.mapIndexed { index, tab ->
                                val (filled, total) = FormValidators.tabProgress(index, state)
                                "${stringResource(tab.labelResId)} $filled/$total"
                            },
                            selectedIndex = selectedTab,
                            onTabSelected = { selectedTab = it },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        },
        bottomBar = {
            // Контент панели поднят над жестовой навигационной панелью
            Column(
                modifier = Modifier
                    .background(AppColors.BackgroundSoft)
                    .navigationBarsPadding()
            ) {
                AdaptiveContent {
                    Column {
                        if (!state.isValid) {
                            // Баннер кликабелен: перекидывает на первую вкладку
                            // с незаполненными полями, не заставляя искать вручную
                            val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
                            val errorBanner = stringResource(R.string.required_field_all)
                            val errorHint = stringResource(R.string.form_errors_tap_hint)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        haptics.performHapticFeedback(
                                            androidx.compose.ui.hapticfeedback.HapticFeedbackType.Confirm
                                        )
                                        // indexOfFirst по индексам: tabProgress ждёт номер вкладки
                                        tabs.indices.firstOrNull { tabIndex ->
                                            val (filled, total) = FormValidators.tabProgress(
                                                tabIndex,
                                                state
                                            )
                                            filled < total
                                        }?.let { selectedTab = it }
                                    }
                                    .padding(horizontal = 24.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = errorBanner,
                                    color = AppColors.Danger,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = errorHint,
                                    color = AppColors.Danger.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        state.error?.let { error ->
                            LlmLogger.logError("Form error: $error")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .background(AppColors.DangerSoft, RoundedCornerShape(MD))
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(error, color = AppColors.TextPrimary, fontSize = 14.sp)
                                }
                                StaffAiTextAction(
                                    text = stringResource(R.string.retry),
                                    onClick = { viewModel.onEvent(ResumeFormEvent.Submit) }
                                )
                            }
                        }
                        SubmitButton(
                            enabled = state.isValid,
                            isLoading = state.isLoading,
                            onClick = { viewModel.onEvent(ResumeFormEvent.Submit) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedContent(
                targetState = tabs[selectedTab],
                label = "formTabContent",
                // Короткий кроссфейд без слайда: старая и новая вкладка
                // не накладываются друг на друга визуально
                transitionSpec = {
                    fadeIn(tween(170)) togetherWith fadeOut(tween(120))
                }
            ) { tab ->
                AdaptiveContent(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(bottom = 24.dp)
                    ) {
                        when (tab) {
                            FormTab.Personal -> Column {
                                PersonalInfoSection(
                                    fullName = state.fullName,
                                    specialization = state.specialization,
                                    totalExperience = state.totalExperience,
                                    specializationExperience = state.specializationExperience,
                                    email = state.email,
                                    phone = state.phone,
                                    aboutMe = state.aboutMe,
                                    location = state.location,
                                    photoUri = state.photoUri,
                                    resumeId = state.resumeId,
                                    aiBusy = state.aiBusy,
                                    onEvent = onEvent
                                )
                                DesiredJobSection(
                                    salaryMin = state.salaryMin,
                                    salaryMax = state.salaryMax,
                                    readyToRelocate = state.readyToRelocate,
                                    relocationCities = state.relocationCities,
                                    employment = state.employment,
                                    workSchedule = state.workSchedule,
                                    onEvent = onEvent
                                )
                                SocialLinksSection(
                                    socialLinks = state.socialLinks,
                                    onEvent = onEvent
                                )
                            }

                            FormTab.Education -> EducationSection(
                                educations = state.educations,
                                onEvent = onEvent
                            )

                            FormTab.Languages -> LanguagesSection(
                                languages = state.languages,
                                onEvent = onEvent
                            )

                            FormTab.Skills -> Column {
                                ProfessionalSkillsSection(
                                    operatingSystems = state.operatingSystems,
                                    programmingLanguages = state.programmingLanguages,
                                    frameworks = state.frameworks,
                                    libraries = state.libraries,
                                    databases = state.databases,
                                    otherTechnologies = state.otherTechnologies,
                                    certifications = state.certifications,
                                    professionalAchievements = state.professionalAchievements,
                                    onEvent = onEvent
                                )
                                SoftSkillsSection(
                                    softSkills = state.softSkills,
                                    onEvent = onEvent
                                )
                            }

                            FormTab.Projects -> ProjectsSection(
                                projects = state.projects,
                                onEvent = onEvent
                            )
                        }
                    }
                }
            }

            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppColors.BackgroundSoft.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.Accent)
                }
            }
        }
    }
}
