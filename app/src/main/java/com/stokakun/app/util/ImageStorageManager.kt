package com.stokakun.app.util

import android.content.Context
import android.graphics.BitmapFactory
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
    private const val MAX_IMAGE_PIXELS = 40_000_000L
    private const val MAX_IMAGE_DIMENSION = 12_000

    private fun screenshotsDir(context: Context): File {
        val dir = File(context.filesDir, FOLDER_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun copyImageToLocalStorage(context: Context, sourceUri: Uri): String? {
        var destFile: File? = null
        return try {
            val mimeType = context.contentResolver.getType(sourceUri)
            require(mimeType?.startsWith("image/") == true) { "File yang dipilih bukan gambar." }

            val extension = guessExtension(mimeType)
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

            require(isReadableImage(file)) { "File yang dipilih bukan gambar yang valid." }
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
            require(sourceFile.length() in 1..MAX_IMAGE_BYTES) { "Ukuran screenshot tidak valid." }
            require(isReadableImage(sourceFile)) { "File backup berisi screenshot yang tidak valid." }
            val extension = sourceFile.extension.lowercase().ifBlank { "jpg" }
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

    private fun guessExtension(mimeType: String): String = when {
        mimeType.contains("png", ignoreCase = true) -> "png"
        mimeType.contains("webp", ignoreCase = true) -> "webp"
        mimeType.contains("jpeg", ignoreCase = true) || mimeType.contains("jpg", ignoreCase = true) -> "jpg"
        else -> "img"
    }

    private fun isReadableImage(file: File): Boolean {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        val width = options.outWidth
        val height = options.outHeight
        if (width <= 0 || height <= 0) return false
        if (width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION) return false
        return width.toLong() * height.toLong() <= MAX_IMAGE_PIXELS
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
