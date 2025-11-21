package com.bober.autcsv.presentation.screens.list

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bober.autcsv.R
import com.bober.autcsv.core.utils.LlmLogger
import com.bober.autcsv.core.utils.PdfImportParser
import com.bober.autcsv.domain.model.Resume
import com.bober.autcsv.ui.theme.CBlackLight
import com.bober.autcsv.ui.theme.CYellow
import com.bober.autcsv.ui.theme.ErrorDark
import com.bober.autcsv.ui.theme.Hint
import com.bober.autcsv.ui.theme.White
import kotlinx.coroutines.delay

/**
 * Экран списка резюме: загрузка, пустое состояние, список карточек,
 * импорт PDF, переходы к формам/предпросмотру/дашборду.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeListScreen(
    onNavigateToForm: () -> Unit,
    onNavigateToPreview: (String) -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToFormEdit: (String) -> Unit,
    viewModel: ResumeListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var resumeToDelete by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Показываем секундный прогресс перед отображением контента
    var initialLoading by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(1000)
        initialLoading = false
    }
    val isUiLoading by remember(state.isLoading, initialLoading) {
        derivedStateOf { state.isLoading || initialLoading }
    }

    LlmLogger.logUiEvent("List", "Screen rendered", "Resumes count: ${state.resumes.size}")

    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    val resolver = context.contentResolver
                    resolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                }

                // Создаем новый Resume, распарсив PDF, и сохраняем
                try {
                    val resume = PdfImportParser.parseNewResumeFromPdf(context, uri)
                    LlmLogger.logUiEvent("Import", "Parsed resume from PDF", resume.id)
                    // Сохраняем импортированное резюме и переходим в форму
                    viewModel.saveImportedResume(resume) { newId ->
                        onNavigateToFormEdit(newId)
                    }
                } catch (e: Exception) {
                    LlmLogger.logError("Failed to import PDF: ${e.message}", e)
                }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_list)) },
                actions = {
                    // Import PDF action
                    IconButton(
                        onClick = {
                            LlmLogger.logUserAction("Import PDF button pressed", "List", null)
                            importLauncher.launch(arrayOf("application/pdf"))
                        }
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Import PDF"
                        )
                    }
                    IconButton(
                        onClick = {
                            LlmLogger.logUserAction("Dashboard button pressed", "List", null)
                            onNavigateToDashboard()
                        }
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = stringResource(R.string.nav_dashboard)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    LlmLogger.logUserAction("Add resume button pressed", "List", null)
                    onNavigateToForm()
                },
                containerColor = CYellow
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.add),
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isUiLoading) {
                LlmLogger.logUiEvent("List", "Loading state", "UI loading spinner")
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                if (state.resumes.isEmpty()) {
                    LlmLogger.logUiEvent("List", "Empty state", "No resumes found")
                    Text(
                        text = stringResource(R.string.empty_resume_list),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.resumes) { resume ->
                            SwipeableResumeCard(
                                resume = resume,
                                onCardClick = {
                                    LlmLogger.logUserAction(
                                        "Resume card clicked",
                                        "List",
                                        resume.id
                                    )
                                    onNavigateToPreview(resume.id)
                                },
                                onDeleteClick = {
                                    LlmLogger.logUserAction(
                                        "Delete resume swipe",
                                        "List",
                                        resume.id
                                    )
                                    resumeToDelete = resume.id
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }

        // Диалог подтверждения удаления
        if (showDeleteDialog && resumeToDelete != null) {
            LlmLogger.logUiEvent("List", "Delete dialog shown", "Resume ID: $resumeToDelete")
            AlertDialog(
                onDismissRequest = {
                    LlmLogger.logUserAction("Delete dialog dismissed", "List", resumeToDelete)
                    showDeleteDialog = false
                    resumeToDelete = null
                },
                title = {
                    Text(text = stringResource(R.string.delete_resume_title))
                },
                text = {
                    Text(text = stringResource(R.string.delete_resume_message))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            LlmLogger.logUserAction("Delete confirmed", "List", resumeToDelete)
                            resumeToDelete?.let { viewModel.deleteResume(it) }
                            showDeleteDialog = false
                            resumeToDelete = null
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.delete_confirm),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            LlmLogger.logUserAction("Delete cancelled", "List", resumeToDelete)
                            showDeleteDialog = false
                            resumeToDelete = null
                        }
                    ) {
                        Text(text = stringResource(R.string.cancel_action), color = White)
                    }
                }
            )
        }

        // Snackbar для ошибок
        state.error?.let { error ->
            LlmLogger.logError("List screen error: $error")
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(
                        onClick = {
                            LlmLogger.logUserAction("Clear error", "List", null)
                            viewModel.clearError()
                        }
                    ) {
                        Text("OK")
                    }
                }
            ) {
                Text(error)
            }
        }
    }
}

// Импорт по PDF теперь создает новый Resume, парсинг id по имени файла больше не используется

/**
 * Карточка резюме с горизонтальным свайпом для удаления.
 */

@Composable
fun SwipeableResumeCard(
    resume: Resume,
    onCardClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var cardHeightPx by remember { mutableIntStateOf(0) }

    val density = LocalDensity.current
    val backgroundExtra = 8.dp
    val backgroundHeight = remember(cardHeightPx) {
        if (cardHeightPx > 0) with(density) { cardHeightPx.toDp() } + backgroundExtra - 6.dp else 110.dp
    }

    // Максимальное смещение для свайпа (в пикселях)
    val maxSwipeOffset = -200f

    // Прогресс свайпа: 0.0 (не свайпнуто) → 1.0 (макс. свайп)
    val swipeProgress = remember(offsetX) {
        (-offsetX / -maxSwipeOffset).coerceIn(0f, 1f)
    }

    // Анимированный цвет фона: от error → SuccessColor
    val backgroundColor by animateColorAsState(
        targetValue = lerp(
            start = Hint,
            stop = MaterialTheme.colorScheme.error,
            fraction = swipeProgress
        ),
        animationSpec = tween(durationMillis = 100), // короткая анимация для плавности
        label = "swipe_background_color"
    )

    val alpha by animateFloatAsState(
        targetValue = swipeProgress,
        animationSpec = tween(100),
        label = "delete_icon_alpha"
    )

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Фон с иконкой удаления
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(backgroundHeight)
                .background(
                    color = backgroundColor,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.delete),
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .alpha(alpha)
            )
        }

        // Карточка резюме
        ElevatedCard(
            onClick = onCardClick,
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    cardHeightPx = coordinates.size.height
                }
                .offset(x = androidx.compose.ui.unit.Dp(offsetX))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            if (offsetX < -100f) {
                                LlmLogger.logUserAction("Swipe to delete", "List", resume.id)
                                onDeleteClick()
                            }
                            offsetX = 0f
                        }
                    ) { _, dragAmount ->
                        offsetX += dragAmount.x
                        if (offsetX > 0f) offsetX = 0f
                        if (offsetX < -200f) offsetX = -200f
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CBlackLight)
                    .padding(16.dp)
            ) {
                Text(
                    text = resume.personalInfo.fullName,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = resume.personalInfo.specialization,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "${stringResource(R.string.specialization_experience)}: ${resume.personalInfo.specializationExperience}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = resume.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )
            }
        }
    }
} 