package com.stokakun.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun LockScreen(onUnlock: (String) -> Boolean, remainingLockoutSeconds: () -> Int = { 0 }) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var lockoutSeconds by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Stok Akun", style = MaterialTheme.typography.headlineMedium)
        Text("Masukkan PIN untuk membuka aplikasi.", modifier = Modifier.padding(top = 8.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) { pin = it; error = false } },
            label = { Text("PIN") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = error,
            supportingText = {
                when {
                    lockoutSeconds > 0 -> Text("Terlalu banyak percobaan. Tunggu $lockoutSeconds detik.")
                    error -> Text("PIN salah.")
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
        )
        Button(
            onClick = {
                val remaining = remainingLockoutSeconds()
                if (remaining > 0) {
                    lockoutSeconds = remaining
                    error = true
                } else {
                    val verified = onUnlock(pin)
                    val nextLockout = remainingLockoutSeconds()
                    lockoutSeconds = nextLockout
                    error = !verified
                    if (verified) pin = ""
                }
            },
            enabled = pin.length >= 4 && lockoutSeconds == 0,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) { Text("Buka") }
    }
}
