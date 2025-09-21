package com.nirwanrn.thoughtcaddy.journal

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JournalRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val aiSummaryService: com.nirwanrn.thoughtcaddy.ai.AiSummaryService
) {

    private fun getUserId(): String? = auth.currentUser?.uid

    private fun getUserEntriesCollection() = getUserId()?.let { userId ->
        firestore.collection("users")
            .document(userId)
            .collection("journalEntries")
    }

    suspend fun addJournalEntry(text: String): Result<String> {
        return try {
            val userId = getUserId() ?: return Result.failure(Exception("User not authenticated"))

            val entry = JournalEntry(
                text = text,
                createdAt = Timestamp(Date()),
                updatedAt = Timestamp(Date()),
                userId = userId
            )

            val docRef = getUserEntriesCollection()?.add(entry.toMap())?.await()
                ?: return Result.failure(Exception("Failed to create document"))

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addJournalEntryWithSummary(text: String): Result<String> {
        return try {
            val userId = getUserId() ?: return Result.failure(Exception("User not authenticated"))

            val entry = JournalEntry(
                id = "",
                text = text,
                createdAt = Timestamp(Date()),
                updatedAt = Timestamp(Date()),
                userId = userId,
                summary = null
            )

            val dataToSend = entry.toMap()
            Log.d("JournalRepository", "Creating entry with data: $dataToSend")
            Log.d("JournalRepository", "User ID: $userId")
            Log.d("JournalRepository", "Text length: ${text.length}")

            val docRef = getUserEntriesCollection()?.add(dataToSend)?.await()
                ?: return Result.failure(Exception("Failed to create document"))

            try {
                val summaryResult = aiSummaryService.summarizeEntry(text)
                summaryResult.fold(
                    onSuccess = { summary ->
                        val updates = mapOf("summary" to summary)
                        getUserEntriesCollection()?.document(docRef.id)?.update(updates)
                    },
                    onFailure = {
                    }
                )
            } catch (e: Exception) {
            }

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateJournalEntry(entryId: String, text: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "text" to text,
                "updatedAt" to Timestamp(Date())
            )

            getUserEntriesCollection()?.document(entryId)?.update(updates)?.await()
                ?: return Result.failure(Exception("Failed to update document"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteJournalEntry(entryId: String): Result<Unit> {
        return try {
            getUserEntriesCollection()?.document(entryId)?.delete()?.await()
                ?: return Result.failure(Exception("Failed to delete document"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeJournalEntries(): Flow<List<JournalEntry>> = callbackFlow {
        val collection = getUserEntriesCollection()
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = collection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val entries = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(JournalEntry::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(entries)
            }

        awaitClose { listener.remove() }
    }

    suspend fun getJournalEntry(entryId: String): Result<JournalEntry> {
        return try {
            val doc = getUserEntriesCollection()?.document(entryId)?.get()?.await()
                ?: return Result.failure(Exception("Document not found"))

            val entry = doc.toObject(JournalEntry::class.java)?.copy(id = doc.id)
                ?: return Result.failure(Exception("Failed to parse document"))

            Result.success(entry)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateSummaryForEntry(entryId: String): Result<String> {
        return try {
            val entryResult = getJournalEntry(entryId)
            val entry = entryResult.getOrElse {
                return Result.failure(Exception("Entry not found"))
            }

            val summaryResult = aiSummaryService.summarizeEntry(entry.text)
            val summary = summaryResult.getOrElse { error ->
                return Result.failure(error)
            }

            val updates = mapOf(
                "summary" to summary,
                "updatedAt" to Timestamp(Date())
            )

            getUserEntriesCollection()?.document(entryId)?.update(updates)?.await()
                ?: return Result.failure(Exception("Failed to update entry with summary"))

            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}