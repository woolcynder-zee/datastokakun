package com.stokakun.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stokakun.app.data.AccountStatus
import com.stokakun.app.ui.components.StockCard
import com.stokakun.app.viewmodel.AccountViewModel
import com.stokakun.app.viewmodel.SortOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockListScreen(viewModel: AccountViewModel, onAccountClick: (Long) -> Unit, onBack: () -> Unit) {
    val query by viewModel.searchQuery.collectAsState()
    val filter by viewModel.statusFilter.collectAsState()
    val accounts by viewModel.filteredAccounts.collectAsState()
    val sort by viewModel.sort.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    var showBulkMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val snackbar = remember { SnackbarHostState() }

    fun clearSelection() { selectedIds = emptySet() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(if (selectedIds.isEmpty()) "Daftar Stok" else "${selectedIds.size} dipilih") },
                navigationIcon = {
                    IconButton(onClick = { if (selectedIds.isEmpty()) onBack() else clearSelection() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    if (selectedIds.isEmpty()) {
                        androidx.compose.foundation.layout.Box {
                            IconButton(onClick = { showSortMenu = true }) { Icon(Icons.Filled.Sort, contentDescription = "Urutkan") }
                            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                SortOption.entries.forEach { option ->
                                    DropdownMenuItem(text = { Text(if (option == sort) "✓ ${option.label}" else option.label) }, onClick = { viewModel.setSort(option); showSortMenu = false })
                                }
                            }
                        }
                    } else {
                        androidx.compose.foundation.layout.Box {
                            IconButton(onClick = { showBulkMenu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Aksi massal") }
                            DropdownMenu(expanded = showBulkMenu, onDismissRequest = { showBulkMenu = false }) {
                                DropdownMenuItem(text = { Text("Tandai Available") }, onClick = {
                                    viewModel.bulkUpdateStatus(selectedIds, AccountStatus.AVAILABLE, { count -> clearSelection(); showBulkMenu = false }, { showBulkMenu = false })
                                })
                                DropdownMenuItem(text = { Text("Tandai Reserved") }, onClick = {
                                    viewModel.bulkUpdateStatus(selectedIds, AccountStatus.RESERVED, { count -> clearSelection(); showBulkMenu = false }, { showBulkMenu = false })
                                })
                                DropdownMenuItem(text = { Text("Tandai Sold") }, onClick = {
                                    viewModel.bulkUpdateStatus(selectedIds, AccountStatus.SOLD, { count -> clearSelection(); showBulkMenu = false }, { showBulkMenu = false })
                                })
                                DropdownMenuItem(text = { Text("Hapus yang dipilih") }, onClick = { showBulkMenu = false; showDeleteConfirm = true })
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                OutlinedTextField(value = query, onValueChange = viewModel::setSearchQuery, label = { Text("Cari game, nama, atau username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            item {
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = filter == null, onClick = { viewModel.setStatusFilter(null) }, label = { Text("Semua") })
                    FilterChip(selected = filter == AccountStatus.AVAILABLE, onClick = { viewModel.setStatusFilter(AccountStatus.AVAILABLE) }, label = { Text("Available") })
                    FilterChip(selected = filter == AccountStatus.RESERVED, onClick = { viewModel.setStatusFilter(AccountStatus.RESERVED) }, label = { Text("Reserved") })
                    FilterChip(selected = filter == AccountStatus.SOLD, onClick = { viewModel.setStatusFilter(AccountStatus.SOLD) }, label = { Text("Sold") })
                }
            }
            item { Text("Urutan: ${sort.label}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (accounts.isEmpty()) item { Text("Tidak ada stok yang cocok.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(accounts, key = { it.id }) { account ->
                val shotCount by viewModel.screenshotCount(account.id).collectAsState()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = account.id in selectedIds,
                        onClick = { selectedIds = if (account.id in selectedIds) selectedIds - account.id else selectedIds + account.id },
                        label = { Icon(if (account.id in selectedIds) Icons.Filled.Done else Icons.Filled.Done, contentDescription = null) }
                    )
                    StockCard(account, shotCount) { if (selectedIds.isEmpty()) onAccountClick(account.id) else selectedIds = if (account.id in selectedIds) selectedIds - account.id else selectedIds + account.id }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus ${selectedIds.size} akun?") },
            text = { Text("Akun yang dipilih beserta screenshot-nya akan dihapus dan tidak bisa dibatalkan.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.bulkDelete(selectedIds, { count -> clearSelection(); showDeleteConfirm = false }, { showDeleteConfirm = false })
                }) { Icon(Icons.Filled.Delete, contentDescription = null); Text(" Hapus") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Batal") } }
        )
    }
}
