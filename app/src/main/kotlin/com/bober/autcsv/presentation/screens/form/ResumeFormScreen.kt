package com.bober.autcsv.presentation.screens.form

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bober.autcsv.R
import com.bober.autcsv.core.utils.LlmLogger
import com.bober.autcsv.presentation.screens.form.components.LanguagesSection
import com.bober.autcsv.presentation.screens.form.components.PersonalInfoSection
import com.bober.autcsv.presentation.screens.form.components.ProfessionalSkillsSection
import com.bober.autcsv.presentation.screens.form.components.ProjectsSection
import com.bober.autcsv.presentation.screens.form.components.SubmitButton

// Функция для правильной обработки строк с технологиями
private fun parseTechnologies(input: String): List<String> {
    return input.split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

/**
 * Экран формы резюме: персональные данные, языки, навыки, достижения, проекты,
 * валидация и сохранение. При успешном сохранении возвращается назад.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeFormScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPreview: (String) -> Unit,
    viewModel: ResumeFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Возврат на список после успешного сохранения
    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            LlmLogger.logUiEvent("Form", "Navigate back after save", "Resume ID: ${state.resumeId}")
            onNavigateBack()
            viewModel.consumeSaveSuccess()
        }
    }
    val scrollState = rememberScrollState()

    LlmLogger.logUiEvent("Form", "Screen rendered", "Resume ID: ${state.resumeId}")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_form)) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            LlmLogger.logUserAction("Back button pressed", "Form", state.resumeId)
                            onNavigateBack()
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            PersonalInfoSection(
                fullName = state.fullName,
                specialization = state.specialization,
                totalExperience = state.totalExperience,
                specializationExperience = state.specializationExperience,
                education = state.education,
                email = state.email,
                phone = state.phone,
                aboutMe = state.aboutMe,
                location = state.location,
                onEvent = { event ->
                    LlmLogger.logFormEvent(event.javaClass.simpleName, null, null)
                    viewModel.onEvent(event)
                }
            )
            LanguagesSection(
                languages = state.languages
            )
            ProfessionalSkillsSection(
                operatingSystems = state.operatingSystems,
                programmingLanguages = state.programmingLanguages,
                frameworks = state.frameworks,
                libraries = state.libraries,
                databases = state.databases,
                otherTechnologies = state.otherTechnologies,
                certifications = state.certifications,
                softSkills = state.softSkills,
                professionalAchievements = state.professionalAchievements
            )
            ProjectsSection(
                projects = state.projects
            )

            SubmitButton()

            if (!state.isValid) {
                LlmLogger.logUiEvent("Form", "Validation failed", "Form is not valid")
                Text(
                    text = stringResource(R.string.required_field_all),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 56.dp),
                    textAlign = TextAlign.Center
                )
            }

            state.error?.let { error ->
                LlmLogger.logError("Form error: $error")
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(
                            onClick = {
                                LlmLogger.logUserAction("Retry submit", "Form", null)
                                viewModel.onEvent(ResumeFormEvent.Submit)
                            }
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                ) {
                    Text(error)
                }
            }

            if (state.isLoading) {
                LlmLogger.logUiEvent("Form", "Loading state", "Form is being saved")
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
} 