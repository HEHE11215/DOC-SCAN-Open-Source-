package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorHex: String = "#2563EB",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "documents",
    indices = [Index("folderId"), Index("createdAt"), Index("isFavorite")]
)
data class ScannedDocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String = "Scans",
    val folderId: String = "",
    val pdfFilePath: String = "",
    val thumbnailPath: String = "",
    val pageCount: Int = 1,
    val fileSizeBytes: Long = 0L,
    val ocrText: String = "",
    val aiSummary: String = "",
    val tagsCsv: String = "",
    val pageSize: String = "A4",
    val quality: String = "HIGH",
    val isSearchablePdf: Boolean = true,
    val isFavorite: Boolean = false,
    val isPasswordProtected: Boolean = false,
    val passwordHash: String = "",
    val watermarkText: String = "",
    val isCloudSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "pages",
    foreignKeys = [
        ForeignKey(
            entity = ScannedDocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("documentId"), Index("pageIndex")]
)
data class ScannedPageEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val pageIndex: Int,
    val originalImagePath: String,
    val processedImagePath: String,
    val filterName: String = "AUTO",
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val rotationDegrees: Int = 0,
    val ocrText: String = "",
    val ocrBlocksJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    fun observeAllDocuments(): Flow<List<ScannedDocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :docId LIMIT 1")
    fun observeDocumentById(docId: String): Flow<ScannedDocumentEntity?>

    @Query("SELECT * FROM documents WHERE id = :docId LIMIT 1")
    suspend fun getDocumentById(docId: String): ScannedDocumentEntity?

    @Query("SELECT * FROM pages WHERE documentId = :docId ORDER BY pageIndex ASC")
    fun observePagesForDocument(docId: String): Flow<List<ScannedPageEntity>>

    @Query("SELECT * FROM pages WHERE documentId = :docId ORDER BY pageIndex ASC")
    suspend fun getPagesForDocument(docId: String): List<ScannedPageEntity>

    @Query("SELECT * FROM pages WHERE id = :pageId LIMIT 1")
    suspend fun getPageById(pageId: String): ScannedPageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDocument(document: ScannedDocumentEntity)

    @Update
    suspend fun updateDocument(document: ScannedDocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPages(pages: List<ScannedPageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPage(page: ScannedPageEntity)

    @Query("DELETE FROM pages WHERE id = :pageId")
    suspend fun deletePageById(pageId: String)

    @Query("DELETE FROM pages WHERE documentId = :docId")
    suspend fun deletePagesForDocument(docId: String)

    @Query("DELETE FROM documents WHERE id = :docId")
    suspend fun deleteDocumentById(docId: String)

    @Query("DELETE FROM documents WHERE id IN (:docIds)")
    suspend fun deleteDocumentsByIds(docIds: List<String>)

    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun observeAllFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFolder(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteFolderById(folderId: String)

    @Query("UPDATE documents SET folderId = '' WHERE folderId = :folderId")
    suspend fun clearFolderFromDocuments(folderId: String)

    @Transaction
    suspend fun saveDocumentWithPages(
        document: ScannedDocumentEntity,
        pages: List<ScannedPageEntity>
    ) {
        upsertDocument(document)
        deletePagesForDocument(document.id)
        upsertPages(pages)
    }
}

@Database(
    entities = [ScannedDocumentEntity::class, ScannedPageEntity::class, FolderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DocuScanDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
}
