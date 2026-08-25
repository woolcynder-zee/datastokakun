package com.stokakun.app.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * Screenshots are copied as real JPEG/PNG files into app-private internal
 * storage (filesDir/screenshots). Only the file path is stored in Room -
 * no Base64, no blobs.
 */
object ImageStorageManager {

    private const val FOLDER_NAME = "screenshots"

    private fun screenshotsDir(context: Context): File {
        val dir = File(context.filesDir, FOLDER_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Copies the content:// image at [sourceUri] into app-private storage
     * and returns the absolute path of the new file, or null on failure.
     */
    fun copyImageToLocalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val extension = guessExtension(context, sourceUri)
            val fileName = "${UUID.randomUUID()}.$extension"
            val destFile = File(screenshotsDir(context), fileName)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Copies an already-local file (e.g. extracted from a backup zip) into
     * app-private storage under a fresh unique name. Used by BackupManager.
     */
    fun copyLocalFileToStorage(context: Context, sourceFile: File): String? {
        return try {
            val extension = sourceFile.extension.ifBlank { "jpg" }
            val destFile = File(screenshotsDir(context), "${UUID.randomUUID()}.$extension")
            sourceFile.inputStream().use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun guessExtension(context: Context, uri: Uri): String {
        val type = context.contentResolver.getType(uri) ?: return "jpg"
        return when {
            type.contains("png") -> "png"
            type.contains("webp") -> "webp"
            else -> "jpg"
        }
    }

    fun deleteFile(path: String) {
        try {
            val file = File(path)
            if (file.exists()) file.delete()
        } catch (_: Exception) {
            // best-effort delete
        }
    }

    fun deleteFiles(paths: List<String>) {
        paths.forEach { deleteFile(it) }
    }
}
