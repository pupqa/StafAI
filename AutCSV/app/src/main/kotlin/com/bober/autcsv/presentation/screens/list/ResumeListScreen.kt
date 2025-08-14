package com.bober.autcsv.presentation.screens.list

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bober.autcsv.R
import com.bober.autcsv.core.utils.LlmLogger
import com.bober.autcsv.ui.theme.CYellow
import com.bober.autcsv.ui.theme.White
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeListScreen(
    onNavigateToForm: () -> Unit,
    onNavigateToPreview: (String) -> Unit,
    onNavigateToDashboard: () -> Unit,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_list)) },
                actions = {
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
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.resumes) { resume ->
                            SwipeableResumeCard(
                                resume = resume,
                                onCardClick = { 
                                    LlmLogger.logUserAction("Resume card clicked", "List", resume.id)
                                    onNavigateToPreview(resume.id) 
                                },
                                onDeleteClick = {
                                    LlmLogger.logUserAction("Delete resume swipe", "List", resume.id)
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
                        Text(text = stringResource(R.string.delete_confirm), color = MaterialTheme.colorScheme.error)
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

@Composable
fun SwipeableResumeCard(
    resume: com.bober.autcsv.domain.model.Resume,
    onCardClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val alpha by animateFloatAsState(
        targetValue = if (offsetX != 0f) 1f else 0f,
        label = "delete_alpha"
    )

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Фон с иконкой удаления
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(
                    color = MaterialTheme.colorScheme.error,
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
                    text = "${stringResource(R.string.total_experience)}: ${resume.personalInfo.totalExperience}",
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