package com.example.di

import android.content.Context
import androidx.room.Room
import com.example.R
import com.example.data.ai.GeminiDocumentAssistant
import com.example.data.local.DocuScanDatabase
import com.example.data.preferences.SettingsRepository
import com.example.data.remote.CloudSyncRepository
import com.example.data.repository.DocumentRepository
import com.google.firebase.firestore.FirebaseFirestore

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: DocuScanDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            DocuScanDatabase::class.java,
            "docuscan_local.db"
        ).fallbackToDestructiveMigration(true).build()
    }

    val firestore: FirebaseFirestore? by lazy {
        try {
            val dbId = appContext.getString(R.string.firestore_database_id)
            FirebaseFirestore.getInstance(dbId)
        } catch (e: Throwable) {
            android.util.Log.w("AppContainer", "Firestore not available: ${e.message}")
            null
        }
    }

    val cloudSyncRepository: CloudSyncRepository by lazy {
        CloudSyncRepository(firestore)
    }

    val documentRepository: DocumentRepository by lazy {
        DocumentRepository(
            context = appContext,
            documentDao = database.documentDao(),
            cloudSyncRepository = cloudSyncRepository
        )
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(appContext)
    }

    val geminiAssistant: GeminiDocumentAssistant by lazy {
        GeminiDocumentAssistant()
    }
}
