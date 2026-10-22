package com.example.data.remote

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class CloudSyncRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun upsertAndGetDocument_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = CloudSyncRepository(firestore)
        val docId = "doc_${UUID.randomUUID().toString().replace("-", "").take(10)}"

        val createdId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertDocument(
                docId = docId,
                title = "Invoice 2026",
                category = "Receipt",
                pageCount = 2,
                fileSizeBytes = 102400L,
                ocrText = "Total: $149.00",
                aiSummary = "Software subscription invoice.",
                tags = listOf("invoice", "tax"),
                isFavorite = true,
                pageSize = "A4"
            ).getOrThrow()
        }
        assertEquals(docId, createdId)

        val docs = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getUserDocuments().getOrThrow()
        }
        assertTrue(docs.any { it.id == docId && it.userId == aliceUid })
    }

    @Test
    fun observeDocuments_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = CloudSyncRepository(firestore)
        val docId = "doc_${UUID.randomUUID().toString().replace("-", "").take(10)}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertDocument(
                docId = docId,
                title = "Realtime Scan",
                ocrText = "Hello OCR"
            ).getOrThrow()
        }

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeDocuments(aliceUid).first { list -> list.any { it.id == docId } }
        }
        assertTrue(emitted.any { it.id == docId })
    }

    @Test
    fun getDocumentById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = CloudSyncRepository(firestore)
        val docId = "doc_${UUID.randomUUID().toString().replace("-", "").take(10)}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.upsertDocument(
                docId = docId,
                title = "Alice Private Scan"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = CloudSyncRepository(firestore)
        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getDocumentById(targetUserId = aliceUid, docId = docId)
        }
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue(ex is FirebaseFirestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, (ex as FirebaseFirestoreException).code)
    }

    @Test
    fun observeDocuments_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = CloudSyncRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeDocuments(aliceUid).first()
            }
            fail("Expected FirebaseFirestoreException.Code.PERMISSION_DENIED")
        } catch (t: Throwable) {
            val firestoreEx = generateSequence(t) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull() ?: throw t
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@docuscan.test"
        const val BOB_EMAIL = "bob@docuscan.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
