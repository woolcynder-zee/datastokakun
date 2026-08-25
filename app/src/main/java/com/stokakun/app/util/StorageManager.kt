package com.stokakun.app.util

import android.content.Context
import java.io.File

object StorageManager {
    data class Report(
        val totalBytes: Long,
        val fileCount: Int,
        val orphanBytes: Long,
        val orphanCount: Int
    )

    private fun screenshotDir(context: Context): File = File(context.filesDir, "screenshots")

    fun report(context: Context, referencedPaths: Set<String>): Report {
        val files = screenshotDir(context).listFiles()?.filter { it.isFile }.orEmpty()
        val orphans = files.filter { it.absolutePath !in referencedPaths }
        return Report(
            totalBytes = files.sumOf { it.length() },
            fileCount = files.size,
            orphanBytes = orphans.sumOf { it.length() },
            orphanCount = orphans.size
        )
    }

    fun deleteOrphans(context: Context, referencedPaths: Set<String>): Int {
        val files = screenshotDir(context).listFiles()?.filter { it.isFile }.orEmpty()
        var deleted = 0
        files.filter { it.absolutePath !in referencedPaths }.forEach { if (it.delete()) deleted++ }
        return deleted
    }
}
