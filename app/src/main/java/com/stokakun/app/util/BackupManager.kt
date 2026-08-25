package com.stokakun.app.util

import android.content.Context
import android.net.Uri
import com.stokakun.app.data.AccountDao
import com.stokakun.app.data.AccountEntity
import com.stokakun.app.data.AccountStatus
import com.stokakun.app.data.ScreenshotDao
import com.stokakun.app.data.ScreenshotEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Backup format: a single .zip containing
 *   metadata.json                 -> accounts + screenshot index (small, text only)
 *   screenshots/<uuid>.<ext>      -> the actual screenshot files
 *
 * Screenshot binary data is stored as real files inside the zip, never as
 * Base64 inside the JSON, so backups stay light even with many images.
 *
 * Note: passwords are stored encrypted with an Android Keystore key that is
 * bound to this app installation/device. A backup restored on the same
 * install works normally; restoring after uninstall/reinstall or on another
 * device will not be able to decrypt previously saved passwords.
 */
object BackupManager {

    private const val METADATA_FILE = "metadata.json"
    private const val SCREENSHOTS_FOLDER = "screenshots"

    suspend fun export(
        context: Context,
        destUri: Uri,
        accountDao: AccountDao,
        screenshotDao: ScreenshotDao
    ) {
        val accounts = accountDao.getAllOnce()
        val screenshots = screenshotDao.getAllOnce()

        val accountsJson = JSONArray()
        accounts.forEach { acc ->
            accountsJson.put(
                JSONObject().apply {
                    put("id", acc.id)
                    put("game", acc.game)
                    put("name", acc.name)
                    put("price", acc.price)
                    put("status", acc.status.name)
                    put("username", acc.username)
                    put("passwordEncrypted", acc.passwordEncrypted)
                    put("notes", acc.notes)
                    put("createdAt", acc.createdAt)
                }
            )
        }

        val screenshotsJson = JSONArray()
        screenshots.forEach { shot ->
            val file = File(shot.filePath)
            val zipEntryName = "$SCREENSHOTS_FOLDER/${file.name}"
            screenshotsJson.put(
                JSONObject().apply {
                    put("accountId", shot.accountId)
                    put("zipEntry", zipEntryName)
                    put("createdAt", shot.createdAt)
                    put("sortOrder", shot.sortOrder)
                }
            )
        }

        val root = JSONObject().apply {
            put("version", 1)
            put("accounts", accountsJson)
            put("screenshots", screenshotsJson)
        }

        context.contentResolver.openOutputStream(destUri)?.use { out ->
            ZipOutputStream(out).use { zip ->
                zip.putNextEntry(ZipEntry(METADATA_FILE))
                zip.write(root.toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                screenshots.forEach { shot ->
                    val file = File(shot.filePath)
                    if (file.exists()) {
                        zip.putNextEntry(ZipEntry("$SCREENSHOTS_FOLDER/${file.name}"))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
        }
    }

    suspend fun import(
        context: Context,
        sourceUri: Uri,
        accountDao: AccountDao,
        screenshotDao: ScreenshotDao
    ): Boolean {
        val tempDir = File(context.cacheDir, "backup_import_${UUID.randomUUID()}")
        tempDir.mkdirs()
        try {
            var metadataText: String? = null
            val extractedFiles = mutableMapOf<String, File>()

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            if (entry.name == METADATA_FILE) {
                                metadataText = zip.readBytes().toString(Charsets.UTF_8)
                            } else if (entry.name.startsWith("$SCREENSHOTS_FOLDER/")) {
                                val outFile = File(tempDir, entry.name.substringAfterLast("/"))
                                outFile.outputStream().use { zip.copyTo(it) }
                                extractedFiles[entry.name] = outFile
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: return false

            val json = metadataText?.let { JSONObject(it) } ?: return false
            val accountsJson = json.getJSONArray("accounts")
            val screenshotsJson = json.getJSONArray("screenshots")

            // Map old account id (from backup) -> new inserted account id.
            val idMap = mutableMapOf<Long, Long>()

            for (i in 0 until accountsJson.length()) {
                val a = accountsJson.getJSONObject(i)
                val oldId = a.getLong("id")
                val newId = accountDao.insert(
                    AccountEntity(
                        game = a.getString("game"),
                        name = a.getString("name"),
                        price = a.getLong("price"),
                        status = AccountStatus.valueOf(a.getString("status")),
                        username = a.getString("username"),
                        passwordEncrypted = a.getString("passwordEncrypted"),
                        notes = a.getString("notes"),
                        createdAt = a.getLong("createdAt")
                    )
                )
                idMap[oldId] = newId
            }

            for (i in 0 until screenshotsJson.length()) {
                val s = screenshotsJson.getJSONObject(i)
                val oldAccountId = s.getLong("accountId")
                val newAccountId = idMap[oldAccountId] ?: continue
                val zipEntry = s.getString("zipEntry")
                val sourceFile = extractedFiles[zipEntry] ?: continue

                val newPath = ImageStorageManager.copyLocalFileToStorage(context, sourceFile)
                    ?: continue

                screenshotDao.insert(
                    ScreenshotEntity(
                        accountId = newAccountId,
                        filePath = newPath,
                        createdAt = s.getLong("createdAt"),
                        sortOrder = s.getInt("sortOrder")
                    )
                )
            }

            return true
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
