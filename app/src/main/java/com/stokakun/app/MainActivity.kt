package com.stokakun.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
                    val lifecycleOwner = LocalLifecycleOwner.current

                    DisposableEffect(lifecycleOwner, lockManager.isEnabled) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_STOP && lockManager.isEnabled) {
                                unlocked = false
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }

                    if (unlocked) {
                        val viewModel: AccountViewModel = viewModel(factory = AccountViewModel.Factory(app.repository))
                        StokAkunNavGraph(viewModel = viewModel)
                    } else {
                        LockScreen { pin ->
                            val verified = lockManager.verifyPin(pin)
                            if (verified) unlocked = true
                            verified
                        }
                    }
                }
            }
        }
    }
}
