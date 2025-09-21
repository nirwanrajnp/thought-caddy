package com.nirwanrn.thoughtcaddy.journal

import com.google.firebase.Timestamp
import java.util.Date

data class JournalEntry(
    val id: String = "",
    val text: String = "",
    val summary: String? = null,
    val createdAt: Timestamp = Timestamp(Date()),
    val updatedAt: Timestamp = Timestamp(Date()),
    val userId: String = ""
) {
    // No-argument constructor for Firestore
    constructor() : this("", "", null, Timestamp(Date()), Timestamp(Date()), "")

    // Helper function to convert to Map for Firestore
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "text" to text,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "userId" to userId
        )
        summary?.let { map["summary"] = it }
        return map
    }
}