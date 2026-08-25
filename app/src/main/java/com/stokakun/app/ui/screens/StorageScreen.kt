package com.stokakun.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stokakun.app.data.ScreenshotDao
import com.stokakun.app.util.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(
    screenshotDao: ScreenshotDao,
    context: android.content.Context,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var report by remember { mutableStateOf<StorageManager.Report?>(null) }

    fun refresh() {
        scope.launch {
            val paths = withContext(Dispatchers.IO) { screenshotDao.getAllOnce().map { it.filePath }.toSet() }
            report = withContext(Dispatchers.IO) { StorageManager.report(context, paths) }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Penyimpanan") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Screenshot tersimpan", style = MaterialTheme.typography.titleMedium)
                    Text("File: ${report?.fileCount ?: 0}")
                    Text("Ukuran: ${formatBytes(report?.totalBytes ?: 0)}")
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("File tidak terpakai", style = MaterialTheme.typography.titleMedium)
                    Text("File: ${report?.orphanCount ?: 0}")
                    Text("Ukuran: ${formatBytes(report?.orphanBytes ?: 0)}")
                    Text(
                        "File ini tidak lagi tercatat di database dan aman dibersihkan.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Button(
                onClick = {
                    scope.launch {
                        val paths = withContext(Dispatchers.IO) { screenshotDao.getAllOnce().map { it.filePath }.toSet() }
                        val deleted = withContext(Dispatchers.IO) { StorageManager.deleteOrphans(context, paths) }
                        refresh()
                        snackbar.showSnackbar(if (deleted == 0) "Tidak ada file sampah." else "$deleted file sampah berhasil dibersihkan.")
                    }
                },
                enabled = (report?.orphanCount ?: 0) > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.DeleteSweep, contentDescription = null)
                Text("  Bersihkan file sampah")
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    return "%.2f GB".format(mb / 1024.0)
}
