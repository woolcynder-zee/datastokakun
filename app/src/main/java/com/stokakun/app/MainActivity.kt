package com.stokakun.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stokakun.app.ui.navigation.StokAkunNavGraph
import com.stokakun.app.ui.screens.LockScreen
import com.stokakun.app.ui.theme.StokAkunTheme
import com.stokakun.app.util.AppLockManager
import com.stokakun.app.viewmodel.AccountViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as StokAkunApp
        val lockManager = AppLockManager(this)

        setContent {
            StokAkunTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var unlocked by remember { mutableStateOf(!lockManager.isEnabled) }
                    if (unlocked) {
                        val viewModel: AccountViewModel = viewModel(factory = AccountViewModel.Factory(app.repository))
                        StokAkunNavGraph(viewModel = viewModel)
                    } else {
                        LockScreen { pin ->
                            lockManager.verifyPin(pin).also { if (it) unlocked = true }
                        }
                    }
                }
            }
        }
    }
}
