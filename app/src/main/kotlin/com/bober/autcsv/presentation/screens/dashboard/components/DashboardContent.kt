package com.bober.autcsv.presentation.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bober.autcsv.R
import com.bober.autcsv.domain.model.Resume
import com.bober.autcsv.presentation.screens.dashboard.AvailableFilters
import com.bober.autcsv.presentation.screens.dashboard.DashboardFilters
import com.bober.autcsv.ui.theme.AppColors
import com.bober.autcsv.ui.theme.Pill

/**
 * Контент дашборда: полнотекстовый поиск, mono-лейбл счётчика, сортировка,
 * триггер фильтров и список резюме — стиль экрана «01 · Список резюме».
 *
 * Режим сравнения: слева на карточке появляется переключатель ○/◉;
 * при выборе двух карточек открывается диалог построчного сравнения.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    resumes: List<Resume>,
    filters: DashboardFilters,
    availableFilters: AvailableFilters,
    onFilterChange: (DashboardFilters) -> Unit,
    onClearFilters: () -> Unit,
    onResumeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onToggleFavorite: ((String) -> Unit)? = null,
    onCycleStatus: ((String) -> Unit)? = null,
    onDuplicate: ((String) -> Unit)? = null,
) {
    var searchQuery by remember { mutableStateOf("") }
    var compareMode by remember { mutableStateOf(false) }
    // remember обязателен: без него список пересоздаётся пустым на каждой
    // рекомпозиции — выбор двух карточек не доживал до диалога сравнения
    val compareSelection = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    var showCompareDialog by remember { mutableStateOf(false) }

    val visibleResumes = remember(resumes, searchQuery) {
        if (searchQuery.isBlank()) {
            resumes
        } else {
            resumes.filter { resume -> matchesFullText(resume, searchQuery) }
        }
    }

    val comparePair = compareSelection.mapNotNull { id -> resumes.firstOrNull { it.id == id } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BackgroundSoft)
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp)) {
            Spacer(modifier = Modifier.height(4.dp))

            SearchField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.padding(top = 8.dp, bottom = 10.dp)
            )

            // Счётчик вынесен на отдельную строку-подпись: он больше не
            // конкурирует за ширину с пилюлями управления
            Text(
                text = stringResource(R.string.dashboard_resumes_count, visibleResumes.size),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 0.7.sp,
                color = AppColors.TextFaint,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )

            // Пилюли управления едут в горизонтальном скролле: когда суммарная
            // ширина больше экрана (узкий экран, длинные английские подписи,
            // развёрнутая подпись режима сравнения), они прокручиваются,
            // а не сжимаются и не вытесняют друг друга за край
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (compareMode) {
                        stringResource(R.string.dashboard_compare_select)
                    } else {
                        stringResource(R.string.dashboard_compare)
                    },
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = if (compareMode) AppColors.Accent else AppColors.TextDim,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Pill))
                        .background(if (compareMode) AppColors.AmberSoft else AppColors.Surface)
                        .border(
                            1.dp,
                            if (compareMode) AppColors.Accent else AppColors.BorderSoft,
                            RoundedCornerShape(Pill)
                        )
                        .clickable {
                            compareMode = !compareMode
                            compareSelection.clear()
                        }
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                )
                SortMenu(
                    sortBy = filters.sortBy,
                    sortAscending = filters.sortAscending,
                    onSelect = { onFilterChange(filters.copy(sortBy = it)) },
                    onDirectionChange = {
                        onFilterChange(filters.copy(sortAscending = !filters.sortAscending))
                    }
                )
                FilterSection(
                    filters = filters,
                    availableFilters = availableFilters,
                    onFilterChange = onFilterChange,
                    onClearFilters = onClearFilters
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        if (visibleResumes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.dashboard_no_resumes),
                    color = AppColors.TextFaint,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(visibleResumes, key = { it.id }) { resume ->
                    // В режиме сравнения тап по карточке выделяет её, а не
                    // уводит в предпросмотр — иначе выделить вторую карточку
                    // невозможно, не «сбив» первую переходом
                    val compareToggle: () -> Unit = {
                        if (resume.id in compareSelection) {
                            compareSelection.remove(resume.id)
                        } else if (compareSelection.size < 2) {
                            compareSelection.add(resume.id)
                        }
                        showCompareDialog = compareSelection.size == 2
                    }
                    ResumeCard(
                        resume = resume,
                        onClick = if (compareMode) compareToggle else ({ onResumeClick(resume.id) }),
                        onToggleFavorite = onToggleFavorite?.let { cb -> { cb(resume.id) } },
                        onCycleStatus = onCycleStatus?.let { cb -> { cb(resume.id) } },
                        onDuplicate = onDuplicate?.let { cb -> { cb(resume.id) } },
                        compareSelected = resume.id in compareSelection,
                        onCompareToggle = if (compareMode) compareToggle else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) } // место под FAB
            }
        }
    }

    if (showCompareDialog && comparePair.size == 2) {
        CompareCandidatesDialog(
            first = comparePair[0],
            second = comparePair[1],
            onDismiss = {
                showCompareDialog = false
                compareSelection.clear()
            }
        )
    }
}

/** Полнотекстовый поиск по ключевым полям резюме. */
private fun matchesFullText(resume: Resume, query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    val haystack = buildList {
        add(resume.personalInfo.fullName)
        add(resume.personalInfo.specialization)
        add(resume.personalInfo.location)
        add(resume.personalInfo.aboutMe)
        add(resume.personalInfo.readyToRelocate)
        addAll(resume.personalInfo.languages.map { "${it.name} ${it.level}" })
        addAll(resume.professionalSkills.getAllTechnologies())
        addAll(resume.professionalSkills.softSkills)
        addAll(resume.projects.map { "${it.name} ${it.role} ${it.description}" })
    }
    return haystack.any { it.contains(q, ignoreCase = true) }
}

/** Меню сортировки списка: ключ + направление (по возрастанию/убыванию). */
@Composable
private fun SortMenu(
    sortBy: com.bober.autcsv.presentation.screens.dashboard.SortOrder,
    sortAscending: Boolean,
    onSelect: (com.bober.autcsv.presentation.screens.dashboard.SortOrder) -> Unit,
    onDirectionChange: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Text(
            text = "${stringResource(sortBy.labelRes)} ${if (sortAscending) "↑" else "↓"}",
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            fontSize = 11.sp,
            color = AppColors.TextDim,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .clip(RoundedCornerShape(Pill))
                .background(AppColors.Surface)
                .border(1.dp, AppColors.BorderSoft, RoundedCornerShape(Pill))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 9.dp)
        )
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }) {
            com.bober.autcsv.presentation.screens.dashboard.SortOrder.entries.forEach { option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(stringResource(option.labelRes)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
            androidx.compose.material3.HorizontalDivider()
            // Переключатель направления сортировки
            listOf(
                true to stringResource(R.string.sort_ascending),
                false to stringResource(R.string.sort_descending)
            ).forEach { (ascending, label) ->
                androidx.compose.material3.DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            fontWeight = if (ascending == sortAscending) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        if (ascending != sortAscending) onDirectionChange()
                        expanded = false
                    }
                )
            }
        }
    }
}

/** «150 000–200 000 ₽» или «—», если вилка не указана. */
private fun com.bober.autcsv.domain.model.PersonalInfo.salaryRangeLabel(): String {
    val from = salaryMin.filter { it.isDigit() }
    val to = salaryMax.filter { it.isDigit() }
    if (from.isBlank() && to.isBlank()) return "—"
    val range = when {
        from.isNotBlank() && to.isNotBlank() -> "$from–$to"
        else -> ifBlankEither(from, to)
    }
    return "$range ₽"
}

private fun ifBlankEither(a: String, b: String): String = if (a.isBlank()) b else a

/** Диалог построчного сравнения двух кандидатов. */
@Composable
private fun CompareCandidatesDialog(
    first: Resume,
    second: Resume,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dashboard_compare_close))
            }
        },
        title = { Text(stringResource(R.string.dashboard_compare_title)) },
        text = {
            Column {
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        first.personalInfo.fullName.ifBlank { "—" },
                        Modifier.weight(1f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        second.personalInfo.fullName.ifBlank { "—" },
                        Modifier.weight(1f),
                        fontWeight = FontWeight.Bold
                    )
                }
                compareRows(first, second).forEach { (label, a, b) ->
                    Spacer(Modifier.height(8.dp))
                    Text(label, fontSize = 11.sp, color = AppColors.TextFaint)
                    Row(Modifier.fillMaxWidth()) {
                        Text(a, Modifier.weight(1f), fontSize = 12.5.sp)
                        Text(b, Modifier.weight(1f), fontSize = 12.5.sp)
                    }
                }
            }
        }
    )
}

@Composable
private fun compareRows(first: Resume, second: Resume): List<Triple<String, String, String>> =
    listOf(
        Triple(
            stringResource(R.string.dashboard_col_position),
            first.personalInfo.specialization,
            second.personalInfo.specialization
        ),
        Triple(
            stringResource(R.string.dashboard_col_experience),
            first.personalInfo.totalExperience,
            second.personalInfo.totalExperience
        ),
        Triple(
            stringResource(R.string.dashboard_col_salary),
            first.personalInfo.salaryRangeLabel(),
            second.personalInfo.salaryRangeLabel()
        ),
        Triple(
            stringResource(R.string.dashboard_col_city),
            first.personalInfo.location,
            second.personalInfo.location
        ),
        Triple(
            stringResource(R.string.dashboard_col_relocation),
            first.personalInfo.readyToRelocate,
            second.personalInfo.readyToRelocate
        ),
        Triple(
            stringResource(R.string.dashboard_col_languages),
            first.personalInfo.languages.joinToString(", ") { "${it.name} (${it.level})" },
            second.personalInfo.languages.joinToString(", ") { "${it.name} (${it.level})" }
        ),
        Triple(
            stringResource(R.string.dashboard_col_technologies),
            first.professionalSkills.getAllTechnologies().take(8).joinToString(", "),
            second.professionalSkills.getAllTechnologies().take(8).joinToString(", ")
        ),
        Triple(
            stringResource(R.string.dashboard_col_projects),
            first.projects.size.toString(),
            second.projects.size.toString()
        ),
        Triple(
            stringResource(R.string.dashboard_col_status),
            stringResource(first.status.labelRes),
            stringResource(second.status.labelRes)
        ),
    )

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Pill))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.BorderSoft, RoundedCornerShape(Pill))
            .padding(horizontal = 16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = AppColors.TextFaint,
            modifier = Modifier.width(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = stringResource(R.string.dashboard_search_hint),
                    color = AppColors.TextFaint,
                    fontSize = 13.5.sp
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    color = AppColors.TextPrimary,
                    fontSize = 13.5.sp
                ),
                cursorBrush = SolidColor(AppColors.Accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 13.dp)
            )
        }
    }
}