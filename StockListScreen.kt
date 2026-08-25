package com.stokakun.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stokakun.app.data.AccountStatus
import com.stokakun.app.ui.components.StockCard
import com.stokakun.app.viewmodel.AccountViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockListScreen(
    viewModel: AccountViewModel,
    onAccountClick: (Long) -> Unit,
    onBack: () -> Unit
) {
    val query by viewModel.searchQuery.collectAsState()
    val filter by viewModel.statusFilter.collectAsState()
    val accounts by viewModel.filteredAccounts.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Daftar Stok") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    label = { Text("Cari game atau nama/ID stok") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filter == null,
                        onClick = { viewModel.setStatusFilter(null) },
                        label = { Text("Semua") }
                    )
                    FilterChip(
                        selected = filter == AccountStatus.AVAILABLE,
                        onClick = { viewModel.setStatusFilter(AccountStatus.AVAILABLE) },
                        label = { Text("Available") }
                    )
                    FilterChip(
                        selected = filter == AccountStatus.RESERVED,
                        onClick = { viewModel.setStatusFilter(AccountStatus.RESERVED) },
                        label = { Text("Reserved") }
                    )
                    FilterChip(
                        selected = filter == AccountStatus.SOLD,
                        onClick = { viewModel.setStatusFilter(AccountStatus.SOLD) },
                        label = { Text("Sold") }
                    )
                }
            }

            if (accounts.isEmpty()) {
                item {
                    Text(
                        text = "Tidak ada stok yang cocok.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(accounts, key = { it.id }) { account ->
                val shotCount by viewModel.screenshotCount(account.id).collectAsState()
                StockCard(
                    account = account,
                    screenshotCount = shotCount,
                    onClick = { onAccountClick(account.id) }
                )
            }
        }
    }
}
