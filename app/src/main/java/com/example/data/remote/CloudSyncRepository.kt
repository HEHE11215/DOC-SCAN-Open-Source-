package com.example.data.remote

import android.content.Context
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CloudSyncRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance(db.app)

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeDocuments(userId: String = auth.currentUser?.uid ?: "unauthenticated"): Flow<List<CloudDocument>> = flow {
        val path = "users/$userId/documents"
        emitAll(
            db.collection("users").document(userId).collection("documents")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(CloudDocument::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeFolders(userId: String = auth.currentUser?.uid ?: "unauthenticated"): Flow<List<CloudFolder>> = flow {
        val path = "users/$userId/folders"
        emitAll(
            db.collection("users").document(userId).collection("folders")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(CloudFolder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun getUserDocuments(): Result<List<CloudDocument>> = runCatching {
        val uid = requireUserId()
        val path = "users/$uid/documents"
        try {
            val snapshot = db.collection("users").document(uid).collection("documents")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(CloudDocument::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
    }

    suspend fun getDocumentById(targetUserId: String, docId: String): Result<CloudDocument?> = runCatching {
        val path = "users/$targetUserId/documents/$docId"
        try {
            val doc = db.collection("users").document(targetUserId)
                .collection("documents").document(docId)
                .get()
                .await()
            doc.toObject(CloudDocument::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            throw e
        }
    }

    suspend fun upsertDocument(
        docId: String = UUID.randomUUID().toString(),
        title: String,
        folderId: String = "",
        category: String = "Scan",
        pageCount: Int = 1,
        fileSizeBytes: Long = 0L,
        ocrText: String = "",
        aiSummary: String = "",
        tags: List<String> = emptyList(),
        isFavorite: Boolean = false,
        isPasswordProtected: Boolean = false,
        pageSize: String = "A4",
        isUpdate: Boolean = false
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val cleanDocId = docId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120).ifBlank { UUID.randomUUID().toString() }
        val docRef = db.collection("users").document(uid).collection("documents").document(cleanDocId)
        val safePageSize = if (pageSize in listOf("A4", "LETTER", "ORIGINAL")) pageSize else "A4"
        try {
            if (isUpdate) {
                val updatePayload = mapOf(
                    "title" to title.take(200).ifBlank { "Untitled Scan" },
                    "folderId" to folderId.take(128),
                    "category" to category.take(64).ifBlank { "Scan" },
                    "pageCount" to pageCount.coerceIn(1, 2000).toLong(),
                    "fileSizeBytes" to fileSizeBytes.coerceAtLeast(0L),
                    "ocrText" to ocrText.take(95000),
                    "aiSummary" to aiSummary.take(19000),
                    "tags" to tags.map { it.take(50) }.take(10),
                    "isFavorite" to isFavorite,
                    "isPasswordProtected" to isPasswordProtected,
                    "pageSize" to safePageSize,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            } else {
                val createPayload = mapOf(
                    "userId" to uid,
                    "title" to title.take(200).ifBlank { "Untitled Scan" },
                    "folderId" to folderId.take(128),
                    "category" to category.take(64).ifBlank { "Scan" },
                    "pageCount" to pageCount.coerceIn(1, 2000).toLong(),
                    "fileSizeBytes" to fileSizeBytes.coerceAtLeast(0L),
                    "ocrText" to ocrText.take(95000),
                    "aiSummary" to aiSummary.take(19000),
                    "tags" to tags.map { it.take(50) }.take(10),
                    "isFavorite" to isFavorite,
                    "isPasswordProtected" to isPasswordProtected,
                    "pageSize" to safePageSize,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }
            cleanDocId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun deleteDocument(docId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("documents").document(docId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun upsertFolder(
        folderId: String = UUID.randomUUID().toString(),
        name: String,
        colorHex: String = "#2563EB",
        isUpdate: Boolean = false
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val cleanId = folderId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120).ifBlank { UUID.randomUUID().toString() }
        val folderRef = db.collection("users").document(uid).collection("folders").document(cleanId)
        try {
            if (isUpdate) {
                val updatePayload = mapOf(
                    "name" to name.take(100).ifBlank { "Folder" },
                    "colorHex" to colorHex.take(16).ifBlank { "#2563EB" },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                folderRef.update(updatePayload).await()
            } else {
                val createPayload = mapOf(
                    "userId" to uid,
                    "name" to name.take(100).ifBlank { "Folder" },
                    "colorHex" to colorHex.take(16).ifBlank { "#2563EB" },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                folderRef.set(createPayload).await()
            }
            cleanId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, folderRef.path)
            throw e
        }
    }

    suspend fun deleteFolder(folderId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val folderRef = db.collection("users").document(uid).collection("folders").document(folderId)
        try {
            folderRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, folderRef.path)
            throw e
        }
    }
}
