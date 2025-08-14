package com.bober.autcsv.presentation.screens.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bober.autcsv.R
import com.bober.autcsv.core.pdf.PdfTemplateType
import com.bober.autcsv.presentation.common.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumePreviewScreen(
    resumeId: String,
    viewModel: ResumePreviewViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToAnalysis: (String) -> Unit,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(resumeId) {
        viewModel.onEvent(ResumePreviewEvent.LoadResume(resumeId))
    }

    LaunchedEffect(Unit) {
        viewModel.setNavigationCallback(onNavigateToAnalysis)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.preview)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Resume Quality Assessment
//            state.resume?.let { resume ->
//                ResumeQualityCard(resume = resume)
//                Spacer(modifier = Modifier.height(16.dp))
//            }

            // Template selection
            TemplateSelector(
                selectedTemplate = state.selectedTemplate,
                onTemplateSelected = { template ->
                    viewModel.onEvent(ResumePreviewEvent.SelectTemplate(template))
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Actions
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                item {
                    Button(
                        onClick = { viewModel.onEvent(ResumePreviewEvent.SharePdf) },
                        enabled = state.isPdfReady && !state.isPdfGenerating,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.share_pdf))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                item {
                    Button(
                        onClick = { viewModel.onEvent(ResumePreviewEvent.DownloadPdf) },
                        enabled = state.isPdfReady && !state.isPdfGenerating,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.download_pdf))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                item {
                    Button(
                        onClick = { viewModel.onEvent(ResumePreviewEvent.AnalyzeResume) },
                        enabled = !state.isAnalyzing,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.analyze_pdf))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status and errors
            when {
                state.isLoading -> {
                    LoadingIndicator()
                }

                state.error != null -> {
                    Text(
                        text = state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                state.isPdfGenerating -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.generating_pdf))
                    }
                }

                state.pdfGenerationError != null -> {
                    Text(
                        text = state.pdfGenerationError!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                state.isAnalyzing -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.analyzing_pdf))
                    }
                }

                state.analyzeError != null -> {
                    Text(
                        text = state.analyzeError!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                else -> {
                    // Анализ завершен, результаты будут показаны на отдельном экране
                    if (state.resume?.aiAnalysis?.lastAnalyzed ?: 0 > 0) {
                        Text(
                            text = stringResource(R.string.the_analysis_is_completed),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateSelector(
    selectedTemplate: PdfTemplateType,
    onTemplateSelected: (PdfTemplateType) -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.choose_template_style),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                PdfTemplateType.values().forEach { template ->
                    FilterChip(
                        selected = template == selectedTemplate,
                        onClick = { onTemplateSelected(template) },
                        label = {
                            Text(getTemplateDisplayName(template))
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }
    }
}

@Composable
private fun getTemplateDisplayName(template: PdfTemplateType): String {
    return when (template) {
        PdfTemplateType.MODERN -> "Современный"
        PdfTemplateType.CREATIVE -> "Креативный"
        PdfTemplateType.MINIMALIST -> "Минималистичный"
        PdfTemplateType.PROFESSIONAL -> "Профессиональный"
    }
}