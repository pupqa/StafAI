package com.bober.autcsv.presentation.screens.form.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bober.autcsv.R
import com.bober.autcsv.presentation.screens.form.FormValidators
import com.bober.autcsv.presentation.screens.form.ProjectFormState
import com.bober.autcsv.presentation.screens.form.ResumeFormEvent
import com.bober.autcsv.presentation.screens.form.SuggestionDictionary
import com.bober.autcsv.ui.theme.AppColors
import com.bober.autcsv.ui.theme.MD

/**
 * Раздел «Проекты»: stateless, каждая карточка проекта изолирована key().
 *
 * Структура карточки повторяет практику LinkedIn/hh.ru: название, роль,
 * период, описание, стек тегами с автодополнением, задачи по одной на строку,
 * размер команды и ссылка на репозиторий/демо с валидацией.
 */
@Composable
fun ProjectsSection(
    projects: List<ProjectFormState>,
    onEvent: (ResumeFormEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    StaffAiSectionCard(modifier = modifier, title = stringResource(R.string.projects)) {
        Column {
            if (projects.isEmpty()) {
                Text(
                    text = stringResource(R.string.projects_empty_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextDim,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            projects.forEachIndexed { index, project ->
                key(index) {
                    ProjectCard(
                        index = index,
                        project = project,
                        onEvent = onEvent
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            StaffAiAddButton(
                text = stringResource(R.string.project),
                onClick = { onEvent(ResumeFormEvent.AddProject) },
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun ProjectCard(
    index: Int,
    project: ProjectFormState,
    onEvent: (ResumeFormEvent) -> Unit,
) {
    // Ошибка ссылки считается только для непустого значения; текст ошибки
    // резолвится в точке отображения, как в остальных секциях формы
    val linkErrorRes = remember(project.link) {
        FormValidators.validateField(FormValidators.Field.SOCIAL_URL, project.link)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(MD),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface2),
        border = BorderStroke(1.dp, AppColors.BorderSoft)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${stringResource(R.string.project)} ${index + 1}",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp)
                )
                StaffAiDeleteIconButton(
                    onClick = { onEvent(ResumeFormEvent.DeleteProject(index)) },
                    contentDescription = stringResource(R.string.delete)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            StaffAiTextField(
                value = project.name,
                onValueChange = { onEvent(ResumeFormEvent.ProjectNameChanged(index, it)) },
                labelText = stringResource(R.string.project_name),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            AutocompleteTextField(
                value = project.role,
                onValueChange = { onEvent(ResumeFormEvent.ProjectRoleChanged(index, it)) },
                labelText = stringResource(R.string.role),
                suggestions = SuggestionDictionary.positions,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            StaffAiTextField(
                value = project.duration,
                onValueChange = { onEvent(ResumeFormEvent.ProjectDurationChanged(index, it)) },
                labelText = stringResource(R.string.duration),
                hint = stringResource(R.string.project_duration_hint),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            StaffAiTextField(
                value = project.description,
                onValueChange = {
                    onEvent(ResumeFormEvent.ProjectDescriptionChanged(index, it))
                },
                labelText = stringResource(R.string.project_description),
                singleLine = false,
                maxLines = 5,
                tipsIntroRes = R.string.tips_project_description_intro,
                tipsBulletsRes = R.array.tips_project_description,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            TagInputField(
                value = project.technologies,
                onValueChange = {
                    onEvent(ResumeFormEvent.ProjectTechnologiesChanged(index, it))
                },
                labelText = stringResource(R.string.tech_stack),
                hint = stringResource(R.string.tech_stack_hint),
                suggestions = SuggestionDictionary.programmingLanguages +
                        SuggestionDictionary.frameworks +
                        SuggestionDictionary.libraries +
                        SuggestionDictionary.databases +
                        SuggestionDictionary.otherTechnologies,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            StaffAiTextField(
                value = project.responsibilitiesText,
                onValueChange = {
                    onEvent(ResumeFormEvent.ProjectResponsibilitiesChanged(index, it))
                },
                labelText = stringResource(R.string.main_tasks),
                hint = stringResource(R.string.project_tasks_hint),
                singleLine = false,
                maxLines = 6,
                tipsIntroRes = R.string.tips_project_tasks_intro,
                tipsBulletsRes = R.array.tips_project_tasks,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            StaffAiTextField(
                value = project.teamSize,
                onValueChange = { onEvent(ResumeFormEvent.ProjectTeamSizeChanged(index, it)) },
                labelText = stringResource(R.string.project_team_size),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            StaffAiTextField(
                value = project.link,
                onValueChange = { onEvent(ResumeFormEvent.ProjectLinkChanged(index, it)) },
                labelText = stringResource(R.string.project_link),
                hint = stringResource(R.string.project_link_hint),
                isError = linkErrorRes != null,
                errorText = linkErrorRes?.let { stringResource(it) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
