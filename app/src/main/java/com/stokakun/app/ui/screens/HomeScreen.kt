package com.stokakun.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.stokakun.app.data.AppDatabase
import com.stokakun.app.data.GameStat
import com.stokakun.app.ui.components.StockCard
import com.stokakun.app.ui.components.formatPrice
import com.stokakun.app.util.BackupManager
import com.stokakun.app.viewmodel.AccountViewModel
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: AccountViewModel,
    onAddClick: () -> Unit,
    onAccountClick: (Long) -> Unit,
    onSeeAllClick: () -> Unit,
    onStorageClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val total by viewModel.totalCount.collectAsState()
    val available by viewModel.availableCount.collectAsState()
    val reserved by viewModel.reservedCount.collectAsState()
    val sold by viewModel.soldCount.collectAsState()
    val activeValue by viewModel.activeStockValue.collectAsState()
    val soldValue by viewModel.soldStockValue.collectAsState()
    val gameStats by viewModel.gameStats.collectAsState()
    val recent by viewModel.recentAccounts.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showExportPassword by remember { mutableStateOf(false) }
    var showImportPassword by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf("") }
    var exportPasswordConfirm by remember { mutableStateOf("") }
    var importPassword by remember { mutableStateOf("") }
    var pendingExportPassword by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri == null) {
            pendingExportPassword = ""
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val password = pendingExportPassword
            runCatching {
                val db = AppDatabase.getInstance(context)
                BackupManager.export(context, uri, db.accountDao(), db.screenshotDao(), password)
            }.onSuccess {
                snackbarHostState.showSnackbar("Backup portable berhasil diekspor.")
            }.onFailure {
                snackbarHostState.showSnackbar("Gagal mengekspor backup: ${it.message ?: "kesalahan tidak diketahui"}")
            }
            pendingExportPassword = ""
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            importPassword = ""
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val password = importPassword.ifBlank { null }
            runCatching {
                val db = AppDatabase.getInstance(context)
                BackupManager.import(context, uri, db, db.accountDao(), db.screenshotDao(), password)
            }.onSuccess { success ->
                snackbarHostState.showSnackbar(
                    if (success) "Backup berhasil diimpor. Data duplikat otomatis dilewati."
                    else "Gagal membaca file backup."
                )
            }.onFailure {
                snackbarHostState.showSnackbar("Import dibatalkan: ${it.message ?: "file tidak valid"}")
            }
            importPassword = ""
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("Stok Akun") }, actions = {
                IconButton(onClick = onSettingsClick) { Icon(Icons.Filled.Settings, contentDescription = "Pengaturan") }
                IconButton(onClick = onStorageClick) { Icon(Icons.Filled.Storage, contentDescription = "Penyimpanan") }
                IconButton(onClick = { showExportPassword = true }) { Icon(Icons.Filled.FileUpload, contentDescription = "Export backup") }
                IconButton(onClick = { showImportPassword = true }) { Icon(Icons.Filled.FileDownload, contentDescription = "Import backup") }
            })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Tambah Akun") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SummaryCard("Nilai Stok Aktif", formatPrice(activeValue), Modifier.fillMaxWidth()) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryCard("Total Stok", total.toString(), Modifier.weight(1f))
                    SummaryCard("Available", available.toString(), Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryCard("Reserved", reserved.toString(), Modifier.weight(1f))
                    SummaryCard("Sold", sold.toString(), Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryCard("Nilai Terjual", formatPrice(soldValue), Modifier.weight(1f))
                    SummaryCard("Akun Terjual", sold.toString(), Modifier.weight(1f))
                }
            }
            if (gameStats.isNotEmpty()) {
                item { Text("Stok per Game", style = MaterialTheme.typography.titleMedium) }
                items(gameStats.take(8), key = { it.game }) { stat -> GameStatCard(stat) }
                if (gameStats.size > 8) item { Text("+ ${gameStats.size - 8} game lainnya", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            item {
                Spacer(Modifier.height(4.dp))
                Text("Stok Terbaru", style = MaterialTheme.typography.titleMedium)
            }
            if (recent.isEmpty()) {
                item { Text("Belum ada stok akun. Tekan \"Tambah Akun\" untuk mulai.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(recent, key = { it.id }) { account ->
                val shotCount by viewModel.screenshotCount(account.id).collectAsState()
                StockCard(account = account, screenshotCount = shotCount, onClick = { onAccountClick(account.id) })
            }
            item { TextButton(onClick = onSeeAllClick) { Text("Lihat semua stok") } }
        }
    }

    if (showExportPassword) {
        AlertDialog(
            onDismissRequest = { showExportPassword = false },
            title = { Text("Backup Portable") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Password ini diperlukan untuk memulihkan backup di HP lain. Minimal 8 karakter.")
                    OutlinedTextField(
                        value = exportPassword,
                        onValueChange = { if (it.length <= 64) exportPassword = it },
                        label = { Text("Password backup") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                    OutlinedTextField(
                        value = exportPasswordConfirm,
                        onValueChange = { if (it.length <= 64) exportPasswordConfirm = it },
                        label = { Text("Konfirmasi password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    when {
                        exportPassword.length < 8 -> scope.launch { snackbarHostState.showSnackbar("Password backup minimal 8 karakter.") }
                        exportPassword != exportPasswordConfirm -> scope.launch { snackbarHostState.showSnackbar("Konfirmasi password tidak cocok.") }
                        else -> {
                            pendingExportPassword = exportPassword
                            exportPassword = ""
                            exportPasswordConfirm = ""
                            showExportPassword = false
                            exportLauncher.launch("stok-akun-portable-backup.zip")
                        }
                    }
                }) { Text("Export") }
            },
            dismissButton = {
                TextButton(onClick = {
                    exportPassword = ""
                    exportPasswordConfirm = ""
                    showExportPassword = false
                }) { Text("Batal") }
            }
        )
    }

    if (showImportPassword) {
        AlertDialog(
            onDismissRequest = { showImportPassword = false },
            title = { Text("Import Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Untuk backup portable v2, masukkan password backup. Backup lama v1 boleh dibiarkan kosong.")
                    OutlinedTextField(
                        value = importPassword,
                        onValueChange = { if (it.length <= 64) importPassword = it },
                        label = { Text("Password backup (opsional untuk backup lama)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showImportPassword = false
                    importLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                }) { Text("Pilih File") }
            },
            dismissButton = {
                TextButton(onClick = {
                    importPassword = ""
                    showImportPassword = false
                }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GameStatCard(stat: GameStat) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stat.game, style = MaterialTheme.typography.titleMedium)
            Text("${stat.total} akun • ${stat.available} available • ${stat.reserved} reserved • ${stat.sold} sold", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Nilai aktif: ${formatPrice(stat.activeValue)}", color = MaterialTheme.colorScheme.primary)
        }
    }
}
