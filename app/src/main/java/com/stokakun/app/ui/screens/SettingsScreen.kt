package com.stokakun.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.stokakun.app.util.AppLockManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(lockManager: AppLockManager, onBack: () -> Unit) {
    var enabled by remember { mutableStateOf(lockManager.isEnabled) }
    var showDialog by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Keamanan", style = MaterialTheme.typography.titleLarge)
            Text(
                if (enabled) "App Lock aktif. PIN diperlukan saat aplikasi dibuka kembali."
                else "App Lock belum aktif."
            )
            Text(
                "PIN disimpan sebagai hash dengan salt di penyimpanan aplikasi, bukan sebagai teks biasa.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!enabled) {
                Button(onClick = { pin = ""; confirm = ""; error = ""; showDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Aktifkan App Lock") }
            } else {
                Button(onClick = { lockManager.clearPin(); enabled = false }, modifier = Modifier.fillMaxWidth()) { Text("Matikan App Lock") }
                TextButton(onClick = { pin = ""; confirm = ""; error = ""; showDialog = true }) { Text("Ganti PIN") }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (enabled) "Ganti PIN" else "Buat PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { value -> if (value.length <= 8 && value.all(Char::isDigit)) pin = value },
                        label = { Text("PIN (4–8 digit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { value -> if (value.length <= 8 && value.all(Char::isDigit)) confirm = value },
                        label = { Text("Konfirmasi PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true
                    )
                    if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                Button(onClick = {
                    when {
                        pin.length !in 4..8 -> error = "PIN harus 4–8 digit."
                        pin != confirm -> error = "Konfirmasi PIN tidak cocok."
                        else -> { lockManager.setPin(pin); enabled = true; showDialog = false }
                    }
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Batal") } }
        )
    }
}
