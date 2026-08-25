package com.stokakun.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.stokakun.app.data.AppDatabase
import com.stokakun.app.ui.components.StockCard
import com.stokakun.app.util.BackupManager
import com.stokakun.app.viewmodel.AccountViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AccountViewModel,
    onAddClick: () -> Unit,
    onAccountClick: (Long) -> Unit,
    onSeeAllClick: () -> Unit
) {
    val total by viewModel.totalCount.collectAsState()
    val available by viewModel.availableCount.collectAsState()
    val reserved by viewModel.reservedCount.collectAsState()
    val sold by viewModel.soldCount.collectAsState()
    val recent by viewModel.recentAccounts.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var backupMessage by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val db = AppDatabase.getInstance(context)
                BackupManager.export(context, uri, db.accountDao(), db.screenshotDao())
                backupMessage = "Backup berhasil diekspor."
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val db = AppDatabase.getInstance(context)
                val success = BackupManager.import(context, uri, db.accountDao(), db.screenshotDao())
                backupMessage = if (success) "Backup berhasil diimpor." else "Gagal membaca file backup."
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stok Akun") },
                actions = {
                    IconButton(onClick = { exportLauncher.launch("stok-akun-backup.zip") }) {
                        Icon(Icons.Filled.FileUpload, contentDescription = "Export backup")
                    }
                    IconButton(onClick = { importLauncher.launch(arrayOf("application/zip")) }) {
                        Icon(Icons.Filled.FileDownload, contentDescription = "Import backup")
                    }
                }
            )
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (backupMessage != null) {
                item {
                    Text(
                        text = backupMessage ?: "",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryCard(label = "Total Stok", value = total, modifier = Modifier.weight(1f))
                    SummaryCard(label = "Available", value = available, modifier = Modifier.weight(1f))
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryCard(label = "Reserved", value = reserved, modifier = Modifier.weight(1f))
                    SummaryCard(label = "Sold", value = sold, modifier = Modifier.weight(1f))
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Stok Terbaru",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (recent.isEmpty()) {
                item {
                    Text(
                        text = "Belum ada stok akun. Tekan \"Tambah Akun\" untuk mulai.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(recent, key = { it.id }) { account ->
                val shotCount by viewModel.screenshotCount(account.id).collectAsState()
                StockCard(
                    account = account,
                    screenshotCount = shotCount,
                    onClick = { onAccountClick(account.id) }
                )
            }

            item {
                androidx.compose.material3.TextButton(onClick = onSeeAllClick) {
                    Text("Lihat semua stok")
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "$value",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
