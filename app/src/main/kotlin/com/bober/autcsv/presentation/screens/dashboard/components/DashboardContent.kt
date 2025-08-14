package com.bober.autcsv.presentation.screens.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bober.autcsv.domain.model.Resume
import com.bober.autcsv.presentation.screens.dashboard.AvailableFilters
import com.bober.autcsv.presentation.screens.dashboard.DashboardFilters

/**
 * Контент дашборда: секция фильтров и список резюме с применёнными фильтрами.
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
) {
    Column(modifier = modifier.fillMaxSize()) {
        FilterSection(
            filters = filters,
            availableFilters = availableFilters,
            onFilterChange = onFilterChange,
            onClearFilters = onClearFilters,
            modifier = Modifier.padding(8.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
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