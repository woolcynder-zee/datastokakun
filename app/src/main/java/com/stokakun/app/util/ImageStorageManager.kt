package com.stokakun.app.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * Screenshots are copied as real JPEG/PNG/WebP files into app-private
 * internal storage. Only the file path is stored in Room.
 */
object ImageStorageManager {

    private const val FOLDER_NAME = "screenshots"
    private const val MAX_IMAGE_BYTES = 25L * 1024L * 1024L

    private fun screenshotsDir(context: Context): File {
        val dir = File(context.filesDir, FOLDER_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun copyImageToLocalStorage(context: Context, sourceUri: Uri): String? {
        var destFile: File? = null
        return try {
            val extension = guessExtension(context, sourceUri)
            val file = File(screenshotsDir(context), "${UUID.randomUUID()}.$extension")
            destFile = file
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        total += read
                        require(total <= MAX_IMAGE_BYTES) { "Ukuran screenshot terlalu besar. Maksimal 25 MB per gambar." }
                        output.write(buffer, 0, read)
                    }
                }
            } ?: return null
            file.absolutePath
        } catch (_: Exception) {
            destFile?.delete()
            null
        }
    }

    fun copyLocalFileToStorage(context: Context, sourceFile: File): String? {
        var destFile: File? = null
        return try {
            require(sourceFile.isFile) { "File screenshot tidak valid." }
            require(sourceFile.length() <= MAX_IMAGE_BYTES) { "Ukuran screenshot terlalu besar. Maksimal 25 MB per gambar." }
            val extension = sourceFile.extension.ifBlank { "jpg" }
            val file = File(screenshotsDir(context), "${UUID.randomUUID()}.$extension")
            destFile = file
            sourceFile.inputStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (_: Exception) {
            destFile?.delete()
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
        paths.forEach(::deleteFile)
    }
}
