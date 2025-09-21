package com.nirwanrn.thoughtcaddy.ai

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiSummaryService @Inject constructor(
    private val functions: FirebaseFunctions
) {

    suspend fun summarizeEntry(text: String): Result<String> {
        return try {
            val data = hashMapOf(
                "text" to text
            )

            val result = functions
                .getHttpsCallable("summarizeEntry")
                .call(data)
                .await()

            val summary = (result.data as? Map<*, *>)?.get("summary") as? String
                ?: return Result.failure(Exception("Invalid response format"))

            Result.success(summary)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}