package com.stokakun.app.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stokakun.app.data.AccountEntity
import com.stokakun.app.data.AccountStatus
import com.stokakun.app.data.ScreenshotEntity
import com.stokakun.app.repository.AccountRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountViewModel(private val repository: AccountRepository) : ViewModel() {
    val totalCount = repository.getTotalCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val availableCount = repository.getCountByStatus(AccountStatus.AVAILABLE).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val reservedCount = repository.getCountByStatus(AccountStatus.RESERVED).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val soldCount = repository.getCountByStatus(AccountStatus.SOLD).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val activeStockValue = repository.getActiveStockValue().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val recentAccounts = repository.getRecent(5).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    private val _statusFilter = MutableStateFlow<AccountStatus?>(null)
    val statusFilter: StateFlow<AccountStatus?> = _statusFilter
    private val _sort = MutableStateFlow(SortOption.NEWEST)
    val sort: StateFlow<SortOption> = _sort

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setStatusFilter(status: AccountStatus?) { _statusFilter.value = status }
    fun setSort(option: SortOption) { _sort.value = option }

    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredAccounts: StateFlow<List<AccountEntity>> = combine(_searchQuery, _statusFilter, _sort) { query, status, sort -> Triple(query, status, sort) }
        .flatMapLatest { (query, status, sort) -> repository.getFiltered(status, query).map { sortAccounts(it, sort) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun sortAccounts(accounts: List<AccountEntity>, sort: SortOption): List<AccountEntity> = when (sort) {
        SortOption.NEWEST -> accounts.sortedByDescending { it.createdAt }
        SortOption.OLDEST -> accounts.sortedBy { it.createdAt }
        SortOption.NAME_AZ -> accounts.sortedBy { it.name.lowercase() }
        SortOption.NAME_ZA -> accounts.sortedByDescending { it.name.lowercase() }
        SortOption.PRICE_HIGH -> accounts.sortedByDescending { it.price }
        SortOption.PRICE_LOW -> accounts.sortedBy { it.price }
    }

    fun screenshotCount(accountId: Long): StateFlow<Int> = repository.getScreenshotCount(accountId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    fun accountFlow(id: Long): StateFlow<AccountEntity?> = repository.getAccountById(id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    fun screenshotsFlow(accountId: Long): StateFlow<List<ScreenshotEntity>> = repository.getScreenshots(accountId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Returns null when the Android Keystore key is unavailable/corrupted instead of crashing Compose. */
    fun decryptPasswordOrNull(encrypted: String): String? = runCatching { repository.decryptPassword(encrypted) }.getOrNull()

    fun saveAccount(existingId: Long?, originalCreatedAt: Long?, game: String, name: String, price: Long, status: AccountStatus, username: String, plainPassword: String, passwordEncryptedOverride: String?, notes: String, newImageUris: List<Uri>, removedScreenshotIds: List<Long>, onDone: (Long) -> Unit, onError: (Throwable) -> Unit = {}) {
        viewModelScope.launch {
            runCatching { repository.saveAccount(existingId, originalCreatedAt, game, name, price, status, username, plainPassword, passwordEncryptedOverride, notes, newImageUris, removedScreenshotIds) }.onSuccess(onDone).onFailure(onError)
        }
    }

    fun deleteAccount(account: AccountEntity, onDone: () -> Unit, onError: (Throwable) -> Unit = {}) {
        viewModelScope.launch { runCatching { repository.deleteAccount(account) }.onSuccess { onDone() }.onFailure(onError) }
    }

    fun bulkUpdateStatus(ids: Set<Long>, status: AccountStatus, onDone: (Int) -> Unit, onError: (Throwable) -> Unit = {}) {
        viewModelScope.launch { runCatching { repository.bulkUpdateStatus(ids.toList(), status) }.onSuccess(onDone).onFailure(onError) }
    }

    fun bulkDelete(ids: Set<Long>, onDone: (Int) -> Unit, onError: (Throwable) -> Unit = {}) {
        viewModelScope.launch { runCatching { repository.bulkDelete(ids.toList()) }.onSuccess(onDone).onFailure(onError) }
    }

    class Factory(private val repository: AccountRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AccountViewModel(modelClass as? Class<*>?.let { AccountViewModel::class.java }?.let { AccountViewModel::class.java } as Class<T>)
    }
}

enum class SortOption(val label: String) {
    NEWEST("Terbaru"), OLDEST("Terlama"), NAME_AZ("Nama A–Z"), NAME_ZA("Nama Z–A"), PRICE_HIGH("Harga tertinggi"), PRICE_LOW("Harga terendah")
}
