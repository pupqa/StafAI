package com.bober.autcsv.presentation.screens.form.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.bober.autcsv.R
import com.bober.autcsv.presentation.screens.form.ResumeFormEvent
import com.bober.autcsv.presentation.screens.form.SuggestionDictionary

/**
 * Раздел «Гибкие навыки (soft skills)»: отдельная карточка формы —
 * технические и гибкие навыки заполняются и хранятся раздельно.
 * Ввод — тегами через [TagInputField] с автодополнением.
 */
@Composable
fun SoftSkillsSection(
    softSkills: String,
    onEvent: (ResumeFormEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    StaffAiSectionCard(
        modifier = modifier,
        title = stringResource(R.string.soft_skills)
    ) {
        TagInputField(
            value = softSkills,
            onValueChange = { onEvent(ResumeFormEvent.SoftSkillsChanged(it)) },
            labelText = stringResource(R.string.soft_skills),
            hint = stringResource(R.string.soft_skills_hint),
            suggestions = SuggestionDictionary.softSkills,
            tipsIntroRes = R.string.tips_soft_skills_intro,
            tipsBulletsRes = R.array.tips_soft_skills,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
