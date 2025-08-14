package com.bober.autcsv.presentation.screens.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bober.autcsv.R
import com.bober.autcsv.core.utils.LlmLogger
import com.bober.autcsv.domain.model.CvAnalysis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutAnalysisScreen(
    resumeId: String,
    viewModel: AnalysisViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    LaunchedEffect(resumeId) {
        LlmLogger.logAnalysisScreenEvent("Screen launched", resumeId)
        viewModel.loadResumeAndAnalyze(resumeId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.resume_analysis)) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            LlmLogger.logUserAction("Back button pressed", "Analysis", resumeId)
                            onNavigateBack()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (val currentState = state) {
                AnalysisState.Initial -> {
                    LlmLogger.logAnalysisScreenEvent("State: Initial", resumeId)
                    CircularProgressIndicator()
                }
                AnalysisState.Loading -> {
                    LlmLogger.logAnalysisScreenEvent("State: Loading", resumeId)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Анализируем резюме...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                is AnalysisState.Success -> {
                    LlmLogger.logAnalysisScreenEvent("State: Success", resumeId)
                    LlmLogger.logAnalysisResult("Completeness", currentState.analysis.completenessAnalysis)
                    LlmLogger.logAnalysisResult("Logic", currentState.analysis.logicAnalysis)
                    LlmLogger.logAnalysisResult("Recommendations count", "${currentState.analysis.recommendations.size}")
                    AnalysisContent(
                        analysis = currentState.analysis,
                        resumeId = resumeId,
                        onRetry = { viewModel.retryAnalysis(resumeId) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is AnalysisState.Error -> {
                    LlmLogger.logError("Analysis screen error: ${currentState.message}")
                    ErrorContent(
                        message = currentState.message,
                        onRetry = { viewModel.retryAnalysis(resumeId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp)
        )
        Text(
            text = "Ошибка анализа",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        IconButton(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = "ReloadAn")
        }
    }
}

@Composable
private fun AnalysisContent(
    analysis: CvAnalysis,
    resumeId: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    LlmLogger.logUiEvent("Analysis", "Content rendered", "Recommendations: ${analysis.recommendations.size}")
    
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Rating section
        item {
            RatingCard(rating = analysis.rating)
        }
        
        // Analysis stats
        item {
            AnalysisStatsCard(analysis = analysis)
        }
        
        // Completeness analysis
        if (analysis.completenessAnalysis.isNotBlank()) {
            item {
                AnalysisCard(
                    title = "Анализ полноты",
                    content = analysis.completenessAnalysis,
                    icon = Icons.Default.Info,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // Logic analysis
        if (analysis.logicAnalysis.isNotBlank()) {
            item {
                AnalysisCard(
                    title = "Анализ логичности",
                    content = analysis.logicAnalysis,
                    icon = Icons.Default.Info,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        
        // Strengths section
        if (analysis.strengths.isNotEmpty()) {
            item {
                Text(
                    text = "Сильные стороны",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(analysis.strengths) { strength ->
                RecommendationCard(
                    text = strength,
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF4CAF50) // Green
                )
            }
        }
        
        // Improvements section
        if (analysis.improvements.isNotEmpty()) {
            item {
                Text(
                    text = "Зоны для улучшения",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(analysis.improvements) { improvement ->
                RecommendationCard(
                    text = improvement,
                    icon = Icons.Default.Warning,
                    color = Color(0xFFFF9800) // Orange
                )
            }
        }
        
        // Recommendations section
        if (analysis.recommendations.isNotEmpty()) {
            item {
                Text(
                    text = "Рекомендации",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(analysis.recommendations) { recommendation ->
                RecommendationCard(
                    text = recommendation,
                    icon = Icons.Default.Info,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // Empty state if no data
        if (analysis.completenessAnalysis.isBlank() && 
            analysis.logicAnalysis.isBlank() && 
            analysis.strengths.isEmpty() && 
            analysis.improvements.isEmpty() && 
            analysis.recommendations.isEmpty()) {
            item {
                EmptyAnalysisCard(onRetry = onRetry)
            }
        }
    }
}

@Composable
private fun RatingCard(
    rating: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Rating",
                    tint = Color(0xFFFFD700) // Gold
                )
                Text(
                    text = "Общая оценка",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "★".repeat(rating) + "☆".repeat(5 - rating),
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFFFFD700)
            )
            Text(
                text = "${rating}/5",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AnalysisCard(
    title: String,
    content: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun RecommendationCard(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Recommendation",
                tint = color,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun EmptyAnalysisCard(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "No data",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Анализ не содержит данных",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Попробуйте проанализировать резюме еще раз или проверьте содержимое резюме",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(onClick = onRetry) {
                Text("Повторить анализ")
            }
        }
    }
} 

@Composable
private fun AnalysisStatsCard(
    analysis: CvAnalysis,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Статистика анализа",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "Сильные стороны",
                    value = "${analysis.strengths.size}",
                    color = Color(0xFF4CAF50)
                )
                StatItem(
                    label = "Улучшения",
                    value = "${analysis.improvements.size}",
                    color = Color(0xFFFF9800)
                )
                StatItem(
                    label = "Рекомендации",
                    value = "${analysis.recommendations.size}",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
} 