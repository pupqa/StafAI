package com.bober.autcsv.presentation.screens.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.bober.autcsv.R
import com.bober.autcsv.presentation.common.components.LoadingState
import com.bober.autcsv.presentation.screens.dashboard.components.DashboardContent

/**
 * Экран дашборда: фильтры резюме и список карточек, переходы к формам и предпросмотру.
 */
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



