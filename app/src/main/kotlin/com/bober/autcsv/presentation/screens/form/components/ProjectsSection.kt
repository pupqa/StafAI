package com.bober.autcsv.presentation.screens.form.components

import androidx.compose.foundation.layout.Arrangement
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
import com.bober.autcsv.presentation.screens.form.ProjectFormState
import com.bober.autcsv.presentation.screens.form.ResumeFormEvent
import com.bober.autcsv.presentation.screens.form.ResumeFormViewModel

@Composable
fun ProjectsSection(
    viewModel: ResumeFormViewModel = hiltViewModel(),
    projects: List<ProjectFormState>,
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
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.projects),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            state.projects.forEachIndexed { index, project ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(0.dp)
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
                                    LlmLogger.logUserAction(
                                        "Delete project",
                                        "Form",
                                        "index: $index"
                                    )
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
                                LlmLogger.logFormEvent(
                                    "ProjectNameChanged",
                                    "project[$index].name",
                                    it
                                )
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
                                LlmLogger.logFormEvent(
                                    "ProjectRoleChanged",
                                    "project[$index].role",
                                    it
                                )
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
                                LlmLogger.logFormEvent(
                                    "ProjectDurationChanged",
                                    "project[$index].duration",
                                    it
                                )
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
                                LlmLogger.logFormEvent(
                                    "ProjectDescriptionChanged",
                                    "project[$index].description",
                                    it
                                )
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
                                LlmLogger.logFormEvent(
                                    "ProjectTechnologiesChanged",
                                    "project[$index].technologies",
                                    it
                                )
                                viewModel.onEvent(
                                    ResumeFormEvent.ProjectTechnologiesChanged(
                                        index,
                                        it
                                    )
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
                                LlmLogger.logFormEvent(
                                    "ProjectTeamSizeChanged",
                                    "project[$index].teamSize",
                                    it
                                )
                                viewModel.onEvent(
                                    ResumeFormEvent.ProjectTeamSizeChanged(
                                        index,
                                        it
                                    )
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
                                        LlmLogger.logFormEvent(
                                            "ProjectResponsibilityChanged",
                                            "project[$index].responsibility[$respIndex]",
                                            it
                                        )
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
                                        LlmLogger.logUserAction(
                                            "Delete project responsibility",
                                            "Form",
                                            "project: $index, resp: $respIndex"
                                        )
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
                                LlmLogger.logUserAction(
                                    "Add project responsibility",
                                    "Form",
                                    "project: $index"
                                )
                                viewModel.onEvent(
                                    ResumeFormEvent.AddProjectResponsibility(
                                        index
                                    )
                                )
                            },
                            modifier = Modifier.align(Alignment.End),
                            shape = RoundedCornerShape(12.dp)
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
                modifier = Modifier.align(Alignment.End),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.project))
            }
        }
    }
}