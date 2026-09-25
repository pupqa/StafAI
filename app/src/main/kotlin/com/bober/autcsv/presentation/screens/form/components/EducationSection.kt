package com.bober.autcsv.presentation.screens.form.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bober.autcsv.R
import com.bober.autcsv.domain.model.EducationEntry
import com.bober.autcsv.presentation.screens.form.ResumeFormEvent
import com.bober.autcsv.presentation.screens.form.SuggestionDictionary
import com.bober.autcsv.presentation.screens.form.SuggestionDictionary.EducationLevel

/**
 * Раздел «Образование»: неограниченный список записей — уровень (СПО /
 * высшее / послевузовское / дополнительное) с динамической фильтрацией
 * специальностей, автодополнением заведения. Запись добавляется кнопкой
 * «Добавить ещё одно образование» и удаляется крестиком рядом с ней.
 */
@Composable
fun EducationSection(
    educations: List<EducationEntry>,
    onEvent: (ResumeFormEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    StaffAiSectionCard(modifier = modifier, title = stringResource(R.string.education)) {
        Column {
            if (educations.isNotEmpty()) {
                StaffAiSectionLabel(
                    text = "${stringResource(R.string.education)} \u00B7 ${educations.size}",
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            educations.forEachIndexed { index, entry ->
                key(index) {
                    EducationEntryCard(
                        entry = entry,
                        onEvent = onEvent,
                        index = index
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            StaffAiAddButton(
                text = stringResource(R.string.add_education_entry),
                onClick = { onEvent(ResumeFormEvent.AddEducationEntry) },
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

/** Одна запись об образовании: уровень чипами + специальность + заведение. */
@Composable
private fun EducationEntryCard(
    entry: EducationEntry,
    index: Int,
    onEvent: (ResumeFormEvent) -> Unit,
) {
    val level = remember(entry.level) { EducationLevel.fromLabel(entry.level) }

    // Список специальностей пересчитывается при смене уровня или языка интерфейса
    val specialtySuggestions = SuggestionDictionary.specialtiesFor(level ?: EducationLevel.BACHELOR)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StaffAiSectionLabel(
                text = stringResource(R.string.education_level),
                modifier = Modifier.weight(1f)
            )
            StaffAiDeleteIconButton(
                onClick = { onEvent(ResumeFormEvent.DeleteEducationEntry(index)) },
                contentDescription = stringResource(R.string.delete)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Пилюльный выбор уровня, горизонтальная прокрутка на узких экранах
        val levelSelectionDescription = stringResource(R.string.form_education_level_selection)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .semantics { contentDescription = levelSelectionDescription }
        ) {
            EducationLevel.entries.forEachIndexed { chipIndex, levelOption ->
                val selected = levelOption == level
                StaffAiLevelChip(
                    label = levelOption.label,
                    selected = selected,
                    onClick = {
                        onEvent(
                            ResumeFormEvent.EducationEntryLevelChanged(
                                index,
                                levelOption
                            )
                        )
                    },
                    modifier = Modifier.padding(
                        end = if (chipIndex == EducationLevel.entries.size - 1) 0.dp else 6.dp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        AutocompleteTextField(
            value = entry.specialty,
            onValueChange = { onEvent(ResumeFormEvent.EducationEntrySpecialtyChanged(index, it)) },
            labelText = stringResource(R.string.education_specialty),
            hint = stringResource(R.string.education_specialty_hint),
            suggestions = specialtySuggestions,
            tipsIntroRes = R.string.tips_education_intro,
            tipsBulletsRes = R.array.tips_education,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        AutocompleteTextField(
            value = entry.institution,
            onValueChange = {
                onEvent(
                    ResumeFormEvent.EducationEntryInstitutionChanged(
                        index,
                        it
                    )
                )
            },
            labelText = stringResource(R.string.education_institution),
            suggestions = SuggestionDictionary.institutions,
            modifier = Modifier.fillMaxWidth()
        )
    }
}