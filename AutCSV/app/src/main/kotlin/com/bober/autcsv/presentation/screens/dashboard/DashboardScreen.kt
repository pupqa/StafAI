package com.bober.autcsv.presentation.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bober.autcsv.R
import com.bober.autcsv.domain.model.Resume
import com.bober.autcsv.presentation.common.components.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutDashboardScreen(
    onNavigateToForm: () -> Unit,
    onNavigateToPreview: (String) -> Unit,
    onNavigateBack: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cv_control_panel)) },
                navigationIcon = {
                    if (onNavigateBack != {}) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Navigate back"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToForm) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add new CV"
                        )
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is DashboardUiState.Loading -> LoadingState()
            is DashboardUiState.Success -> {
                DashboardContent(
                    resumes = state.resumes,
                    filters = state.filters,
                    availableFilters = state.availableFilters,
                    onFilterChange = viewModel::updateFilters,
                    onClearFilters = viewModel::clearFilters,
                    onResumeClick = onNavigateToPreview,
                    modifier = Modifier.padding(padding)
                )
            }

            is DashboardUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(state.message)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardContent(
    resumes: List<Resume>,
    filters: DashboardFilters,
    availableFilters: AvailableFilters,
    onFilterChange: (DashboardFilters) -> Unit,
    onClearFilters: () -> Unit,
    onResumeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        FilterSection(
            filters = filters,
            availableFilters = availableFilters,
            onFilterChange = onFilterChange,
            onClearFilters = onClearFilters,
            modifier = Modifier.padding(16.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(resumes) { resume ->
                ResumeCard(
                    resume = resume,
                    onClick = { onResumeClick(resume.id) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSection(
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
            Text(
                text = stringResource(R.string.filters),
                style = MaterialTheme.typography.titleMedium
            )

            if (filters.hasActiveFilters()) {
                Button(
                    onClick = onClearFilters,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear filters",
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Очистить")
                }
            }
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Experience Level Filter (multi-select)
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
                        DropdownMenuItem(text = { Text("Clear") }, onClick = {
                            onFilterChange(filters.copy(experienceLevels = emptySet()))
                        })
                        DropdownMenuItem(text = { Text("Junior (< 3 years)") }, onClick = { toggleExperience("Junior") })
                        DropdownMenuItem(text = { Text("Middle (3-5 years)") }, onClick = { toggleExperience("Middle") })
                        DropdownMenuItem(text = { Text("Senior (> 5 years)") }, onClick = { toggleExperience("Senior") })
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
                        DropdownMenuItem(text = { Text("Clear") }, onClick = {
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
                                if (filters.languages.isEmpty()) stringResource(R.string.language_name)
                                else filters.languages.joinToString(", ")
                            )
                        }
                    )

                    DropdownMenu(
                        expanded = showLanguageMenu,
                        onDismissRequest = { showLanguageMenu = false }
                    ) {
                        DropdownMenuItem(text = { Text("Clear") }, onClick = {
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
                        DropdownMenuItem(text = { Text("Clear") }, onClick = {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResumeCard(
    resume: Resume,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = resume.personalInfo.fullName,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${resume.personalInfo.specialization} • ${resume.personalInfo.totalExperience}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                                        text = resume.professionalSkills.getAllTechnologies().take(3).joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
            if (resume.summary.isNotBlank()) {
                Text(
                    text = resume.summary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
} 