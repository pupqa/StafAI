package com.bober.autcsv.presentation.screens.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bober.autcsv.R
import com.bober.autcsv.presentation.screens.dashboard.AvailableFilters
import com.bober.autcsv.presentation.screens.dashboard.DashboardFilters

/**
 * Секция фильтров: опыт, технологии, языки, специализации.
 * Поддерживает мультивыбор и очистку.
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
    var showExperienceMenu by remember { mutableStateOf(false) }
    var showTechnologyMenu by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }
    var showSpecializationMenu by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (filters.hasActiveFilters()) {
                Button(
                    onClick = onClearFilters,
                    modifier = Modifier.padding(start = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.clear_all_filters)
                    )
                }
            }
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Box {
                    FilterChip(
                        selected = filters.experienceLevels.isNotEmpty(),
                        onClick = { showExperienceMenu = true },
                        label = {
                            Text(
                                if (filters.experienceLevels.isEmpty()) stringResource(R.string.experience)
                                else filters.experienceLevels.joinToString(", ")
                            )
                        }
                    )

                    DropdownMenu(
                        expanded = showExperienceMenu,
                        onDismissRequest = { showExperienceMenu = false }
                    ) {
                        fun toggleExperience(option: String) {
                            val newSet = filters.experienceLevels.toMutableSet().apply {
                                if (contains(option)) remove(option) else add(option)
                            }
                            onFilterChange(filters.copy(experienceLevels = newSet))
                        }
                        DropdownMenuItem(text = { Text("Сбросить") }, onClick = {
                            onFilterChange(filters.copy(experienceLevels = emptySet()))
                        })
                        DropdownMenuItem(
                            text = { Text("Junior (< 3 years)") },
                            onClick = { toggleExperience("Junior") })
                        DropdownMenuItem(
                            text = { Text("Middle (3-5 years)") },
                            onClick = { toggleExperience("Middle") })
                        DropdownMenuItem(
                            text = { Text("Senior (> 5 years)") },
                            onClick = { toggleExperience("Senior") })
                    }
                }
            }

            // Technology Filter (multi-select)
            item {
                Box {
                    FilterChip(
                        selected = filters.technologies.isNotEmpty(),
                        onClick = { showTechnologyMenu = true },
                        label = {
                            Text(
                                if (filters.technologies.isEmpty()) stringResource(R.string.programming_languages)
                                else filters.technologies.joinToString(", ")
                            )
                        }
                    )

                    DropdownMenu(
                        expanded = showTechnologyMenu,
                        onDismissRequest = { showTechnologyMenu = false }
                    ) {
                        DropdownMenuItem(text = { Text("Сбросить") }, onClick = {
                            onFilterChange(filters.copy(technologies = emptySet()))
                        })
                        availableFilters.programmingLanguages.forEach { tech ->
                            DropdownMenuItem(
                                text = { Text(tech) },
                                onClick = {
                                    val newSet = filters.technologies.toMutableSet().apply {
                                        if (contains(tech)) remove(tech) else add(tech)
                                    }
                                    onFilterChange(filters.copy(technologies = newSet))
                                }
                            )
                        }
                    }
                }
            }

            // Language Filter (multi-select)
            item {
                Box {
                    FilterChip(
                        selected = filters.languages.isNotEmpty(),
                        onClick = { showLanguageMenu = true },
                        label = {
                            Text(
                                if (filters.languages.isEmpty()) stringResource(R.string.languages_name)
                                else filters.languages.joinToString(", ")
                            )
                        }
                    )

                    DropdownMenu(
                        expanded = showLanguageMenu,
                        onDismissRequest = { showLanguageMenu = false }
                    ) {
                        DropdownMenuItem(text = { Text("Сбросить") }, onClick = {
                            onFilterChange(filters.copy(languages = emptySet()))
                        })
                        availableFilters.languages.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang) },
                                onClick = {
                                    val newSet = filters.languages.toMutableSet().apply {
                                        if (contains(lang)) remove(lang) else add(lang)
                                    }
                                    onFilterChange(filters.copy(languages = newSet))
                                }
                            )
                        }
                    }
                }
            }

            // Specialization Filter (multi-select)
            item {
                Box {
                    FilterChip(
                        selected = filters.specializations.isNotEmpty(),
                        onClick = { showSpecializationMenu = true },
                        label = {
                            Text(
                                if (filters.specializations.isEmpty()) stringResource(R.string.specialization_filter)
                                else filters.specializations.joinToString(", ")
                            )
                        }
                    )

                    DropdownMenu(
                        expanded = showSpecializationMenu,
                        onDismissRequest = { showSpecializationMenu = false }
                    ) {
                        DropdownMenuItem(text = { Text("Сбросить") }, onClick = {
                            onFilterChange(filters.copy(specializations = emptySet()))
                        })
                        availableFilters.specializations.forEach { spec ->
                            DropdownMenuItem(
                                text = { Text(spec) },
                                onClick = {
                                    val newSet = filters.specializations.toMutableSet().apply {
                                        if (contains(spec)) remove(spec) else add(spec)
                                    }
                                    onFilterChange(filters.copy(specializations = newSet))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}