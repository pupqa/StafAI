package com.bober.autcsv.presentation.screens.form

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bober.autcsv.R
import com.bober.autcsv.core.utils.LlmLogger
import com.bober.autcsv.presentation.common.components.*
import com.bober.autcsv.presentation.screens.form.components.PersonalInfoSection

// Функция для правильной обработки строк с технологиями
private fun parseTechnologies(input: String): List<String> {
    return input.split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

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

            // Languages Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.languages),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    state.languages.forEachIndexed { index, language ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RoundedCorner(
                                value = language.name,
                                onValueChange = {
                                    LlmLogger.logFormEvent("LanguageNameChanged", "language[$index].name", it)
                                    viewModel.onEvent(
                                        ResumeFormEvent.LanguageNameChanged(
                                            index,
                                            it
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                labelText = stringResource(R.string.language_name),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            RoundedCorner(
                                value = language.level,
                                onValueChange = {
                                    LlmLogger.logFormEvent("LanguageLevelChanged", "language[$index].level", it)
                                    viewModel.onEvent(
                                        ResumeFormEvent.LanguageLevelChanged(
                                            index,
                                            it
                                        )
                                    )
                                },
                                labelText = "Уровень",
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )
                            IconButton(
                                onClick = {
                                    LlmLogger.logUserAction("Delete language", "Form", "index: $index")
                                    viewModel.onEvent(
                                        ResumeFormEvent.DeleteLanguage(
                                            index
                                        )
                                    )
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.cancel)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = { 
                            LlmLogger.logUserAction("Add language", "Form", null)
                            viewModel.onEvent(ResumeFormEvent.AddLanguage) 
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.languages))
                    }
                }
            }

            // Professional Skills Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.professional_skills),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    RoundedCorner(
                        value = state.operatingSystems,
                        onValueChange = {
                            LlmLogger.logFormEvent("OperatingSystemsChanged", "operatingSystems", it)
                            viewModel.onEvent(
                                ResumeFormEvent.OperatingSystemsChanged(it)
                            )
                        },
                        labelText = stringResource(R.string.operating_systems),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    RoundedCorner(
                        value = state.programmingLanguages,
                        onValueChange = {
                            LlmLogger.logFormEvent("ProgrammingLanguagesChanged", "programmingLanguages", it)
                            viewModel.onEvent(
                                ResumeFormEvent.ProgrammingLanguagesChanged(it)
                            )
                        },
                        labelText = stringResource(R.string.programming_languages),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    RoundedCorner(
                        value = state.frameworks,
                        onValueChange = {
                            LlmLogger.logFormEvent("FrameworksChanged", "frameworks", it)
                            viewModel.onEvent(
                                ResumeFormEvent.FrameworksChanged(it)
                            )
                        },
                        labelText = stringResource(R.string.frameworks),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    RoundedCorner(
                        value = state.libraries,
                        onValueChange = {
                            LlmLogger.logFormEvent("LibrariesChanged", "libraries", it)
                            viewModel.onEvent(
                                ResumeFormEvent.LibrariesChanged(it)
                            )
                        },
                        labelText = stringResource(R.string.libraries),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    RoundedCorner(
                        value = state.databases,
                        onValueChange = {
                            LlmLogger.logFormEvent("DatabasesChanged", "databases", it)
                            viewModel.onEvent(
                                ResumeFormEvent.DatabasesChanged(it)
                            )
                        },
                        labelText = stringResource(R.string.databases),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    RoundedCorner(
                        value = state.otherTechnologies,
                        onValueChange = {
                            LlmLogger.logFormEvent("OtherTechnologiesChanged", "otherTechnologies", it)
                            viewModel.onEvent(
                                ResumeFormEvent.OtherTechnologiesChanged(it)
                            )
                        },
                        labelText = stringResource(R.string.other_technologies),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    RoundedCorner(
                        value = state.certifications,
                        onValueChange = {
                            LlmLogger.logFormEvent("CertificationsChanged", "certifications", it)
                            viewModel.onEvent(
                                ResumeFormEvent.CertificationsChanged(it)
                            )
                        },
                        labelText = stringResource(R.string.certifications),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    RoundedCorner(
                        value = state.softSkills,
                        onValueChange = {
                            LlmLogger.logFormEvent("SoftSkillsChanged", "softSkills", it)
                            viewModel.onEvent(
                                ResumeFormEvent.SoftSkillsChanged(it)
                            )
                        },
                        labelText = stringResource(R.string.soft_skills),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    state.professionalAchievements.forEachIndexed { index, achievement ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RoundedCorner(
                                value = achievement,
                                onValueChange = {
                                    LlmLogger.logFormEvent("ProfessionalAchievementChanged", "achievement[$index]", it)
                                    viewModel.onEvent(
                                        ResumeFormEvent.ProfessionalAchievementChanged(
                                            index,
                                            it
                                        )
                                    )
                                },
                                labelText = stringResource(R.string.achievements),
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )
                            IconButton(
                                onClick = {
                                    LlmLogger.logUserAction("Delete achievement", "Form", "index: $index")
                                    viewModel.onEvent(
                                        ResumeFormEvent.DeleteProfessionalAchievement(
                                            index
                                        )
                                    )
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.cancel)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = { 
                            LlmLogger.logUserAction("Add achievement", "Form", null)
                            viewModel.onEvent(ResumeFormEvent.AddProfessionalAchievement) 
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.prof_achievements))
                    }
                }
            }

            // Projects Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.projects),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    state.projects.forEachIndexed { index, project ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${stringResource(R.string.project)} ${index + 1}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    IconButton(
                                        onClick = {
                                            LlmLogger.logUserAction("Delete project", "Form", "index: $index")
                                            viewModel.onEvent(
                                                ResumeFormEvent.DeleteProject(
                                                    index
                                                )
                                            )
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.cancel)
                                        )
                                    }
                                }
                                RoundedCorner(
                                    value = project.name,
                                    onValueChange = {
                                        LlmLogger.logFormEvent("ProjectNameChanged", "project[$index].name", it)
                                        viewModel.onEvent(
                                            ResumeFormEvent.ProjectNameChanged(
                                                index,
                                                it
                                            )
                                        )
                                    },
                                    labelText = stringResource(R.string.project_name),
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                RoundedCorner(
                                    value = project.role,
                                    onValueChange = {
                                        LlmLogger.logFormEvent("ProjectRoleChanged", "project[$index].role", it)
                                        viewModel.onEvent(
                                            ResumeFormEvent.ProjectRoleChanged(
                                                index,
                                                it
                                            )
                                        )
                                    },
                                    labelText = stringResource(R.string.role),
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                RoundedCorner(
                                    value = project.duration,
                                    onValueChange = {
                                        LlmLogger.logFormEvent("ProjectDurationChanged", "project[$index].duration", it)
                                        viewModel.onEvent(
                                            ResumeFormEvent.ProjectDurationChanged(
                                                index,
                                                it
                                            )
                                        )
                                    },
                                    labelText = stringResource(R.string.duration),
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                RoundedCorner(
                                    value = project.description,
                                    onValueChange = {
                                        LlmLogger.logFormEvent("ProjectDescriptionChanged", "project[$index].description", it)
                                        viewModel.onEvent(
                                            ResumeFormEvent.ProjectDescriptionChanged(
                                                index,
                                                it
                                            )
                                        )
                                    },
                                    labelText = stringResource(R.string.project_description),
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    maxLines = 5,
                                    singleLine = false
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                RoundedCorner(
                                    value = project.technologies,
                                    onValueChange = {
                                        LlmLogger.logFormEvent("ProjectTechnologiesChanged", "project[$index].technologies", it)
                                        viewModel.onEvent(
                                            ResumeFormEvent.ProjectTechnologiesChanged(
                                                index,
                                                it)
                                        )
                                    },
                                    labelText = stringResource(R.string.tech_stack),
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                RoundedCorner(
                                    value = project.teamSize,
                                    onValueChange = {
                                        LlmLogger.logFormEvent("ProjectTeamSizeChanged", "project[$index].teamSize", it)
                                        viewModel.onEvent(
                                            ResumeFormEvent.ProjectTeamSizeChanged(
                                                index,
                                                it)
                                        )
                                    },
                                    labelText = stringResource(R.string.project_team_size),
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                project.responsibilities.forEachIndexed { respIndex, responsibility ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RoundedCorner(
                                            value = responsibility,
                                            onValueChange = {
                                                LlmLogger.logFormEvent("ProjectResponsibilityChanged", "project[$index].responsibility[$respIndex]", it)
                                                viewModel.onEvent(
                                                    ResumeFormEvent.ProjectResponsibilityChanged(
                                                        index,
                                                        respIndex,
                                                        it
                                                    )
                                                )
                                            },
                                            labelText = stringResource(R.string.main_tasks),
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                                        )

                                        IconButton(
                                            onClick = {
                                                LlmLogger.logUserAction("Delete project responsibility", "Form", "project: $index, resp: $respIndex")
                                                viewModel.onEvent(
                                                    ResumeFormEvent.DeleteProjectResponsibility(
                                                        index,
                                                        respIndex
                                                    )
                                                )
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = stringResource(R.string.cancel)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                Button(
                                    onClick = {
                                        LlmLogger.logUserAction("Add project responsibility", "Form", "project: $index")
                                        viewModel.onEvent(
                                            ResumeFormEvent.AddProjectResponsibility(
                                                index
                                            )
                                        )
                                    },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.main_tasks))
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { 
                            LlmLogger.logUserAction("Add project", "Form", null)
                            viewModel.onEvent(ResumeFormEvent.AddProject) 
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.project))
                    }
                }
            }

            // Submit Button
            Button(
                onClick = { 
                    LlmLogger.logUserAction("Submit form", "Form", "Resume ID: ${state.resumeId}")
                    viewModel.onEvent(ResumeFormEvent.Submit) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = state.isValid && !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.save))
                }
            }

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