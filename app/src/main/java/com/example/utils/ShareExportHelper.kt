package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object ShareExportHelper {

    fun formatFileSize(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024.0) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format("%.2f MB", mb)
    }

    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String = "Share Document") {
        if (!file.exists()) return
        val uri = getUriForFile(context, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun sharePdfFile(context: Context, pdfFile: File, title: String = "Share PDF") {
        shareFile(context, pdfFile, "application/pdf", title)
    }

    fun shareTextFile(context: Context, txtFile: File, title: String = "Share Text File") {
        shareFile(context, txtFile, "text/plain", title)
    }

    fun shareMultipleFiles(context: Context, files: List<File>, mimeType: String, chooserTitle: String = "Share Files") {
        val existing = files.filter { it.exists() }
        if (existing.isEmpty()) return
        if (existing.size == 1) {
            shareFile(context, existing.first(), mimeType, chooserTitle)
            return
        }
        val uris = ArrayList(existing.map { getUriForFile(context, it) })
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mimeType
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun sharePlainText(context: Context, title: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(intent, "Share Extracted Text").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun exportTextToCacheFile(context: Context, text: String, fileNameWithoutExt: String): File {
        val clean = fileNameWithoutExt.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "ocr_export" }
        val file = File(context.cacheDir, "$clean.txt")
        FileOutputStream(file).use { out ->
            out.write(text.toByteArray(Charsets.UTF_8))
        }
        return file
    }

    suspend fun createTempTxtFile(context: Context, fileName: String, content: String): File = withContext(Dispatchers.IO) {
        exportTextToCacheFile(context, content, fileName)
    }

    fun copyFileToUri(context: Context, sourceFile: File, destUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(destUri)?.use { output ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun copyFileToSafUri(context: Context, sourceFile: File, destUri: Uri): Boolean = withContext(Dispatchers.IO) {
        copyFileToUri(context, sourceFile, destUri)
    }

    fun writeTextToUri(context: Context, text: String, destUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(destUri)?.use { output ->
                output.write(text.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun writeTextToSafUri(context: Context, text: String, destUri: Uri): Boolean = withContext(Dispatchers.IO) {
        writeTextToUri(context, text, destUri)
    }
}
