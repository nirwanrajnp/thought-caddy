package com.nirwanrn.thoughtcaddy.ai

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

class AiSummaryExample {

    private val functions = FirebaseFunctions.getInstance()

    suspend fun generateSummaryOnly(text: String): Result<String> {
        return try {
            val data = hashMapOf(
                "text" to text
            )

            val result = functions
                .getHttpsCallable("summarizeEntry")
                .call(data)
                .await()

            val response = result.data as? Map<*, *>
            val summary = response?.get("summary") as? String
                ?: return Result.failure(Exception("Invalid response format"))

            Result.success(summary)

        } catch (e: Exception) {
            when {
                e.message?.contains("unauthenticated") == true -> {
                    Result.failure(Exception("Please log in to use AI summarization"))
                }
                e.message?.contains("invalid-argument") == true -> {
                    Result.failure(Exception("Journal entry text is required"))
                }
                e.message?.contains("resource-exhausted") == true -> {
                    Result.failure(Exception("AI service is busy, please try again"))
                }
                else -> {
                    Result.failure(Exception("Failed to generate summary: ${e.message}"))
                }
            }
        }
    }

    suspend fun exampleUsage() {
        val journalText = "Today was a challenging day at work. I had to present to the board and felt nervous, but it went well. I learned that preparation really pays off and I'm feeling more confident about future presentations."

        generateSummaryOnly(journalText).fold(
            onSuccess = { summary ->
                println("AI Summary: $summary")
                // Expected output: "You had a challenging but successful day presenting to the board at work. Despite initial nervousness, your preparation helped you succeed and build confidence. This experience taught you valuable lessons about the importance of preparation for future presentations."
            },
            onFailure = { error ->
                println("Error: ${error.message}")
                // Handle error appropriately in your UI
            }
        )
    }

    suspend fun generateMultipleSummaries(entries: List<String>): List<Pair<String, String?>> {
        return entries.map { text ->
            val summary = generateSummaryOnly(text).getOrNull()
            text to summary
        }
    }
}