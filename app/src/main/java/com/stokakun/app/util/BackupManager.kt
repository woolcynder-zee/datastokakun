package com.stokakun.app.util

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.stokakun.app.data.AccountDao
import com.stokakun.app.data.AccountEntity
import com.stokakun.app.data.AccountStatus
import com.stokakun.app.data.AppDatabase
import com.stokakun.app.data.ScreenshotDao
import com.stokakun.app.data.ScreenshotEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupManager {
    private const val METADATA_FILE = "metadata.json"
    private const val SCREENSHOTS_FOLDER = "screenshots"
    private const val FORMAT_VERSION = 2
    private const val LEGACY_FORMAT_VERSION = 1
    private const val MAX_ENTRIES = 2000
    private const val MAX_UNCOMPRESSED_BYTES = 512L * 1024L * 1024L

    suspend fun export(
        context: Context,
        destUri: Uri,
        accountDao: AccountDao,
        screenshotDao: ScreenshotDao,
        backupPassword: String
    ) {
        require(backupPassword.length >= 8) { "Password backup minimal 8 karakter." }
        val accounts = accountDao.getAllOnce()
        val screenshots = screenshotDao.getAllOnce()
        val accountsJson = JSONArray()
        accounts.forEach { acc ->
            val plainPassword = CryptoManager.decryptOrNull(acc.passwordEncrypted)
                ?: throw IllegalArgumentException(
                    "Tidak bisa membaca password akun ${acc.name}. Data mungkin berasal dari instalasi lama atau Keystore tidak tersedia."
                )
            accountsJson.put(JSONObject().apply {
                put("id", acc.id)
                put("game", acc.game)
                put("name", acc.name)
                put("price", acc.price)
                put("status", acc.status.name)
                put("username", acc.username)
                put("passwordBackup", PortableBackupCrypto.encrypt(plainPassword, backupPassword))
                put("notes", acc.notes)
                put("createdAt", acc.createdAt)
            })
        }
        val screenshotsJson = JSONArray()
        screenshots.forEach { shot ->
            val file = File(shot.filePath)
            if (!file.exists() || !file.isFile) return@forEach
            screenshotsJson.put(JSONObject().apply {
                put("accountId", shot.accountId)
                put("zipEntry", "$SCREENSHOTS_FOLDER/${file.name}")
                put("createdAt", shot.createdAt)
                put("sortOrder", shot.sortOrder)
            })
        }
        val root = JSONObject().apply {
            put("version", FORMAT_VERSION)
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
                    if (file.exists() && file.isFile) {
                        zip.putNextEntry(ZipEntry("$SCREENSHOTS_FOLDER/${file.name}"))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
        } ?: error("Tidak dapat membuka lokasi backup.")
    }

    suspend fun import(
        context: Context,
        sourceUri: Uri,
        database: AppDatabase,
        accountDao: AccountDao,
        screenshotDao: ScreenshotDao,
        backupPassword: String?
    ): Boolean {
        val tempDir = File(context.cacheDir, "backup_import_${UUID.randomUUID()}")
        require(tempDir.mkdirs()) { "Tidak dapat membuat folder sementara." }
        val copiedFiles = mutableListOf<String>()
        try {
            var metadataText: String? = null
            val extractedFiles = mutableMapOf<String, File>()
            var entryCount = 0
            var totalBytes = 0L
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                ZipInputStream(input).use { zip ->
                    var entry: ZipEntry? = zip.nextEntry
                    while (entry != null) {
                        entryCount++
                        require(entryCount <= MAX_ENTRIES) { "Backup berisi terlalu banyak file." }
                        if (!entry.isDirectory) {
                            val name = entry.name
                            require(name == METADATA_FILE || isSafeScreenshotEntry(name)) { "Backup memiliki path/file yang tidak valid." }
                            if (name == METADATA_FILE) {
                                val bytes = zip.readBytes()
                                totalBytes += bytes.size
                                require(totalBytes <= MAX_UNCOMPRESSED_BYTES) { "Backup terlalu besar." }
                                metadataText = bytes.toString(Charsets.UTF_8)
                            } else {
                                val safeName = File(name).name
                                require(safeName == name.substringAfterLast('/')) { "Nama file screenshot tidak valid." }
                                val outFile = File(tempDir, safeName)
                                outFile.outputStream().use { output ->
                                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                                    while (true) {
                                        val read = zip.read(buffer)
                                        if (read <= 0) break
                                        totalBytes += read
                                        require(totalBytes <= MAX_UNCOMPRESSED_BYTES) { "Backup terlalu besar." }
                                        output.write(buffer, 0, read)
                                    }
                                }
                                extractedFiles[name] = outFile
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: return false

            val json = JSONObject(metadataText ?: return false)
            val version = json.optInt("version", -1)
            require(version == FORMAT_VERSION || version == LEGACY_FORMAT_VERSION) { "Versi backup tidak didukung." }
            if (version == FORMAT_VERSION) {
                require(!backupPassword.isNullOrBlank()) { "Backup ini membutuhkan password backup." }
                require(backupPassword.length >= 8) { "Password backup minimal 8 karakter." }
            }
            val accountsJson = json.getJSONArray("accounts")
            val screenshotsJson = json.getJSONArray("screenshots")
            require(accountsJson.length() <= MAX_ENTRIES) { "Backup berisi terlalu banyak akun." }

            val idMap = mutableMapOf<Long, Long>()
            val newlyInsertedOldIds = mutableSetOf<Long>()
            database.withTransaction {
                for (i in 0 until accountsJson.length()) {
                    val a = accountsJson.getJSONObject(i)
                    val oldId = a.getLong("id")
                    val game = a.getString("game").trim()
                    val name = a.getString("name").trim()
                    val username = a.getString("username").trim()
                    require(game.isNotBlank() && name.isNotBlank()) { "Data akun di backup tidak valid." }
                    val duplicate = accountDao.findDuplicate(game, name, username)
                    if (duplicate != null) {
                        idMap[oldId] = duplicate.id
                        continue
                    }
                    val passwordEncrypted = if (version == FORMAT_VERSION) {
                        val plain = PortableBackupCrypto.decrypt(a.getString("passwordBackup"), backupPassword!!)
                        CryptoManager.encrypt(plain)
                    } else {
                        a.getString("passwordEncrypted")
                    }
                    val newId = accountDao.insert(
                        AccountEntity(
                            game = game,
                            name = name,
                            price = a.getLong("price").coerceAtLeast(0L),
                            status = AccountStatus.valueOf(a.getString("status")),
                            username = username,
                            passwordEncrypted = passwordEncrypted,
                            notes = a.getString("notes"),
                            createdAt = a.getLong("createdAt")
                        )
                    )
                    idMap[oldId] = newId
                    newlyInsertedOldIds += oldId
                }

                for (i in 0 until screenshotsJson.length()) {
                    val s = screenshotsJson.getJSONObject(i)
                    val oldAccountId = s.getLong("accountId")
                    if (oldAccountId !in newlyInsertedOldIds) continue
                    val newAccountId = idMap[oldAccountId] ?: continue
                    val zipEntry = s.getString("zipEntry")
                    val sourceFile = extractedFiles[zipEntry] ?: continue
                    val newPath = ImageStorageManager.copyLocalFileToStorage(context, sourceFile)
                        ?: throw IllegalArgumentException("Gagal memulihkan screenshot.")
                    copiedFiles += newPath
                    screenshotDao.insert(
                        ScreenshotEntity(
                            accountId = newAccountId,
                            filePath = newPath,
                            createdAt = s.getLong("createdAt"),
                            sortOrder = s.getInt("sortOrder")
                        )
                    )
                }
            }
            return true
        } catch (e: Exception) {
            ImageStorageManager.deleteFiles(copiedFiles)
            throw e
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun isSafeScreenshotEntry(name: String): Boolean {
        if (!name.startsWith("$SCREENSHOTS_FOLDER/")) return false
        val fileName = name.removePrefix("$SCREENSHOTS_FOLDER/")
        if (fileName.isBlank() || fileName.contains('/') || fileName.contains('\\')) return false
        if (fileName == "." || fileName == ".." || fileName.contains("..")) return false
        return true
    }
}
