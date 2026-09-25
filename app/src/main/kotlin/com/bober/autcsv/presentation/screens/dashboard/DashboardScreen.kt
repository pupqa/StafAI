package com.bober.autcsv.presentation.screens.dashboard

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bober.autcsv.R
import com.bober.autcsv.presentation.common.components.LoadingIndicator
import com.bober.autcsv.presentation.screens.dashboard.components.DashboardContent
import com.bober.autcsv.ui.theme.AppColors

/**
 * Экран дашборда: фильтры резюме и список карточек, переходы к формам и предпросмотру.
 * Стиль — экран «01 · Список резюме» из макета: тёмная шапка, FAB с пружинным нажатием.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutDashboardScreen(
    onNavigateToForm: () -> Unit,
    onNavigateToPreview: (String) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = AppColors.BackgroundSoft,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.cv_control_panel),
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp,
                        letterSpacing = (-0.2).sp,
                        color = AppColors.TextPrimary
                    )
                },
                navigationIcon = {
                    onNavigateBack?.let { back ->
                        IconButton(onClick = back) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = AppColors.TextPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.BackgroundSoft,
                    titleContentColor = AppColors.TextPrimary,
                    navigationIconContentColor = AppColors.TextPrimary
                )
            )
        },
        floatingActionButton = {
            AutFloatingActionButton(onClick = onNavigateToForm)
        }
    ) { padding ->
        when (val state = uiState) {
            is DashboardUiState.Loading -> LoadingIndicator()
            is DashboardUiState.Success -> {
                DashboardContent(
                    resumes = state.resumes,
                    filters = state.filters,
                    availableFilters = state.availableFilters,
                    onFilterChange = viewModel::updateFilters,
                    onClearFilters = viewModel::clearFilters,
                    onResumeClick = onNavigateToPreview,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onCycleStatus = viewModel::cycleStatus,
                    onDuplicate = viewModel::duplicateResume,
                    modifier = Modifier.padding(padding)
                )
            }

            is DashboardUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(AppColors.BackgroundSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = state.message, color = AppColors.TextDim)
                }
            }
        }
    }
}

/**
 * FAB в стиле .fab из макета: скруглённый квадрат, золотая заливка,
 * пружинное сжатие с лёгким поворотом при нажатии.
 */
@Composable
private fun AutFloatingActionButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.86f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "fabScale"
    )
    val rotation by animateFloatAsState(
        targetValue = if (pressed) -6f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "fabRotation"
    )

    Box(
        modifier = Modifier
            .size(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
            }
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = AppColors.Accent
            )
            .background(AppColors.Accent, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(R.string.cd_add_resume),
            tint = AppColors.AccentInk
        )
    }
}