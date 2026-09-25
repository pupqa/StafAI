package com.bober.autcsv.presentation.screens.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bober.autcsv.R
import com.bober.autcsv.presentation.screens.dashboard.AvailableFilters
import com.bober.autcsv.presentation.screens.dashboard.DashboardFilters
import com.bober.autcsv.ui.theme.AppColors
import com.bober.autcsv.ui.theme.MD
import com.bober.autcsv.ui.theme.Pill

/**
 * Фильтры дашборда — стиль экрана «05 · Фильтрация» из макета:
 * пилюля-триггер со счётчиком активных фильтров открывает нижний лист
 * с группами чипов (моно-заголовки), сброс и применение закреплены внизу.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSection(
    filters: DashboardFilters,
    availableFilters: AvailableFilters,
    onFilterChange: (DashboardFilters) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var sheetOpen by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(filters) }

    val activeCount = filters.experienceLevels.size + filters.technologies.size +
            filters.languages.size + filters.specializations.size +
            (if (filters.favoritesOnly) 1 else 0) +
            (if (filters.readyToRelocateOnly) 1 else 0) +
            (if (filters.minSalary != null) 1 else 0) +
            (if (filters.status != null) 1 else 0)

    FilterTriggerChip(
        activeCount = activeCount,
        onClick = {
            draft = filters
            sheetOpen = true
        },
        modifier = modifier
    )

    if (sheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            sheetState = sheetState,
            containerColor = AppColors.BackgroundSoft,
            contentColor = AppColors.TextPrimary,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 6.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .background(AppColors.Surface3, RoundedCornerShape(3.dp))
                )
            }
        ) {
            // Тело листа скроллится, а кнопки закреплены футером: раньше при
            // большом числе групп фильтров ряд «Сбросить / Применить» уезжал
            // за нижний край и становился недостижимым
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.filters),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = AppColors.TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.filter_reset),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.TextFaint,
                            modifier = Modifier
                                .clickable { draft = DashboardFilters() }
                                .padding(horizontal = 8.dp, vertical = 10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    FilterChipGroup(
                        title = stringResource(R.string.filter_work_experience),
                        options = listOf(
                            "Junior" to stringResource(R.string.filter_junior),
                            "Middle" to stringResource(R.string.filter_middle),
                            "Senior" to stringResource(R.string.filter_senior)
                        ),
                        selected = draft.experienceLevels,
                        onToggle = { option ->
                            draft =
                                draft.copy(experienceLevels = draft.experienceLevels.toggled(option))
                        }
                    )

                    if (availableFilters.programmingLanguages.isNotEmpty()) {
                        FilterChipGroup(
                            title = stringResource(R.string.programming_languages).uppercase(),
                            options = availableFilters.programmingLanguages.map { it to it },
                            selected = draft.technologies,
                            onToggle = { option ->
                                draft =
                                    draft.copy(technologies = draft.technologies.toggled(option))
                            }
                        )
                    }

                    if (availableFilters.languages.isNotEmpty()) {
                        FilterChipGroup(
                            title = stringResource(R.string.languages_name).uppercase(),
                            options = availableFilters.languages.map { it to it },
                            selected = draft.languages,
                            onToggle = { option ->
                                draft = draft.copy(languages = draft.languages.toggled(option))
                            }
                        )
                    }

                    if (availableFilters.specializations.isNotEmpty()) {
                        FilterChipGroup(
                            title = stringResource(R.string.specialization_filter).uppercase(),
                            options = availableFilters.specializations.map { it to it },
                            selected = draft.specializations,
                            onToggle = { option ->
                                draft =
                                    draft.copy(
                                        specializations = draft.specializations.toggled(
                                            option
                                        )
                                    )
                            }
                        )
                    }

                    // ── Условия работы ─────────────────────────────────────────
                    FilterChipGroup(
                        title = stringResource(R.string.filter_conditions),
                        options = listOf(
                            "favorites" to stringResource(R.string.filter_favorites_only),
                            "relocation" to stringResource(R.string.filter_ready_to_relocate)
                        ),
                        selected = buildSet {
                            if (draft.favoritesOnly) add("favorites")
                            if (draft.readyToRelocateOnly) add("relocation")
                        },
                        onToggle = { option ->
                            draft = when (option) {
                                "favorites" -> draft.copy(favoritesOnly = !draft.favoritesOnly)
                                else -> draft.copy(readyToRelocateOnly = !draft.readyToRelocateOnly)
                            }
                        }
                    )

                    // ── Статус пайплайна ───────────────────────────────────────
                    FilterChipGroup(
                        title = stringResource(R.string.filter_status_group),
                        options = com.bober.autcsv.domain.model.CandidateStatus.entries
                            .filter { it != com.bober.autcsv.domain.model.CandidateStatus.NONE }
                            .map { it.name to stringResource(it.labelRes) },
                        selected = draft.status?.let { setOf(it.name) } ?: emptySet(),
                        onToggle = { option ->
                            val chosen =
                                com.bober.autcsv.domain.model.CandidateStatus.fromName(option)
                            draft = draft.copy(
                                status = if (draft.status == chosen) null else chosen
                            )
                        }
                    )

                    // ── Минимальная зарплата ───────────────────────────────────
                    Column(modifier = Modifier.padding(bottom = 20.dp)) {
                        Text(
                            text = stringResource(R.string.filter_salary_from_label),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            letterSpacing = 0.6.sp,
                            color = AppColors.TextFaint,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                        val salaryOptions = listOf(50000, 100000, 150000, 200000, 300000)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(null to stringResource(R.string.filter_any))
                                .plus(salaryOptions.map {
                                    it to stringResource(
                                        R.string.filter_salary_k_fmt,
                                        it / 1000
                                    )
                                })
                                .forEach { (value, label) ->
                                    val isSelected = draft.minSalary == value
                                    Text(
                                        text = label,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) AppColors.AccentInk else AppColors.TextDim,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(Pill))
                                            .background(if (isSelected) AppColors.Accent else AppColors.Surface)
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) AppColors.Accent else AppColors.BorderSoft,
                                                shape = RoundedCornerShape(Pill)
                                            )
                                            .clickable { draft = draft.copy(minSalary = value) }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                        }
                    }
                } // конец скроллящегося тела

                // Футер с действиями вне скроллящейся области: кнопки всегда
                // на виду, сколько бы групп фильтров ни было
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onClearFilters()
                            sheetOpen = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(MD),
                        border = BorderStroke(1.5.dp, AppColors.Border),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.TextPrimary)
                    ) {
                        Text(
                            text = stringResource(R.string.filter_reset),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    val draftCount = draft.experienceLevels.size + draft.technologies.size +
                            draft.languages.size + draft.specializations.size +
                            (if (draft.favoritesOnly) 1 else 0) +
                            (if (draft.readyToRelocateOnly) 1 else 0) +
                            (if (draft.minSalary != null) 1 else 0) +
                            (if (draft.status != null) 1 else 0)

                    Button(
                        onClick = {
                            onFilterChange(draft)
                            sheetOpen = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(MD),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Accent,
                            contentColor = AppColors.AccentInk
                        )
                    ) {
                        Text(
                            text = if (draftCount > 0) {
                                stringResource(R.string.filter_apply_count, draftCount)
                            } else {
                                stringResource(R.string.filter_apply)
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

private fun Set<String>.toggled(item: String): Set<String> =
    if (contains(item)) this - item else this + item

@Composable
private fun FilterTriggerChip(
    activeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = activeCount > 0
    // Подпись видна всегда (раньше без активных фильтров оставался только
    // значок): «Фильтры» / «Фильтры · 3» — кнопка не выглядит пустой
    val label = if (selected) {
        "${stringResource(R.string.filters)} · $activeCount"
    } else {
        stringResource(R.string.filters)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(Pill))
            .background(if (selected) AppColors.Accent else AppColors.Surface)
            .border(
                width = 1.dp,
                color = if (selected) AppColors.Accent else AppColors.BorderSoft,
                shape = RoundedCornerShape(Pill)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.FilterList,
            contentDescription = null,
            tint = if (selected) AppColors.AccentInk else AppColors.TextDim,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) AppColors.AccentInk else AppColors.TextDim,
            maxLines = 1,
            softWrap = false
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterChipGroup(
    title: String,
    options: List<Pair<String, String>>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = 20.dp)) {
        Text(
            text = title,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            letterSpacing = 0.6.sp,
            color = AppColors.TextFaint,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (value, label) ->
                val isSelected = selected.contains(value)
                Text(
                    text = label,
                    fontSize = 12.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) AppColors.AccentInk else AppColors.TextDim,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Pill))
                        .background(if (isSelected) AppColors.Accent else AppColors.Surface)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) AppColors.Accent else AppColors.BorderSoft,
                            shape = RoundedCornerShape(Pill)
                        )
                        .clickable { onToggle(value) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}