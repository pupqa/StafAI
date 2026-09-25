package com.bober.autcsv.presentation.screens.analysis

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bober.autcsv.R
import com.bober.autcsv.core.utils.LlmLogger
import com.bober.autcsv.domain.model.CvAnalysis
import com.bober.autcsv.ui.theme.AppColors
import com.bober.autcsv.ui.theme.Body
import com.bober.autcsv.ui.theme.Display
import com.bober.autcsv.ui.theme.LG
import com.bober.autcsv.ui.theme.MD
import com.bober.autcsv.ui.theme.Mono
import kotlinx.coroutines.delay

/**
 * Экран анализа резюме: запускает анализ по ID, отображает прогресс,
 * результат или ошибку, и позволяет перейти к редактированию.
 *
 * Визуальный язык соответствует дизайн-макету «СтафИИ»: тёмная поверхность,
 * золотой акцент для основного интерфейса и индиго — для «голоса ИИ».
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutAnalysisScreen(
    resumeId: String,
    viewModel: AnalysisViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(resumeId) {
        LlmLogger.logAnalysisScreenEvent("Screen launched", resumeId)
        viewModel.loadResumeAndAnalyze(resumeId)
    }

    Scaffold(
        containerColor = AppColors.BackgroundSoft,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.resume_analysis),
                        fontFamily = Display,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp,
                        color = AppColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconCircleButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    ) {
                        LlmLogger.logUserAction("Back button pressed", "Analysis", resumeId)
                        onNavigateBack()
                    }
                },
                actions = {
                    IconCircleButton(
                        icon = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.cd_edit_resume)
                    ) {
                        LlmLogger.logUserAction("Edit button pressed", "Analysis", resumeId)
                        onNavigateToEdit(resumeId)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.BackgroundSoft,
                    titleContentColor = AppColors.TextPrimary
                )
            )
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(AppColors.BackgroundSoft)
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (val currentState = state) {
                AnalysisState.Initial -> {
                    LlmLogger.logAnalysisScreenEvent("State: Initial", resumeId)
                    CircularProgressIndicator(color = AppColors.Accent)
                }

                AnalysisState.Loading -> {
                    LlmLogger.logAnalysisScreenEvent("State: Loading", resumeId)
                    LoadingContent()
                }

                is AnalysisState.Success -> {
                    LlmLogger.logAnalysisScreenEvent("State: Success", resumeId)
                    LlmLogger.logAnalysisResult(
                        "Completeness",
                        currentState.analysis.completenessAnalysis
                    )
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            AnalysisContent(
                                analysis = currentState.analysis,
                                resumeId = resumeId,
                                onRetry = { viewModel.retryAnalysis(resumeId) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        VacancyMatcherCard(viewModel = viewModel)
                    }
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

// ─────────────────────────────────────────────────────────────────────────────
// Палитра и типографика макета «СтафИИ»
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Цвета из дизайн-системы макета. При наличии единой темы приложения
 * их стоит перенести в `ui/theme` и переиспользовать в обоих экранах.
 */


/**
 * Радиусы и шрифты берутся из единой темы приложения `com.bober.autcsv.ui.theme`
 * (см. Radius.kt / Fonts.kt), чтобы не расходиться между экранами.
 */

// ─────────────────────────────────────────────────────────────────────────────
// Сопоставление с вакансией
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Сворачиваемая карточка внизу экрана анализа: вставка текста вакансии и
 * LLM-оценка соответствия резюме.
 */
@Composable
private fun VacancyMatcherCard(viewModel: AnalysisViewModel) {
    val matcher by viewModel.matcher.collectAsStateWithLifecycle()
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        border = BorderStroke(1.dp, AppColors.BorderSoft)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.vacancy_match_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = AppColors.Accent
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = matcher.vacancyText,
                    onValueChange = viewModel::onVacancyTextChanged,
                    placeholder = { Text(stringResource(R.string.vacancy_match_hint)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 90.dp, max = 150.dp),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = viewModel::matchVacancy,
                    enabled = matcher.vacancyText.isNotBlank() && !matcher.isLoading,
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    if (matcher.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.vacancy_match_run))
                }

                matcher.error?.let { error ->
                    Text(
                        text = error,
                        color = AppColors.Danger,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                matcher.result?.let { result ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.vacancy_match_score),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${result.score}/100",
                            color = AppColors.Accent,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }
                    if (result.verdict.isNotBlank()) {
                        Text(
                            text = result.verdict,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    if (result.matchedKeywords.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.vacancy_match_matched),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        KeywordChips(result.matchedKeywords, AppColors.Accent)
                    }
                    if (result.missingKeywords.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.vacancy_match_missing),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        KeywordChips(result.missingKeywords, AppColors.Danger)
                    }
                }
            }
        }
    }
}

/** Строка чипов ключевых слов. */
@Composable
private fun KeywordChips(keywords: List<String>, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        keywords.forEach { keyword ->
            Text(
                text = keyword,
                fontSize = 11.sp,
                color = color,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Общие мелкие компоненты интерфейса
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun IconCircleButton(
    icon: ImageVector,
    contentDescription: String?,
    tint: Color = AppColors.TextPrimary,
    background: Color = AppColors.Surface,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(start = 12.dp, top = 6.dp, bottom = 6.dp)
            .size(36.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(17.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(56.dp),
            color = AppColors.Accent,
            trackColor = AppColors.Surface3
        )
        Text(
            text = stringResource(R.string.analysis_loading),
            fontFamily = Body,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary
        )
        Text(
            text = stringResource(R.string.analysis_loading_hint),
            fontFamily = Body,
            fontSize = 13.sp,
            color = AppColors.TextDim
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Error
// ─────────────────────────────────────────────────────────────────────────────

@Composable
/** Карточка ошибки с повторной попыткой анализа. */
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(24.dp)
            .clip(RoundedCornerShape(LG))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.DangerSoft, RoundedCornerShape(LG))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(MD))
                .background(AppColors.DangerSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = stringResource(R.string.error_generic),
                tint = AppColors.Danger,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = stringResource(R.string.analysis_error_title),
            fontFamily = Display,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            color = AppColors.TextPrimary
        )
        Text(
            text = message,
            fontFamily = Body,
            fontSize = 14.sp,
            color = AppColors.TextDim,
            textAlign = TextAlign.Center
        )
        PillButton(
            text = stringResource(R.string.retry),
            icon = Icons.Default.Refresh,
            filled = true,
            onClick = onRetry
        )
    }
}

/** Кнопка-пилюля в двух вариантах: заливка акцентом или контур — как `.btn-fill` / `.btn-outline`. */
@Composable
private fun PillButton(
    text: String,
    icon: ImageVector? = null,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (filled) AppColors.Accent else Color.Transparent
    val contentColor = if (filled) AppColors.AccentInk else AppColors.TextPrimary
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(MD))
            .background(bg)
            .then(
                if (!filled) Modifier.border(
                    1.5.dp,
                    AppColors.Border,
                    RoundedCornerShape(MD)
                )
                else Modifier
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(15.dp)
            )
        }
        Text(
            text = text,
            fontFamily = Body,
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp,
            color = contentColor
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AppColors.Success — основной контент
// ─────────────────────────────────────────────────────────────────────────────

@Composable
/** Контент с деталями анализа: рейтинг, статистика, секции и рекомендации. */
private fun AnalysisContent(
    analysis: CvAnalysis,
    resumeId: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LlmLogger.logUiEvent(
        "Analysis",
        "Content rendered",
        "Recommendations: ${analysis.recommendations.size}"
    )

    val isEmpty = analysis.completenessAnalysis.isBlank() &&
            analysis.logicAnalysis.isBlank() &&
            analysis.strengths.isEmpty() &&
            analysis.improvements.isEmpty() &&
            analysis.recommendations.isEmpty()

    LazyColumn(
        modifier = modifier
            .background(AppColors.BackgroundSoft)
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp)
    ) {

        // Индиго-хиро с рейтингом — «голос ИИ»
        item {
            RatingHero(
                rating = analysis.rating,
                completenessScore = analysis.completenessScore.takeIf { it > 0 }
            )
        }

        // Полоса прогресса заполненности резюме
        if (analysis.completenessScore > 0) {
            item { CompletenessProgressCard(score = analysis.completenessScore) }
        }

        // Сводная статистика: сильные / улучшения / рекомендации
        item { StatsRow(analysis = analysis) }

        // Анализ полноты
        if (analysis.completenessAnalysis.isNotBlank()) {
            item {
                AnalysisSectionCard(
                    title = stringResource(R.string.analysis_completeness),
                    content = analysis.completenessAnalysis,
                    icon = Icons.Default.TaskAlt,
                    accentColor = AppColors.Accent,
                    accentSoft = AppColors.AmberSoft
                )
            }
        }

        // Анализ логичности
        if (analysis.logicAnalysis.isNotBlank()) {
            item {
                AnalysisSectionCard(
                    title = stringResource(R.string.analysis_structure),
                    content = analysis.logicAnalysis,
                    icon = Icons.Default.AccountTree,
                    accentColor = AppColors.Mint,
                    accentSoft = AppColors.MintSoft
                )
            }
        }

        // Сильные стороны — открыт по умолчанию, как в макете
        if (analysis.strengths.isNotEmpty()) {
            item {
                AccordionSection(
                    title = stringResource(R.string.analysis_strengths),
                    icon = Icons.Default.CheckCircle,
                    accentColor = AppColors.Success,
                    accentSoft = AppColors.SuccessSoft,
                    initiallyExpanded = true
                ) {
                    analysis.strengths.forEach { strength ->
                        AccordionBullet(
                            text = strength,
                            icon = Icons.Default.CheckCircle,
                            color = AppColors.Success
                        )
                    }
                }
            }
        }

        // Зоны для улучшения
        if (analysis.improvements.isNotEmpty()) {
            item {
                AccordionSection(
                    title = stringResource(R.string.analysis_improvements),
                    icon = Icons.Default.Warning,
                    accentColor = AppColors.Accent,
                    accentSoft = AppColors.AmberSoft
                ) {
                    analysis.improvements.forEach { improvement ->
                        AccordionBullet(
                            text = improvement,
                            icon = Icons.Default.Warning,
                            color = AppColors.Accent
                        )
                    }
                }
            }
        }

        // Рекомендации
        if (analysis.recommendations.isNotEmpty()) {
            item {
                AccordionSection(
                    title = stringResource(R.string.analysis_recommendations),
                    icon = Icons.Default.Lightbulb,
                    accentColor = AppColors.Mint,
                    accentSoft = AppColors.MintSoft
                ) {
                    analysis.recommendations.forEach { recommendation ->
                        AccordionBullet(
                            text = recommendation,
                            icon = Icons.Default.Lightbulb,
                            color = AppColors.Mint
                        )
                    }
                }
            }
        }

        // Дополнительные параметры
        if (analysis.additionalParameters.isNotEmpty()) {
            item { SectionHeader(title = stringResource(R.string.analysis_additional_params)) }
            item { AdditionalParametersCard(parameters = analysis.additionalParameters) }
        }

        // Пустое состояние при полном отсутствии данных
        if (isEmpty) {
            item { EmptyAnalysisCard(onRetry = onRetry) }
        }

        // Мета-подпись, как в макете: дата анализа
        if (!isEmpty) {
            item {
                Text(
                    text = stringResource(R.string.analysis_completed, resumeId.take(8)),
                    fontFamily = Mono,
                    fontSize = 11.sp,
                    color = AppColors.TextFaint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 4.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Компонент: индиго-хиро с рейтингом (аналог .rating-hero из макета)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
/** Карточка общего рейтинга: звёзды «загораются» пружинной анимацией при появлении. */
private fun RatingHero(
    rating: Int,
    completenessScore: Int?,
    modifier: Modifier = Modifier,
) {
    val clamped = rating.coerceIn(0, 5)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LG))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        AppColors.Mint.copy(alpha = 0.35f),
                        AppColors.MintSoft
                    ),
                    center = Offset(0.15f, 0f),
                    radius = 900f
                )
            )
            .border(
                width = 1.dp,
                color = AppColors.Mint.copy(alpha = 0.25f),
                shape = RoundedCornerShape(LG)
            )
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = stringResource(R.string.cd_rating),
                tint = AppColors.Accent,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = stringResource(R.string.analysis_overall_rating),
                fontFamily = Body,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = Color(0xFFBFE8D2)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(5) { index ->
                AnimatedStar(filled = index < clamped, delayMillis = index * 70)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "$clamped",
                fontFamily = Mono,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = AppColors.TextPrimary
            )
            Text(
                text = " / 5.0",
                fontFamily = Mono,
                fontSize = 13.sp,
                color = AppColors.TextDim
            )
            if (completenessScore != null) {
                Text(
                    text = stringResource(R.string.analysis_completeness_detail, completenessScore),
                    fontFamily = Mono,
                    fontSize = 13.sp,
                    color = AppColors.TextDim
                )
            }
        }
    }
}

@Composable
private fun AnimatedStar(filled: Boolean, delayMillis: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        visible = true
    }
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "starPop"
    )
    Icon(
        imageVector = Icons.Default.Star,
        contentDescription = null,
        tint = if (filled) AppColors.Accent else AppColors.TextFaint,
        modifier = Modifier
            .size(24.dp)
            .scale(0.4f + 0.6f * progress)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Компонент: полоса заполненности резюме
// ─────────────────────────────────────────────────────────────────────────────

@Composable
/** Анимированная полоса прогресса — процент заполненности резюме (0–100%). */
private fun CompletenessProgressCard(
    score: Int,
    modifier: Modifier = Modifier,
) {
    var triggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { triggered = true }
    val animatedProgress by animateFloatAsState(
        targetValue = if (triggered) score / 100f else 0f,
        animationSpec = tween(durationMillis = 900),
        label = "completenessProgress"
    )

    val barColor = when {
        score >= 80 -> AppColors.Success
        score >= 55 -> AppColors.Accent
        else -> AppColors.Danger
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LG))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.BorderSoft, RoundedCornerShape(LG))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.analysis_resume_completeness),
                fontFamily = Body,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = AppColors.TextPrimary
            )
            Text(
                text = "$score%",
                fontFamily = Mono,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = barColor
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50)),
            color = barColor,
            trackColor = AppColors.Surface3,
            strokeCap = StrokeCap.Round
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Компонент: сводная статистика (аналог .stats-row из макета)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatsRow(
    analysis: CvAnalysis,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            value = "${analysis.strengths.size}",
            label = stringResource(R.string.analysis_strengths_label),
            color = AppColors.Success,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            value = "${analysis.improvements.size}",
            label = stringResource(R.string.analysis_improvements_label),
            color = AppColors.Accent,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            value = "${analysis.recommendations.size}",
            label = stringResource(R.string.analysis_recommendations_label),
            color = AppColors.Mint,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(MD))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.BorderSoft, RoundedCornerShape(MD))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            fontFamily = Mono,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = color
        )
        Text(
            text = label,
            fontFamily = Body,
            fontSize = 10.5.sp,
            color = AppColors.TextFaint,
            textAlign = TextAlign.Center,
            lineHeight = 13.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Компонент: секция анализа (текстовый блок)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
/** Универсальная карточка секции анализа с заголовком, иконкой и текстом. */
private fun AnalysisSectionCard(
    title: String,
    content: String,
    icon: ImageVector,
    accentColor: Color,
    accentSoft: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LG))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.BorderSoft, RoundedCornerShape(LG))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(accentSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = title,
                fontFamily = Display,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = AppColors.TextPrimary
            )
        }
        Text(
            text = content,
            fontFamily = Body,
            fontSize = 13.5.sp,
            lineHeight = 20.sp,
            color = AppColors.TextDim
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Компонент: аккордеон (аналог .accordion из макета)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
/**
 * Секция-аккордеон: цветная иконка в скруглённом квадрате, заголовок,
 * шеврон, разворачивающийся список пунктов. Повторяет `.accordion` из макета.
 */
private fun AccordionSection(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    accentSoft: Color,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(200),
        label = "chevron"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LG))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.BorderSoft, RoundedCornerShape(LG))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(accentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Text(
                    text = title,
                    fontFamily = Body,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = AppColors.TextPrimary
                )
            }
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = stringResource(if (expanded) R.string.analysis_collapse else R.string.analysis_expand),
                tint = AppColors.TextFaint,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(chevronRotation)
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        }
    }
}

@Composable
private fun AccordionBullet(
    text: String,
    icon: ImageVector,
    color: Color,
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(14.dp)
                .padding(top = 2.dp)
        )
        Text(
            text = text,
            fontFamily = Body,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = AppColors.TextDim
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Компонент: дополнительные параметры
// ─────────────────────────────────────────────────────────────────────────────

@Composable
/** Карточка с таблицей дополнительных параметров (ключ → значение). */
private fun AdditionalParametersCard(
    parameters: Map<String, String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LG))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.BorderSoft, RoundedCornerShape(LG))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        parameters.entries.forEachIndexed { index, (key, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = key,
                    fontFamily = Body,
                    fontSize = 13.sp,
                    color = AppColors.TextDim,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = value,
                    fontFamily = Mono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = AppColors.TextPrimary,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                )
            }
            if (index < parameters.size - 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(AppColors.BorderSoft)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Компонент: заголовок секции
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title.uppercase(),
        fontFamily = Mono,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.7.sp,
        color = AppColors.TextFaint,
        modifier = modifier.padding(top = 6.dp, bottom = 2.dp, start = 2.dp)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Компонент: пустое состояние
// ─────────────────────────────────────────────────────────────────────────────

@Composable
/** Плашка пустого состояния с кнопкой повторного анализа. */
private fun EmptyAnalysisCard(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LG))
            .background(AppColors.Surface2)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(MD))
                .background(AppColors.Surface3),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = stringResource(R.string.no_data),
                tint = AppColors.TextDim,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = stringResource(R.string.analysis_empty_title),
            fontFamily = Display,
            fontWeight = FontWeight.Bold,
            fontSize = 14.5.sp,
            color = AppColors.TextPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.analysis_empty_hint),
            fontFamily = Body,
            fontSize = 13.sp,
            color = AppColors.TextDim,
            textAlign = TextAlign.Center
        )
        PillButton(
            text = stringResource(R.string.analysis_retry),
            icon = Icons.Default.Refresh,
            filled = true,
            onClick = onRetry
        )
    }
}