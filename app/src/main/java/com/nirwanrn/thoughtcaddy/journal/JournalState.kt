package com.nirwanrn.thoughtcaddy.journal

sealed class JournalUiState {
    object Loading : JournalUiState()
    data class Success(
        val entries: List<JournalEntry> = emptyList(),
        val isAddingEntry: Boolean = false,
        val newEntryText: String = "",
        val summarizingEntryIds: Set<String> = emptySet(),
        val isGeneratingAiSummary: Boolean = false,
        val deletingEntryIds: Set<String> = emptySet()
    ) : JournalUiState()
    data class Error(val message: String) : JournalUiState()
}

sealed class JournalEvent {
    data class NewEntryTextChanged(val text: String) : JournalEvent()
    object AddEntry : JournalEvent()
    data class DeleteEntry(val entryId: String) : JournalEvent()
    data class EditEntry(val entryId: String, val newText: String) : JournalEvent()
    data class SummarizeEntry(val entryId: String) : JournalEvent()
    object ClearError : JournalEvent()
}