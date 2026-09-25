package com.bober.autcsv.presentation.screens.form.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bober.autcsv.R
import com.bober.autcsv.domain.model.Language
import com.bober.autcsv.presentation.screens.form.ResumeFormEvent
import com.bober.autcsv.presentation.screens.form.SuggestionDictionary

/**
 * Раздел «Языки»: stateless-список карточек с автодополнением названий
 * и уровней. key() сохраняет состояние и позицию каждого поля при
 * удалении элементов списка.
 */
@Composable
fun LanguagesSection(
    languages: List<Language>,
    onEvent: (ResumeFormEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    StaffAiSectionCard(
        modifier = modifier,
        title = stringResource(R.string.languages)
    ) {
        Column {
            if (languages.isNotEmpty()) {
                StaffAiSectionLabel(
                    text = "${stringResource(R.string.languages)} \u00B7 ${languages.size}",
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            languages.forEachIndexed { index, language ->
                key(index) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        AutocompleteTextField(
                            value = language.name,
                            onValueChange = {
                                onEvent(ResumeFormEvent.LanguageNameChanged(index, it))
                            },
                            labelText = stringResource(R.string.language_name),
                            suggestions = SuggestionDictionary.languages,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AutocompleteTextField(
                            value = language.level,
                            onValueChange = {
                                onEvent(ResumeFormEvent.LanguageLevelChanged(index, it))
                            },
                            labelText = stringResource(R.string.language_level),
                            suggestions = SuggestionDictionary.languageLevels,
                            tipsIntroRes = R.string.tips_language_levels_intro,
                            tipsBulletsRes = R.array.tips_language_levels,
                            modifier = Modifier.weight(0.8f)
                        )
                        StaffAiDeleteIconButton(
                            onClick = { onEvent(ResumeFormEvent.DeleteLanguage(index)) },
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            StaffAiAddButton(
                text = stringResource(R.string.language_name),
                onClick = { onEvent(ResumeFormEvent.AddLanguage) },
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}
