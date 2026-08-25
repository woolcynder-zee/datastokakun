package com.stokakun.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.stokakun.app.data.AccountEntity
import com.stokakun.app.data.AccountStatus
import com.stokakun.app.data.ScreenshotEntity
import com.stokakun.app.ui.components.statusLabel
import com.stokakun.app.viewmodel.AccountViewModel
import java.io.File

private data class ThumbItem(
    val key: String,
    val isExisting: Boolean,
    val existingId: Long? = null,
    val existingPath: String? = null,
    val newUri: Uri? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountScreen(
    viewModel: AccountViewModel,
    accountId: Long?,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val isEdit = accountId != null

    val existingAccount: AccountEntity? = if (isEdit) {
        val state by viewModel.accountFlow(accountId!!).collectAsState()
        state
    } else {
        null
    }

    val existingScreenshots: List<ScreenshotEntity> = if (isEdit) {
        val state by viewModel.screenshotsFlow(accountId!!).collectAsState()
        state
    } else {
        emptyList()
    }

    var game by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(AccountStatus.AVAILABLE) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var initialized by remember { mutableStateOf(false) }

    var newImages by remember { mutableStateOf(listOf<Uri>()) }
    var removedExistingIds by remember { mutableStateOf(listOf<Long>()) }

    // Preload form fields once, the first time the existing account data arrives.
    if (isEdit && !initialized && existingAccount != null) {
        val acc = existingAccount
        game = acc.game
        name = acc.name
        price = acc.price.toString()
        status = acc.status
        username = acc.username
        password = viewModel.decryptPassword(acc.passwordEncrypted)
        notes = acc.notes
        initialized = true
    }

    val pickImagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            newImages = newImages + uris
        }
    }

    val visibleThumbs = remember(existingScreenshots, newImages, removedExistingIds) {
        val existing = existingScreenshots
            .filter { it.id !in removedExistingIds }
            .map { ThumbItem(key = "e${it.id}", isExisting = true, existingId = it.id, existingPath = it.filePath) }
        val added = newImages.mapIndexed { idx, uri ->
            ThumbItem(key = "n$idx-$uri", isExisting = false, newUri = uri)
        }
        existing + added
    }

    var statusMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Akun" else "Tambah Akun") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
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
            item {
                OutlinedTextField(
                    value = game,
                    onValueChange = { game = it },
                    label = { Text("Game") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama / ID Stok") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = price,
                    onValueChange = { input -> price = input.filter { it.isDigit() } },
                    label = { Text("Harga (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                ExposedDropdownMenuBox(
                    expanded = statusMenuExpanded,
                    onExpandedChange = { statusMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = statusLabel(status),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    DropdownMenu(
                        expanded = statusMenuExpanded,
                        onDismissRequest = { statusMenuExpanded = false }
                    ) {
                        AccountStatus.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(statusLabel(option)) },
                                onClick = {
                                    status = option
                                    statusMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Email / Username") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }

            item {
                Text(
                    text = "Fullspek (${visibleThumbs.size} screenshot)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            item {
                OutlinedButton(
                    onClick = {
                        pickImagesLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                    Text("  Tambah Screenshot")
                }
            }

            // Thumbnails rendered as a manual wrap-grid inside a single LazyColumn item
            // (avoids nesting a scrollable grid inside a scrollable column).
            item {
                ThumbnailGrid(
                    thumbs = visibleThumbs,
                    onRemove = { thumb ->
                        if (thumb.isExisting && thumb.existingId != null) {
                            removedExistingIds = removedExistingIds + thumb.existingId
                        } else if (thumb.newUri != null) {
                            newImages = newImages - thumb.newUri
                        }
                    }
                )
            }

            item {
                Button(
                    onClick = {
                        val priceValue = price.toLongOrNull() ?: 0L
                        viewModel.saveAccount(
                            existingId = accountId,
                            originalCreatedAt = existingAccount?.createdAt,
                            game = game.trim(),
                            name = name.trim(),
                            price = priceValue,
                            status = status,
                            username = username.trim(),
                            plainPassword = password,
                            notes = notes.trim(),
                            newImageUris = newImages,
                            removedScreenshotIds = removedExistingIds,
                            onDone = { onSaved() }
                        )
                    },
                    enabled = game.isNotBlank() && name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Simpan")
                }
            }
        }
    }
}

@Composable
private fun ThumbnailGrid(
    thumbs: List<ThumbItem>,
    onRemove: (ThumbItem) -> Unit
) {
    val columns = 3
    val rows = (thumbs.size + columns - 1) / columns
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in 0 until rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (col in 0 until columns) {
                    val index = row * columns + col
                    if (index < thumbs.size) {
                        ThumbnailCell(
                            thumb = thumbs[index],
                            onRemove = onRemove,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ThumbnailCell(
    thumb: ThumbItem,
    onRemove: (ThumbItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
    ) {
        val model: Any? = if (thumb.isExisting) File(thumb.existingPath ?: "") else thumb.newUri
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
        )
        IconButton(
            onClick = { onRemove(thumb) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(28.dp)
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Hapus screenshot",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
