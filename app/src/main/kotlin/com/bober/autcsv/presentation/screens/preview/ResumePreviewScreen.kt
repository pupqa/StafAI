package com.bober.autcsv.presentation.screens.preview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bober.autcsv.R
import com.bober.autcsv.core.pdf.PdfSection
import com.bober.autcsv.core.pdf.PdfTemplateType
import com.bober.autcsv.core.utils.PdfPrintHelper
import com.bober.autcsv.presentation.common.components.LoadingIndicator
import com.bober.autcsv.ui.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

// ---------------------------------------------------------------------------
private object Radius {
    val Xl = 28.dp
    val Lg = 20.dp
    val Md = 14.dp
    val Sm = 10.dp
    val Pill = 999.dp
}

private object Type {
    val Display = FontFamily.SansSerif
    val Body = FontFamily.Default
    val Mono = FontFamily.Monospace
}

/** Предлагаемые акцентные цвета PDF (№18): нейтральные + фирменные. */
private val ACCENT_CHOICES = listOf(
    0xFF1976D2.toInt(), // синий
    0xFF4F46E5.toInt(), // индиго
    0xFF0D9488.toInt(), // бирюзовый
    0xFF7C3AED.toInt(), // фиолетовый
    0xFFDC2626.toInt(), // красный
    0xFFEA580C.toInt(), // оранжевый
)

// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumePreviewScreen(
    resumeId: String,
    viewModel: ResumePreviewViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToAnalysis: (String) -> Unit,
    onNavigateToCoverLetter: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showTranslateDialog by remember { mutableStateOf(false) }
    // Шаблон/цвет/порядок секций переехали в sheet: документ — главный контент
    var showStyleSheet by remember { mutableStateOf(false) }

    LaunchedEffect(resumeId) {
        viewModel.onEvent(ResumePreviewEvent.LoadResume(resumeId))
    }

    // Навигация к анализу — по состоянию VM, а не по колбэку:
    // не ломается после пересоздания Activity и переживает процесс
    LaunchedEffect(state.navigateToAnalysisResumeId) {
        state.navigateToAnalysisResumeId?.let {
            onNavigateToAnalysis(it)
            viewModel.consumeAnalysisNavigation()
        }
    }

    Scaffold(
        containerColor = AppColors.BackgroundSoft,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.preview),
                        fontFamily = Type.Display,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp,
                        color = AppColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconCircleButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        onClick = onNavigateBack
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.BackgroundSoft,
                    titleContentColor = AppColors.TextPrimary
                )
            )
        },
        bottomBar = {
            StickyActionsBar(
                pdfReady = state.isPdfReady,
                busy = state.isPdfGenerating || state.isAnalyzing || state.isTranslating,
                onShare = { viewModel.onEvent(ResumePreviewEvent.SharePdf) },
                onDownload = { viewModel.onEvent(ResumePreviewEvent.DownloadPdf) },
                onAnalyze = { viewModel.onEvent(ResumePreviewEvent.AnalyzeResume) },
                onCoverLetter = onNavigateToCoverLetter,
                onTranslate = { showTranslateDialog = true }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.BackgroundSoft)
                .padding(padding)
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            StyleSettingsButton(
                onClick = { showStyleSheet = true },
                modifier = Modifier.padding(bottom = 14.dp),
            )

            // Реальный документ выбранного шаблона: все страницы PDF
            DocumentPreviewCard(
                template = state.selectedTemplate,
                pages = state.previewPages,
                fallback = state.thumbnails[state.selectedTemplate],
                isBusy = state.isPdfGenerating || state.previewPages.any { it == null },
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        LoadingIndicator()
                    }
                }

                state.error != null -> {
                    InlineStatusBanner(message = state.error!!, isError = true)
                }

                state.isPdfGenerating -> {
                    InlineBusyRow(text = stringResource(R.string.generating_pdf))
                }

                state.pdfGenerationError != null -> {
                    InlineStatusBanner(message = state.pdfGenerationError!!, isError = true)
                }

                state.isAnalyzing -> {
                    InlineBusyRow(text = rotatingAnalysisHint())
                }

                state.isTranslating -> {
                    InlineBusyRow(text = stringResource(R.string.translate_running))
                }

                state.analyzeError != null -> {
                    InlineStatusBanner(message = state.analyzeError!!, isError = true)
                }

                else -> {
                    if ((state.resume?.aiAnalysis?.lastAnalyzed ?: 0) > 0) {
                        InlineStatusBanner(
                            message = stringResource(R.string.the_analysis_is_completed),
                            isError = false
                        )
                    }
                }
            }

            state.exportMessage?.let { message ->
                Spacer(modifier = Modifier.height(10.dp))
                InlineStatusBanner(message = message, isError = false)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Экспорт в другие форматы и печать (№11/№12/№17)
            val printJobName = stringResource(R.string.preview_fallback_title)
            ExportSection(
                busy = state.isExporting,
                onDocx = { viewModel.onEvent(ResumePreviewEvent.ExportDocx) },
                onCsv = { viewModel.onEvent(ResumePreviewEvent.ExportCsv) },
                onPrint = {
                    val path = state.pdfPath
                    if (path != null) {
                        PdfPrintHelper.print(
                            context,
                            File(path),
                            state.resume?.personalInfo?.fullName?.ifBlank { null } ?: printJobName,
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Диалог выбора языка LLM-перевода
    if (showTranslateDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTranslateDialog = false },
            title = { Text(stringResource(R.string.translate_dialog_title)) },
            text = {
                Text(
                    text = stringResource(
                        R.string.translate_dialog_hint_fmt,
                        state.resume?.personalInfo?.fullName.orEmpty()
                    ),
                    color = AppColors.TextDim,
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showTranslateDialog = false
                    viewModel.translateResume("en")
                }) { Text(stringResource(R.string.translate_to_en)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showTranslateDialog = false
                    viewModel.translateResume("ru")
                }) { Text(stringResource(R.string.translate_to_ru)) }
            }
        )
    }

    // Оформление документа: шаблон, акцентный цвет, порядок секций
    if (showStyleSheet) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showStyleSheet = false },
            containerColor = AppColors.Surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 28.dp)
            ) {
                TemplateSelector(
                    selectedTemplate = state.selectedTemplate,
                    thumbnails = state.thumbnails,
                    onTemplateSelected = { template ->
                        viewModel.onEvent(ResumePreviewEvent.SelectTemplate(template))
                    }
                )
                Spacer(modifier = Modifier.height(18.dp))
                AccentColorRow(
                    selected = state.accentColor,
                    onSelect = { viewModel.onEvent(ResumePreviewEvent.SelectAccentColor(it)) }
                )
                Spacer(modifier = Modifier.height(18.dp))
                SectionOrderEditor(
                    order = state.sectionOrder,
                    onMove = { section, up ->
                        viewModel.onEvent(ResumePreviewEvent.MoveSection(section, up))
                    }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Общие мелкие компоненты
// ---------------------------------------------------------------------------

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

@Composable
private fun InlineBusyRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.Md))
            .background(AppColors.Surface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = AppColors.Accent,
            trackColor = AppColors.Surface3
        )
        Text(text = text, fontFamily = Type.Body, fontSize = 13.5.sp, color = AppColors.TextDim)
    }
}

/** Пилюля-кнопка открытия sheet «Оформление». */
@Composable
private fun StyleSettingsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.Pill))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.BorderSoft, RoundedCornerShape(Radius.Pill))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Palette,
            contentDescription = null,
            tint = AppColors.Accent,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = stringResource(R.string.style_settings_button),
            fontFamily = Type.Mono,
            fontSize = 12.sp,
            color = AppColors.TextDim
        )
    }
}

/**
 * Подсказка этапа анализа, меняющаяся каждые ~2,6 секунды: длинная LLM-
 * операция со спиннером выглядит зависанием — смена сообщений показывает
 * живой процесс без реальной телеметрии от API.
 */
@Composable
private fun rotatingAnalysisHint(): String {
    val hints = listOf(
        stringResource(R.string.analysis_stage_1),
        stringResource(R.string.analysis_stage_2),
        stringResource(R.string.analysis_stage_3),
        stringResource(R.string.analysis_stage_4),
    )
    var index by remember { mutableStateOf(0) }
    LaunchedEffect(hints) {
        while (true) {
            delay(2600)
            index = (index + 1) % hints.size
        }
    }
    return hints[index % hints.size]
}

@Composable
private fun InlineStatusBanner(message: String, isError: Boolean) {
    val accent = if (isError) AppColors.Danger else AppColors.Success
    val bg = if (isError) AppColors.DangerSoft else AppColors.SuccessSoft
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.Md))
            .background(bg)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = if (isError) Icons.Default.Warning else Icons.Default.CheckCircle,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = message,
            fontFamily = Type.Body,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            color = AppColors.TextPrimary
        )
    }
}

// ---------------------------------------------------------------------------
// Лента живых мини-превью стилей (№16): настоящая первая страница PDF
// ---------------------------------------------------------------------------

@Composable
private fun TemplateSelector(
    selectedTemplate: PdfTemplateType,
    thumbnails: Map<PdfTemplateType, ImageBitmap>,
    onTemplateSelected: (PdfTemplateType) -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.choose_template_style).uppercase(),
            fontFamily = Type.Mono,
            fontSize = 11.sp,
            letterSpacing = 0.7.sp,
            color = AppColors.TextFaint,
            modifier = Modifier.padding(bottom = 10.dp, start = 2.dp)
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PdfTemplateType.entries.forEach { template ->
                StyleThumb(
                    label = templateDisplayName(template),
                    preview = thumbnails[template],
                    selected = template == selectedTemplate,
                    onClick = { onTemplateSelected(template) }
                )
            }
        }
    }
}

@Composable
private fun StyleThumb(
    label: String,
    preview: ImageBitmap?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(78.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.Surface2)
            .border(
                width = 2.dp,
                color = if (selected) AppColors.Accent else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(595f / 842f)
                .clip(RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (preview != null) {
                Image(
                    bitmap = preview,
                    contentDescription = label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Мини-превью рендерится в фоне — показываем скелет
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                )
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 1.5.dp,
                    color = AppColors.Accent
                )
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = label,
            fontFamily = Type.Body,
            fontSize = 9.sp,
            color = if (selected) AppColors.TextPrimary else AppColors.TextFaint,
            maxLines = 1
        )
    }
}

// ---------------------------------------------------------------------------
// Акцентный цвет (№18)
// ---------------------------------------------------------------------------

@Composable
private fun AccentColorRow(
    selected: Int?,
    onSelect: (Int?) -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.accent_color_title).uppercase(),
            fontFamily = Type.Mono,
            fontSize = 11.sp,
            letterSpacing = 0.7.sp,
            color = AppColors.TextFaint,
            modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // «Шаблона» — фирменный цвет текущего шаблона
            AccentSwatch(
                color = null,
                label = stringResource(R.string.accent_color_default),
                selected = selected == null,
                onClick = { onSelect(null) }
            )
            ACCENT_CHOICES.forEach { color ->
                AccentSwatch(
                    color = color,
                    label = null,
                    selected = selected == color,
                    onClick = { onSelect(color) }
                )
            }
        }
    }
}

@Composable
private fun AccentSwatch(
    color: Int?,
    label: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(color?.let { Color(it) } ?: AppColors.Surface2)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) AppColors.Accent else AppColors.BorderSoft,
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            if (color == null) {
                Text(
                    text = "Aa",
                    fontFamily = Type.Mono,
                    fontSize = 11.sp,
                    color = AppColors.TextDim
                )
            }
        }
        if (label != null) {
            Text(
                text = label,
                fontFamily = Type.Mono,
                fontSize = 8.5.sp,
                color = AppColors.TextFaint,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Порядок секций (№24): стрелки вверх/вниз вместо полноценного drag&drop
// ---------------------------------------------------------------------------

@Composable
private fun SectionOrderEditor(
    order: List<PdfSection>,
    onMove: (PdfSection, Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.Lg))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.BorderSoft, RoundedCornerShape(Radius.Lg))
            .padding(14.dp)
    ) {
        Text(
            text = stringResource(R.string.section_order_title),
            fontFamily = Type.Display,
            fontWeight = FontWeight.Bold,
            fontSize = 14.5.sp,
            color = AppColors.TextPrimary
        )
        Text(
            text = stringResource(R.string.section_order_hint),
            fontFamily = Type.Body,
            fontSize = 11.5.sp,
            color = AppColors.TextFaint,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
        )
        order.forEachIndexed { index, section ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${index + 1}. ${sectionDisplayName(section)}",
                    fontFamily = Type.Body,
                    fontSize = 13.sp,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconCircleButton(
                    icon = Icons.Default.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.cd_move_up),
                    background = AppColors.Surface2,
                    tint = if (index == 0) AppColors.TextFaint else AppColors.TextPrimary,
                    onClick = { onMove(section, true) }
                )
                IconCircleButton(
                    icon = Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.cd_move_down),
                    background = AppColors.Surface2,
                    tint = if (index == order.lastIndex) AppColors.TextFaint else AppColors.TextPrimary,
                    onClick = { onMove(section, false) }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Документ-превью: все страницы актуального PDF выбранного шаблона.
// Страницы листаются свайпом и стрелками; пока битмап страницы рендерится —
// в слоте спиннер. Если рендер ещё не начат, показывается мини-превью [fallback].
// ---------------------------------------------------------------------------

@Composable
private fun DocumentPreviewCard(
    template: PdfTemplateType,
    pages: List<ImageBitmap?>,
    fallback: ImageBitmap?,
    isBusy: Boolean,
) {
    val pageCount = pages.size
    val pagerState = rememberPagerState(pageCount = { pageCount })
    // Новый документ (смена шаблона/цвета/порядка секций) — с первой страницы
    LaunchedEffect(template, pageCount) {
        pagerState.scrollToPage(0)
    }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.Md))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.BorderSoft, RoundedCornerShape(Radius.Md))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = templateDisplayName(template),
                fontFamily = Type.Display,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 1.5.dp,
                    color = AppColors.Accent
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.previews_loading),
                    fontFamily = Type.Mono,
                    fontSize = 10.sp,
                    color = AppColors.TextFaint
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        if (pageCount == 0) {
            PreviewPageSlot(
                bitmap = fallback,
                contentDescription = templateDisplayName(template),
            )
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                pageSpacing = 8.dp,
            ) { index ->
                PreviewPageSlot(
                    bitmap = pages.getOrNull(index),
                    contentDescription = templateDisplayName(template),
                )
            }

            if (pageCount > 1) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    PageNavArrow(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.preview_prev_page),
                        enabled = pagerState.currentPage > 0,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    )
                    Text(
                        text = stringResource(
                            R.string.preview_page_x_of_y,
                            pagerState.currentPage + 1,
                            pageCount
                        ),
                        fontFamily = Type.Mono,
                        fontSize = 11.sp,
                        color = AppColors.TextDim
                    )
                    PageNavArrow(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.preview_next_page),
                        enabled = pagerState.currentPage < pageCount - 1,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    )
                }
            }
        }
    }
}

/** Слот одной страницы документа: битмап или скелет со спиннером. */
@Composable
private fun PreviewPageSlot(
    bitmap: ImageBitmap?,
    contentDescription: String?,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(595f / 842f)
            .clip(RoundedCornerShape(Radius.Sm))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 1.5.dp,
                color = AppColors.Accent
            )
        }
    }
}

@Composable
private fun PageNavArrow(
    icon: ImageVector,
    contentDescription: String?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            // 36 dp вместо 30: ближе к рекомендованным 48 dp цели касания,
            // не ломая визуальную плотность строки навигации
            .size(36.dp)
            .clip(CircleShape)
            .background(if (enabled) AppColors.Surface2 else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) AppColors.TextPrimary else AppColors.TextFaint,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Экспорт: DOCX / CSV / печать (№11, №12, №17)
// ---------------------------------------------------------------------------

@Composable
private fun ExportSection(
    busy: Boolean,
    onDocx: () -> Unit,
    onCsv: () -> Unit,
    onPrint: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.Lg))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.BorderSoft, RoundedCornerShape(Radius.Lg))
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.export_section_title),
            fontFamily = Type.Display,
            fontWeight = FontWeight.Bold,
            fontSize = 14.5.sp,
            color = AppColors.TextPrimary
        )
        Text(
            text = stringResource(R.string.export_hint),
            fontFamily = Type.Body,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            color = AppColors.TextFaint,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ExportPill(
                text = stringResource(R.string.export_docx),
                icon = Icons.Outlined.Description,
                enabled = !busy,
                onClick = onDocx,
                modifier = Modifier.weight(1f)
            )
            ExportPill(
                text = stringResource(R.string.export_csv),
                icon = Icons.Outlined.TableChart,
                enabled = !busy,
                onClick = onCsv,
                modifier = Modifier.weight(1f)
            )
            ExportPill(
                text = stringResource(R.string.export_print),
                icon = Icons.Default.Print,
                enabled = !busy,
                onClick = onPrint,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ExportPill(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (enabled) AppColors.Surface2 else AppColors.Surface3
    val contentColor = if (enabled) AppColors.Accent else AppColors.TextFaint
    Row(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(Radius.Md))
            .background(bg)
            .border(1.dp, AppColors.BorderSoft, RoundedCornerShape(Radius.Md))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = text,
            fontFamily = Type.Body,
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
            color = contentColor
        )
    }
}

// ---------------------------------------------------------------------------
// Закреплённая нижняя панель действий (аналог .sticky-actions)
// ---------------------------------------------------------------------------

@Composable
private fun StickyActionsBar(
    pdfReady: Boolean,
    busy: Boolean,
    onShare: () -> Unit,
    onDownload: () -> Unit,
    onAnalyze: () -> Unit,
    onCoverLetter: () -> Unit = {},
    onTranslate: () -> Unit = {},
) {
    val enabled = pdfReady && !busy
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.BackgroundSoft)
            .navigationBarsPadding()
            .border(width = 1.dp, color = AppColors.BorderSoft)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlineIconButton(icon = Icons.Outlined.Share, enabled = enabled, onClick = onShare)
        OutlineIconButton(icon = Icons.Outlined.Download, enabled = enabled, onClick = onDownload)
        OutlineIconButton(
            icon = Icons.Outlined.MailOutline,
            enabled = enabled,
            onClick = onCoverLetter
        )
        OutlineIconButton(
            icon = Icons.Outlined.Translate,
            enabled = enabled,
            onClick = onTranslate
        )
        FillPillButton(
            text = stringResource(R.string.preview_ai_analysis),
            icon = Icons.Outlined.AutoAwesome,
            enabled = !busy,
            onClick = onAnalyze,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun OutlineIconButton(
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = if (enabled) AppColors.TextPrimary else AppColors.TextFaint
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(Radius.Md))
            .border(1.5.dp, AppColors.Border, RoundedCornerShape(Radius.Md))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun FillPillButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (enabled) AppColors.Accent else AppColors.Surface3
    val contentColor = if (enabled) AppColors.AccentInk else AppColors.TextFaint
    Row(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(Radius.Md))
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = text,
            fontFamily = Type.Body,
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp,
            color = contentColor
        )
    }
}

// ---------------------------------------------------------------------------

@Composable
private fun templateDisplayName(template: PdfTemplateType): String = when (template) {
    PdfTemplateType.MODERN -> stringResource(R.string.template_modern)
    PdfTemplateType.CREATIVE -> stringResource(R.string.template_creative)
    PdfTemplateType.MINIMALIST -> stringResource(R.string.template_minimalist)
    PdfTemplateType.PROFESSIONAL -> stringResource(R.string.template_professional)
    PdfTemplateType.SIDEBAR -> stringResource(R.string.template_sidebar)
}

@Composable
private fun sectionDisplayName(section: PdfSection): String = when (section) {
    PdfSection.SUMMARY -> stringResource(R.string.section_summary)
    PdfSection.ABOUT -> stringResource(R.string.section_about)
    PdfSection.EDUCATION -> stringResource(R.string.section_education)
    PdfSection.LANGUAGES -> stringResource(R.string.section_languages)
    PdfSection.SKILLS -> stringResource(R.string.section_skills)
    PdfSection.ACHIEVEMENTS -> stringResource(R.string.section_achievements)
    PdfSection.PROJECTS -> stringResource(R.string.section_projects)
}
