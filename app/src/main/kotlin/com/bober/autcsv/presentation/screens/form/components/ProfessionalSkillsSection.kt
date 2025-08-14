package com.bober.autcsv.presentation.screens.form.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bober.autcsv.R
import com.bober.autcsv.core.utils.LlmLogger
import com.bober.autcsv.presentation.common.components.RoundedCorner
import com.bober.autcsv.presentation.screens.form.ResumeFormEvent
import com.bober.autcsv.presentation.screens.form.ResumeFormViewModel

@Composable
fun ProfessionalSkillsSection(
    viewModel: ResumeFormViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    operatingSystems: String,
    programmingLanguages: String,
    frameworks: String,
    libraries: String,
    databases: String,
    otherTechnologies: String,
    certifications: String,
    softSkills: String,
    professionalAchievements: List<String>,
) {
    val state by viewModel.state.collectAsState()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = stringResource(R.string.professional_skills),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            RoundedCorner(
                value = state.operatingSystems,
                onValueChange = {
                    LlmLogger.logFormEvent(
                        "OperatingSystemsChanged",
                        "operatingSystems",
                        it
                    )
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
                    LlmLogger.logFormEvent(
                        "ProgrammingLanguagesChanged",
                        "programmingLanguages",
                        it
                    )
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
                    LlmLogger.logFormEvent(
                        "OtherTechnologiesChanged",
                        "otherTechnologies",
                        it
                    )
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
                            LlmLogger.logFormEvent(
                                "ProfessionalAchievementChanged",
                                "achievement[$index]",
                                it
                            )
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
                            LlmLogger.logUserAction(
                                "Delete achievement",
                                "Form",
                                "index: $index"
                            )
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
                modifier = Modifier.align(Alignment.End),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.prof_achievements))
            }
        }
    }
}