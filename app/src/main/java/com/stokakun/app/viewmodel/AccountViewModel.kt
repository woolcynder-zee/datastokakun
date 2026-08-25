package com.stokakun.app.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stokakun.app.data.AccountEntity
import com.stokakun.app.data.AccountStatus
import com.stokakun.app.data.ScreenshotEntity
import com.stokakun.app.repository.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountViewModel(private val repository: AccountRepository) : ViewModel() {

    val totalCount: StateFlow<Int> = repository.getTotalCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val availableCount: StateFlow<Int> = repository.getCountByStatus(AccountStatus.AVAILABLE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val reservedCount: StateFlow<Int> = repository.getCountByStatus(AccountStatus.RESERVED)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val soldCount: StateFlow<Int> = repository.getCountByStatus(AccountStatus.SOLD)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentAccounts: StateFlow<List<AccountEntity>> = repository.getRecent(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _statusFilter = MutableStateFlow<AccountStatus?>(null)
    val statusFilter: StateFlow<AccountStatus?> = _statusFilter

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(status: AccountStatus?) {
        _statusFilter.value = status
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val filteredAccounts: StateFlow<List<AccountEntity>> =
        combine(_searchQuery, _statusFilter) { query, status -> query to status }
            .flatMapLatest { (query, status) -> repository.getFiltered(status, query) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun screenshotCount(accountId: Long): StateFlow<Int> =
        repository.getScreenshotCount(accountId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun accountFlow(id: Long): StateFlow<AccountEntity?> =
        repository.getAccountById(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun screenshotsFlow(accountId: Long): StateFlow<List<ScreenshotEntity>> =
        repository.getScreenshots(accountId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun decryptPassword(encrypted: String): String = repository.decryptPassword(encrypted)

    fun saveAccount(
        existingId: Long?,
        originalCreatedAt: Long?,
        game: String,
        name: String,
        price: Long,
        status: AccountStatus,
        username: String,
        plainPassword: String,
        passwordEncryptedOverride: String?,
        notes: String,
        newImageUris: List<Uri>,
        removedScreenshotIds: List<Long>,
        onDone: (Long) -> Unit,
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            runCatching {
                repository.saveAccount(
                    existingId,
                    originalCreatedAt,
                    game,
                    name,
                    price,
                    status,
                    username,
                    plainPassword,
                    passwordEncryptedOverride,
                    notes,
                    newImageUris,
                    removedScreenshotIds
                )
            }.onSuccess(onDone).onFailure(onError)
        }
    }

    fun deleteAccount(
        account: AccountEntity,
        onDone: () -> Unit,
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            runCatching { repository.deleteAccount(account) }
                .onSuccess { onDone() }
                .onFailure(onError)
        }
    }

    class Factory(private val repository: AccountRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AccountViewModel(repository) as T
        }
    }
}
