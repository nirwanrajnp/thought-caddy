package com.nirwanrn.thoughtcaddy.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val journalRepository: JournalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<JournalUiState>(JournalUiState.Loading)
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    init {
        observeJournalEntries()
    }

    fun onEvent(event: JournalEvent) {
        when (event) {
            is JournalEvent.NewEntryTextChanged -> {
                updateNewEntryText(event.text)
            }
            is JournalEvent.AddEntry -> {
                addJournalEntry()
            }
            is JournalEvent.DeleteEntry -> {
                deleteJournalEntry(event.entryId)
            }
            is JournalEvent.EditEntry -> {
                editJournalEntry(event.entryId, event.newText)
            }
            is JournalEvent.SummarizeEntry -> {
                summarizeEntry(event.entryId)
            }
            is JournalEvent.ClearError -> {
                clearError()
            }
        }
    }

    private fun observeJournalEntries() {
        viewModelScope.launch {
            journalRepository.observeJournalEntries()
                .catch { error ->
                    _uiState.value = JournalUiState.Error(
                        error.message ?: "Failed to load journal entries"
                    )
                }
                .collect { entries ->
                    val currentState = _uiState.value

                    val isStillGenerating = if (currentState is JournalUiState.Success && currentState.isGeneratingAiSummary) {
                        entries.firstOrNull()?.summary == null
                    } else {
                        if (currentState is JournalUiState.Success) currentState.isGeneratingAiSummary else false
                    }

                    _uiState.value = JournalUiState.Success(
                        entries = entries,
                        isAddingEntry = if (currentState is JournalUiState.Success) currentState.isAddingEntry else false,
                        newEntryText = if (currentState is JournalUiState.Success) currentState.newEntryText else "",
                        summarizingEntryIds = if (currentState is JournalUiState.Success) currentState.summarizingEntryIds else emptySet(),
                        isGeneratingAiSummary = isStillGenerating,
                        deletingEntryIds = if (currentState is JournalUiState.Success) currentState.deletingEntryIds else emptySet()
                    )
                }
        }
    }

    private fun updateNewEntryText(text: String) {
        val currentState = _uiState.value
        if (currentState is JournalUiState.Success) {
            _uiState.value = currentState.copy(newEntryText = text)
        }
    }

    private fun addJournalEntry() {
        val currentState = _uiState.value
        if (currentState !is JournalUiState.Success) return

        val text = currentState.newEntryText.trim()
        if (text.isEmpty()) {
            _uiState.value = JournalUiState.Error("Please enter some text for your journal entry")
            return
        }

        _uiState.value = currentState.copy(isAddingEntry = true, isGeneratingAiSummary = true)

        viewModelScope.launch {
            journalRepository.addJournalEntryWithSummary(text).fold(
                onSuccess = {
                    val updatedState = _uiState.value
                    if (updatedState is JournalUiState.Success) {
                        _uiState.value = updatedState.copy(
                            isAddingEntry = false,
                            newEntryText = "",
                            isGeneratingAiSummary = true
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = JournalUiState.Error(
                        error.message ?: "Failed to add journal entry"
                    )
                }
            )
        }
    }

    private fun deleteJournalEntry(entryId: String) {
        val currentState = _uiState.value
        if (currentState !is JournalUiState.Success) return

        // Add this entry to the deleting set
        _uiState.value = currentState.copy(
            deletingEntryIds = currentState.deletingEntryIds + entryId
        )

        viewModelScope.launch {
            journalRepository.deleteJournalEntry(entryId).fold(
                onSuccess = {
                    val updatedState = _uiState.value
                    if (updatedState is JournalUiState.Success) {
                        _uiState.value = updatedState.copy(
                            deletingEntryIds = updatedState.deletingEntryIds - entryId
                        )
                    }
                },
                onFailure = { error ->
                    val updatedState = _uiState.value
                    if (updatedState is JournalUiState.Success) {
                        _uiState.value = updatedState.copy(
                            deletingEntryIds = updatedState.deletingEntryIds - entryId
                        )
                    }
                }
            )
        }
    }

    private fun editJournalEntry(entryId: String, newText: String) {
        if (newText.trim().isEmpty()) {
            _uiState.value = JournalUiState.Error("Entry text cannot be empty")
            return
        }

        viewModelScope.launch {
            journalRepository.updateJournalEntry(entryId, newText.trim()).fold(
                onSuccess = {
                    // Success is handled by the real-time listener
                },
                onFailure = { error ->
                    _uiState.value = JournalUiState.Error(
                        error.message ?: "Failed to update journal entry"
                    )
                }
            )
        }
    }

    private fun summarizeEntry(entryId: String) {
        val currentState = _uiState.value
        if (currentState !is JournalUiState.Success) return

        _uiState.value = currentState.copy(
            summarizingEntryIds = currentState.summarizingEntryIds + entryId
        )

        viewModelScope.launch {
            try {
                journalRepository.generateSummaryForEntry(entryId).fold(
                    onSuccess = { summary ->
                        val updatedState = _uiState.value
                        if (updatedState is JournalUiState.Success) {
                            _uiState.value = updatedState.copy(
                                summarizingEntryIds = updatedState.summarizingEntryIds - entryId
                            )
                        }
                    },
                    onFailure = { error ->
                        val updatedState = _uiState.value
                        if (updatedState is JournalUiState.Success) {
                            _uiState.value = updatedState.copy(
                                summarizingEntryIds = updatedState.summarizingEntryIds - entryId
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                val updatedState = _uiState.value
                if (updatedState is JournalUiState.Success) {
                    _uiState.value = updatedState.copy(
                        summarizingEntryIds = updatedState.summarizingEntryIds - entryId
                    )
                }
            }
        }
    }

    private fun clearError() {
        observeJournalEntries()
    }
}