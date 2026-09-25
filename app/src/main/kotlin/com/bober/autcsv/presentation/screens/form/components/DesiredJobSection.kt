package com.bober.autcsv.presentation.screens.form.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bober.autcsv.R
import com.bober.autcsv.presentation.screens.form.FormValidators
import com.bober.autcsv.presentation.screens.form.ResumeFormEvent
import com.bober.autcsv.presentation.screens.form.SuggestionDictionary
import com.bober.autcsv.presentation.screens.form.SuggestionDictionary.RelocationOption
import com.bober.autcsv.ui.theme.AppColors

/**
 * Раздел «Желаемая работа и условия»: зарплатная вилка (обязательна нижняя
 * граница), готовность к релокации (обязательный выбор чипом) и города
 * переезда для готовых к релокации кандидатов.
 *
 * Ошибки обязательных полей показываются только после касания поля —
 * пустая форма не «ругается» на старте.
 */
@Composable
fun DesiredJobSection(
    salaryMin: String,
    salaryMax: String,
    readyToRelocate: String,
    relocationCities: String,
    employment: String,
    workSchedule: String,
    onEvent: (ResumeFormEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Ошибки обязательных полей — после первого касания, чтобы не пугать на старте
    var salaryMinTouched by rememberSaveable { mutableStateOf(false) }
    var salaryMaxTouched by rememberSaveable { mutableStateOf(false) }

    val salaryMinError = remember(salaryMin, salaryMinTouched) {
        FormValidators.validateField(FormValidators.Field.SALARY_MIN, salaryMin, salaryMinTouched)
    }
    val salaryMaxError = remember(salaryMax, salaryMin, salaryMaxTouched) {
        FormValidators.validateField(FormValidators.Field.SALARY_MAX, salaryMax, salaryMaxTouched)
            ?: FormValidators.validateSalaryRange(salaryMin, salaryMax)?.takeIf { salaryMaxTouched }
    }

    StaffAiSectionCard(
        modifier = modifier,
        title = stringResource(R.string.desired_job)
    ) {
        androidx.compose.foundation.layout.Column {
            Row(modifier = Modifier.fillMaxWidth()) {
                StaffAiTextFieldValue(
                    value = salaryMin,
                    onTextChange = { text ->
                        // Разряды группируются сразу: «150000» превращается
                        // в «150 000» на лету; валидатор понимает формат
                        onEvent(
                            ResumeFormEvent.SalaryMinChanged(
                                com.bober.autcsv.core.utils.NumberFormatting.groupThousands(text)
                            )
                        )
                    },
                    labelText = stringResource(R.string.salary_from),
                    hint = stringResource(R.string.salary_hint),
                    keyboardType = KeyboardType.Number,
                    isError = salaryMinError != null,
                    errorText = salaryMinError?.let { stringResource(it) },
                    onFocusChanged = { focused -> if (!focused) salaryMinTouched = true },
                    tipsIntroRes = R.string.tips_salary_intro,
                    tipsBulletsRes = R.array.tips_salary,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                StaffAiTextFieldValue(
                    value = salaryMax,
                    onTextChange = { text ->
                        onEvent(
                            ResumeFormEvent.SalaryMaxChanged(
                                com.bober.autcsv.core.utils.NumberFormatting.groupThousands(text)
                            )
                        )
                    },
                    labelText = stringResource(R.string.salary_to),
                    keyboardType = KeyboardType.Number,
                    isError = salaryMaxError != null,
                    errorText = salaryMaxError?.let { stringResource(it) },
                    onFocusChanged = { focused -> if (!focused) salaryMaxTouched = true },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            StaffAiSectionLabel(text = stringResource(R.string.relocation))
            Spacer(modifier = Modifier.height(8.dp))

            // Повторный тап по выбранному чипу снимает выбор.
            // Сохранённое значение — текстовая метка, распознаётся в любой локали
            val selectedOption = remember(readyToRelocate) {
                RelocationOption.fromLabel(readyToRelocate)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                RelocationOption.entries.forEachIndexed { index, option ->
                    val selected = option == selectedOption
                    val optionLabel = option.label
                    StaffAiLevelChip(
                        label = optionLabel,
                        selected = selected,
                        onClick = {
                            onEvent(
                                ResumeFormEvent.RelocationChanged(
                                    if (selected) "" else optionLabel
                                )
                            )
                        },
                        modifier = Modifier.padding(
                            end = if (index == RelocationOption.entries.size - 1) 0.dp else 6.dp
                        )
                    )
                }
            }

            // Релокация обязательна, но у чипов нет error-состояния —
            // мягкая подсказка, пока вариант не выбран
            if (readyToRelocate.isBlank()) {
                Text(
                    text = stringResource(R.string.relocation_required_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextDim,
                    modifier = Modifier.padding(top = 6.dp, start = 2.dp)
                )
            }

            // Города релокации имеют смысл только при готовности к переезду
            if (selectedOption?.ready == true) {
                Spacer(modifier = Modifier.height(14.dp))
                AutocompleteTextField(
                    value = relocationCities,
                    onValueChange = { onEvent(ResumeFormEvent.RelocationCitiesChanged(it)) },
                    labelText = stringResource(R.string.relocation_cities),
                    hint = stringResource(R.string.relocation_cities_hint),
                    suggestions = SuggestionDictionary.cities,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Желаемая занятость — мультивыбор чипами
            Spacer(modifier = Modifier.height(14.dp))
            StaffAiSectionLabel(text = stringResource(R.string.employment))
            Spacer(modifier = Modifier.height(8.dp))
            MultiSelectChipRow(
                options = SuggestionDictionary.employmentOptions,
                selected = employment,
                onToggle = { onEvent(ResumeFormEvent.EmploymentToggled(it)) }
            )

            // Желаемый график — мультивыбор чипами
            Spacer(modifier = Modifier.height(12.dp))
            StaffAiSectionLabel(text = stringResource(R.string.work_schedule))
            Spacer(modifier = Modifier.height(8.dp))
            MultiSelectChipRow(
                options = SuggestionDictionary.workScheduleOptions,
                selected = workSchedule,
                onToggle = { onEvent(ResumeFormEvent.WorkScheduleToggled(it)) }
            )
        }
    }
}

/** Горизонтальная прокручиваемая строка чипов мультивыбора (строка «a, b, c»). */
@Composable
private fun MultiSelectChipRow(
    options: List<String>,
    selected: String,
    onToggle: (String) -> Unit,
) {
    val selectedItems = remember(selected) {
        selected.split(",").map { it.trim() }.filter { it.isNotBlank() }.map { it.lowercase() }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = option.lowercase() in selectedItems
            StaffAiLevelChip(
                label = option,
                selected = isSelected,
                onClick = { onToggle(option) },
                modifier = Modifier.padding(
                    end = if (index == options.lastIndex) 0.dp else 6.dp
                )
            )
        }
    }
}
