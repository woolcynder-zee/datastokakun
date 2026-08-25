package com.stokakun.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.stokakun.app.ui.components.StatusBadge
import com.stokakun.app.ui.components.formatPrice
import com.stokakun.app.viewmodel.AccountViewModel
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: AccountViewModel,
    accountId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDeleted: () -> Unit,
    onImageClick: (Long, Int) -> Unit
) {
    val account by viewModel.accountFlow(accountId).collectAsState()
    val screenshots by viewModel.screenshotsFlow(accountId).collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun copyText(label: String, value: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        scope.launch {
            snackbarHostState.showSnackbar("$label disalin. Clipboard akan dibersihkan otomatis.")
            delay(30_000)
            runCatching {
                val current = clipboard.primaryClip
                val currentText = current?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
                if (currentText == value) clipboard.clearPrimaryClip()
            }
        }
    }

    fun shareAccount(username: String, password: String) {
        val text = buildString {
            appendLine("Game: ${account?.game.orEmpty()}")
            appendLine("Nama/ID: ${account?.name.orEmpty()}")
            appendLine("Username: $username")
            appendLine("Password: $password")
            appendLine("Status: ${account?.status?.name.orEmpty()}")
        }
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }, "Bagikan akun"))
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Detail Akun") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali") } },
                actions = {
                    IconButton(onClick = { onEdit(accountId) }) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                    IconButton(onClick = { showDeleteConfirm = true }) { Icon(Icons.Filled.Delete, contentDescription = "Hapus") }
                }
            )
        }
    ) { padding ->
        val acc = account
        if (acc == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        val decrypted = remember(acc.passwordEncrypted) { viewModel.decryptPassword(acc.passwordEncrypted) }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(acc.game, style = MaterialTheme.typography.titleLarge)
                            StatusBadge(status = acc.status)
                        }
                        Text(acc.name, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                        Text(formatPrice(acc.price), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
            item {
                DetailInfoCard(title = "Email / Username", value = acc.username.ifBlank { "-" }, action = {
                    if (acc.username.isNotBlank()) IconButton(onClick = { copyText("Username", acc.username) }) { Icon(Icons.Filled.ContentCopy, contentDescription = "Salin username") }
                })
            }
            item {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Password", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (passwordVisible) decrypted.ifBlank { "-" } else "••••••••", modifier = Modifier.padding(top = 4.dp))
                        }
                        Row {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = "Tampilkan password") }
                            if (decrypted.isNotBlank()) IconButton(onClick = { copyText("Password", decrypted) }) { Icon(Icons.Filled.ContentCopy, contentDescription = "Salin password") }
                        }
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { shareAccount(acc.username, decrypted) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Text(" Bagikan akun")
                    }
                }
            }
            item { DetailInfoCard(title = "Catatan", value = acc.notes.ifBlank { "-" }) }
            item { Text("Gallery Fullspek (${screenshots.size})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground) }
            item {
                if (screenshots.isEmpty()) Text("Belum ada screenshot fullspek.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else GalleryGrid(paths = screenshots.map { it.filePath }, onImageClick = { index -> onImageClick(accountId, index) })
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Hapus Akun") },
                text = { Text("Akun dan seluruh screenshot fullspek akan dihapus permanen. Lanjutkan?") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteAccount(acc, onDone = { onDeleted() }, onError = { scope.launch { snackbarHostState.showSnackbar("Gagal menghapus: ${it.message ?: "kesalahan tidak diketahui"}") } })
                    }) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Batal") } }
            )
        }
    }
}

@Composable
private fun DetailInfoCard(title: String, value: String, action: @Composable (() -> Unit)? = null) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, modifier = Modifier.padding(top = 4.dp))
            }
            action?.invoke()
        }
    }
}

@Composable
private fun GalleryGrid(paths: List<String>, onImageClick: (Int) -> Unit) {
    val columns = 3
    val rows = (paths.size + columns - 1) / columns
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in 0 until rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until columns) {
                    val index = row * columns + col
                    if (index < paths.size) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp)).clickable { onImageClick(index) }) {
                            AsyncImage(model = File(paths[index]), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                    } else Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
