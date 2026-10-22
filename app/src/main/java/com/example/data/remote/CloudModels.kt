package com.example.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class CloudDocument(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val folderId: String = "",
    val category: String = "Scan",
    val pageCount: Long = 1L,
    val fileSizeBytes: Long = 0L,
    val ocrText: String = "",
    val aiSummary: String = "",
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val isPasswordProtected: Boolean = false,
    val pageSize: String = "A4",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudFolder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val colorHex: String = "#2563EB",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
