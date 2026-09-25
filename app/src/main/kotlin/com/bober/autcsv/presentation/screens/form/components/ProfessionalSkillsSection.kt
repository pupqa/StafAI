package com.bober.autcsv.presentation.screens.form.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bober.autcsv.R
import com.bober.autcsv.presentation.screens.form.ResumeFormEvent
import com.bober.autcsv.presentation.screens.form.SuggestionDictionary

/**
 * Раздел «Технические навыки» (hard skills): stateless, значения каждой
 * подкатегории вводятся тегами через [TagInputField] (как гибкие навыки):
 * выбранные навыки отображаются снимаемыми чипами над полем ввода,
 * автодополнение — из справочника. Гибкие навыки вынесены в [SoftSkillsSection].
 */
@Composable
fun ProfessionalSkillsSection(
    operatingSystems: String,
    programmingLanguages: String,
    frameworks: String,
    libraries: String,
    databases: String,
    otherTechnologies: String,
    certifications: String,
    professionalAchievements: List<String>,
    onEvent: (ResumeFormEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    StaffAiSectionCard(
        modifier = modifier,
        title = stringResource(R.string.hard_skills)
    ) {
        Column {
            TagInputField(
                value = operatingSystems,
                onValueChange = { onEvent(ResumeFormEvent.OperatingSystemsChanged(it)) },
                labelText = stringResource(R.string.operating_systems),
                suggestions = SuggestionDictionary.operatingSystems,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(14.dp))
            TagInputField(
                value = programmingLanguages,
                onValueChange = { onEvent(ResumeFormEvent.ProgrammingLanguagesChanged(it)) },
                labelText = stringResource(R.string.programming_languages),
                hint = stringResource(R.string.programming_languages_hint),
                suggestions = SuggestionDictionary.programmingLanguages,
                tipsIntroRes = R.string.tips_hard_skills_intro,
                tipsBulletsRes = R.array.tips_hard_skills,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(14.dp))
            TagInputField(
                value = frameworks,
                onValueChange = { onEvent(ResumeFormEvent.FrameworksChanged(it)) },
                labelText = stringResource(R.string.frameworks),
                suggestions = SuggestionDictionary.frameworks,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(14.dp))
            TagInputField(
                value = libraries,
                onValueChange = { onEvent(ResumeFormEvent.LibrariesChanged(it)) },
                labelText = stringResource(R.string.libraries),
                suggestions = SuggestionDictionary.libraries,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(14.dp))
            TagInputField(
                value = databases,
                onValueChange = { onEvent(ResumeFormEvent.DatabasesChanged(it)) },
                labelText = stringResource(R.string.databases),
                suggestions = SuggestionDictionary.databases,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(14.dp))
            TagInputField(
                value = otherTechnologies,
                onValueChange = { onEvent(ResumeFormEvent.OtherTechnologiesChanged(it)) },
                labelText = stringResource(R.string.other_technologies),
                suggestions = SuggestionDictionary.otherTechnologies,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(14.dp))
            TagInputField(
                value = certifications,
                onValueChange = { onEvent(ResumeFormEvent.CertificationsChanged(it)) },
                labelText = stringResource(R.string.certifications),
                suggestions = SuggestionDictionary.certifications,
                tipsIntroRes = R.string.tips_certifications_intro,
                tipsBulletsRes = R.array.tips_certifications,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (professionalAchievements.isNotEmpty()) {
                StaffAiSectionLabel(
                    text = "${stringResource(R.string.achievements)} \u00B7 ${professionalAchievements.size}"
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            professionalAchievements.forEachIndexed { index, achievement ->
                key(index) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        StaffAiTextField(
                            value = achievement,
                            onValueChange = {
                                onEvent(ResumeFormEvent.ProfessionalAchievementChanged(index, it))
                            },
                            labelText = stringResource(R.string.achievements),
                            tipsIntroRes = R.string.tips_achievements_intro,
                            tipsBulletsRes = R.array.tips_achievements,
                            modifier = Modifier.weight(1f)
                        )
                        StaffAiDeleteIconButton(
                            onClick = {
                                onEvent(ResumeFormEvent.DeleteProfessionalAchievement(index))
                            },
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            StaffAiAddButton(
                text = stringResource(R.string.prof_achievements),
                onClick = { onEvent(ResumeFormEvent.AddProfessionalAchievement) },
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}
