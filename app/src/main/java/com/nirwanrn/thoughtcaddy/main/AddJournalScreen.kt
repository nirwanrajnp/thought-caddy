package com.nirwanrn.thoughtcaddy.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.alpha
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.util.Log
import com.nirwanrn.thoughtcaddy.journal.JournalEvent
import com.nirwanrn.thoughtcaddy.journal.JournalViewModel
import com.nirwanrn.thoughtcaddy.ui.theme.AISummaryContainer
import com.nirwanrn.thoughtcaddy.ui.theme.ThoughtCaddyBlue
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddJournalScreen(
    onNavigateBack: () -> Unit,
    onJournalSaved: () -> Unit,
    viewModel: JournalViewModel = hiltViewModel()
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var isSaving by remember { mutableStateOf(false) }

    val currentUiState = uiState
    val isLoading = currentUiState is com.nirwanrn.thoughtcaddy.journal.JournalUiState.Success &&
            (currentUiState.isAddingEntry || currentUiState.isGeneratingAiSummary)

    LaunchedEffect(uiState) {
        val currentState = uiState
        Log.d("AddJournalScreen", "UI State changed: ${currentState::class.simpleName}")

        if (currentState is com.nirwanrn.thoughtcaddy.journal.JournalUiState.Error) {
            Log.e("AddJournalScreen", "Error state: ${currentState.message}")
            snackbarHostState.showSnackbar(
                message = "Failed to save: ${currentState.message}",
                duration = SnackbarDuration.Long
            )
            isSaving = false
        } else if (currentState is com.nirwanrn.thoughtcaddy.journal.JournalUiState.Success) {
            Log.d("AddJournalScreen", "Success state - isSaving: $isSaving, isAddingEntry: ${currentState.isAddingEntry}, newEntryText: '${currentState.newEntryText}', entries count: ${currentState.entries.size}")

            if (isSaving) {
                if (!currentState.isAddingEntry && currentState.newEntryText.isEmpty()) {
                    Log.d("AddJournalScreen", "Save appears complete, navigating back...")
                    kotlinx.coroutines.delay(500) // Small delay to ensure save completed
                    onJournalSaved()
                    isSaving = false
                }
            }
        } else {
            Log.d("AddJournalScreen", "Other state: $currentState")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "New Journal Entry",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (!isLoading) ThoughtCaddyBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            Log.d("AddJournalScreen", "Save button clicked")
                            if (title.isNotEmpty() && content.isNotEmpty()) {
                                val entryText = if (title.isNotEmpty()) {
                                    "$title\n\n$content"
                                } else {
                                    content
                                }
                                Log.d("AddJournalScreen", "Entry text created: '$entryText'")
                                isSaving = true
                                Log.d("AddJournalScreen", "Setting newEntryText...")
                                viewModel.onEvent(JournalEvent.NewEntryTextChanged(entryText))
                                Log.d("AddJournalScreen", "Triggering AddEntry...")
                                viewModel.onEvent(JournalEvent.AddEntry)
                                Log.d("AddJournalScreen", "Save events triggered")
                            } else {
                                Log.d("AddJournalScreen", "Title or content empty - title: '$title', content: '$content'")
                            }
                        },
                        enabled = title.isNotEmpty() && content.isNotEmpty() && !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save",
                            tint = if (title.isNotEmpty() && content.isNotEmpty() && !isLoading)
                                ThoughtCaddyBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                windowInsets = WindowInsets(top = 0.5.dp)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            AISummaryContainer.copy(alpha = 0.1f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Date Display
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = ThoughtCaddyBlue.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = ThoughtCaddyBlue,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }

                // Title Input
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Title",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = ThoughtCaddyBlue,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("Give your thoughts a title...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ThoughtCaddyBlue,
                                focusedLabelColor = ThoughtCaddyBlue,
                                cursorColor = ThoughtCaddyBlue
                            )
                        )
                    }
                }

                // Content Input
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Your Thoughts",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = ThoughtCaddyBlue,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = content,
                            onValueChange = { content = it },
                            placeholder = { Text("What's on your mind today?") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ThoughtCaddyBlue,
                                focusedLabelColor = ThoughtCaddyBlue,
                                cursorColor = ThoughtCaddyBlue
                            )
                        )
                    }
                }

                // Save Button
                Button(
                    onClick = {
                        if (title.isNotEmpty() && content.isNotEmpty()) {
                            val entryText = if (title.isNotEmpty()) {
                                "$title\n\n$content"
                            } else {
                                content
                            }
                            isSaving = true
                            viewModel.onEvent(JournalEvent.NewEntryTextChanged(entryText))
                            viewModel.onEvent(JournalEvent.AddEntry)
                        }
                    },
                    enabled = title.isNotEmpty() && content.isNotEmpty() && !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThoughtCaddyBlue,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Save Journal Entry",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Loading overlay for saving and AI summary generation
            if (currentUiState is com.nirwanrn.thoughtcaddy.journal.JournalUiState.Success &&
                (currentUiState.isAddingEntry || currentUiState.isGeneratingAiSummary)) {
                LoadingOverlay(
                    isGeneratingAiSummary = currentUiState.isGeneratingAiSummary,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun LoadingOverlay(
    isGeneratingAiSummary: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loadingAnimation")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaAnimation"
    )

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = ThoughtCaddyBlue,
                    strokeWidth = 4.dp
                )

                Text(
                    text = if (isGeneratingAiSummary) "Generating AI Summary..." else "Saving Journal Entry...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isGeneratingAiSummary)
                        "Creating an AI-powered summary of your thoughts. This may take a moment."
                    else
                        "Saving your journal entry to the cloud.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(alpha)
                )
            }
        }
    }
}