package com.stokakun.app.ui.screens

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.stokakun.app.ui.components.StatusBadge
import com.stokakun.app.ui.components.formatPrice
import com.stokakun.app.viewmodel.AccountViewModel
import java.io.File

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Akun") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    if (account != null) {
                        IconButton(onClick = { onEdit(accountId) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Hapus")
                        }
                    }
                }
            )
        }
    ) { padding ->
        val acc = account
        when {
            acc == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            "Memuat detail akun...",
                            modifier = Modifier.padding(top = 16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
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
                    item { DetailInfoCard(title = "Email / Username", value = acc.username.ifBlank { "-" }) }
                    item {
                        val decrypted = remember(acc.passwordEncrypted) { 
                            try {
                                viewModel.decryptPassword(acc.passwordEncrypted)
                            } catch (e: Exception) {
                                ""
                            }
                        }
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Password", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(if (passwordVisible) decrypted.ifBlank { "-" } else "••••••••", modifier = Modifier.padding(top = 4.dp))
                                }
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = null)
                                }
                            }
                        }
                    }
                    item { DetailInfoCard(title = "Catatan", value = acc.notes.ifBlank { "-" }) }
                    item { Text("Gallery Fullspek (${screenshots.size})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground) }
                    item {
                        if (screenshots.isEmpty()) {
                            Text("Belum ada screenshot fullspek.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            GalleryGrid(paths = screenshots.map { it.filePath }, onImageClick = { index -> onImageClick(accountId, index) })
                        }
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
                                viewModel.deleteAccount(acc, onDone = { onDeleted() })
                            }) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
                        },
                        dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Batal") } }
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailInfoCard(title: String, value: String) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, modifier = Modifier.padding(top = 4.dp))
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
                        Box(
                            modifier = Modifier.weight(1f).aspectRatio(1f)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp)).clickable { onImageClick(index) }
                        ) {
                            AsyncImage(model = File(paths[index]), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                    } else Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
