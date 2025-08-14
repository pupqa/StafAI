package com.bober.autcsv.presentation.screens.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bober.autcsv.R
import com.bober.autcsv.core.pdf.PdfTemplateType
import com.bober.autcsv.core.utils.ResumeValidator
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
                .padding(16.dp)
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

            Spacer(modifier = Modifier.height(16.dp))

            // Actions
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                item {
                    Button(
                        onClick = { viewModel.onEvent(ResumePreviewEvent.SharePdf) },
                        enabled = state.isPdfReady && !state.isPdfGenerating
                    ) {
                        Text(stringResource(R.string.share_pdf))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                item {
                    Button(
                        onClick = { viewModel.onEvent(ResumePreviewEvent.DownloadPdf) },
                        enabled = state.isPdfReady && !state.isPdfGenerating
                    ) {
                        Text(stringResource(R.string.download_pdf))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                item {
                    Button(
                        onClick = { viewModel.onEvent(ResumePreviewEvent.AnalyzeResume) },
                        enabled = !state.isAnalyzing
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
//
//@Composable
//private fun ResumeQualityCard(resume: com.bober.autcsv.domain.model.Resume) {
//    val validationResult = ResumeValidator.validateResume(resume)
//    val qualityLevel = ResumeValidator.getResumeQualityLevel(resume)
//    val tips = ResumeValidator.generateResumeTips(resume)
//
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//    ) {
//        Column(
//            modifier = Modifier.padding(16.dp)
//        ) {
//            // Header
//            Row(
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Icon(
//                    imageVector = when {
//                        validationResult.completenessScore >= 75 -> Icons.Default.CheckCircle
//                        validationResult.completenessScore >= 60 -> Icons.Default.Info
//                        else -> Icons.Default.Warning
//                    },
//                    contentDescription = null,
//                    tint = when {
//                        validationResult.completenessScore >= 75 -> Color(0xFF4CAF50)
//                        validationResult.completenessScore >= 60 -> Color(0xFF2196F3)
//                        else -> Color(0xFFFF9800)
//                    }
//                )
//                Spacer(modifier = Modifier.width(8.dp))
//                Text(
//                    text = "Качество резюме: $qualityLevel",
//                    style = MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.Bold
//                )
//            }
//
//            Spacer(modifier = Modifier.height(12.dp))
//
//            // Progress bar
//            Column {
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween
//                ) {
//                    Text(
//                        text = "Заполненность",
//                        style = MaterialTheme.typography.bodyMedium
//                    )
//                    Text(
//                        text = "${validationResult.completenessScore}%",
//                        style = MaterialTheme.typography.bodyMedium,
//                        fontWeight = FontWeight.Bold
//                    )
//                }
//                Spacer(modifier = Modifier.height(4.dp))
//                LinearProgressIndicator(
//                    progress = validationResult.completenessScore / 100f,
//                    modifier = Modifier.fillMaxWidth(),
//                    color = when {
//                        validationResult.completenessScore >= 75 -> Color(0xFF4CAF50)
//                        validationResult.completenessScore >= 60 -> Color(0xFF2196F3)
//                        else -> Color(0xFFFF9800)
//                    }
//                )
//            }
//
//            Spacer(modifier = Modifier.height(12.dp))
//
//            // Missing fields
//            if (validationResult.missingFields.isNotEmpty()) {
//                Text(
//                    text = "Отсутствующие поля:",
//                    style = MaterialTheme.typography.bodyMedium,
//                    fontWeight = FontWeight.Bold
//                )
//                Spacer(modifier = Modifier.height(4.dp))
//                validationResult.missingFields.take(3).forEach { field ->
//                    Text(
//                        text = "• $field",
//                        style = MaterialTheme.typography.bodySmall,
//                        color = MaterialTheme.colorScheme.error
//                    )
//                }
//                if (validationResult.missingFields.size > 3) {
//                    Text(
//                        text = "... и еще ${validationResult.missingFields.size - 3}",
//                        style = MaterialTheme.typography.bodySmall,
//                        color = MaterialTheme.colorScheme.error
//                    )
//                }
//                Spacer(modifier = Modifier.height(8.dp))
//            }
//
//            // Tips
//            if (tips.isNotEmpty()) {
//                Text(
//                    text = "Рекомендации:",
//                    style = MaterialTheme.typography.bodyMedium,
//                    fontWeight = FontWeight.Bold
//                )
//                Spacer(modifier = Modifier.height(4.dp))
//                tips.take(2).forEach { tip ->
//                    Text(
//                        text = tip,
//                        style = MaterialTheme.typography.bodySmall,
//                        color = MaterialTheme.colorScheme.primary
//                    )
//                }
//                if (tips.size > 2) {
//                    Text(
//                        text = "... и еще ${tips.size - 2} рекомендаций",
//                        style = MaterialTheme.typography.bodySmall,
//                        color = MaterialTheme.colorScheme.primary
//                    )
//                }
//            }
//        }
//    }
//}

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
        PdfTemplateType.TEST -> stringResource(R.string.template_test)
    }
}