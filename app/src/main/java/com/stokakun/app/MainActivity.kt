package com.stokakun.app

import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
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
    private var backgroundAt: Long? = null
    private var lockStateSetter: ((Boolean) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        val app = application as StokAkunApp
        val lockManager = AppLockManager(this)

        setContent {
            StokAkunTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var unlocked by remember { mutableStateOf(!lockManager.isEnabled) }
                    lockStateSetter = { locked ->
                        if (!lockManager.isEnabled) unlocked = true
                        else if (locked) unlocked = false
                    }
                    val lifecycleOwner = LocalLifecycleOwner.current

                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_DESTROY) lockStateSetter = null
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                            lockStateSetter = null
                        }
                    }

                    if (unlocked) {
                        val viewModel: AccountViewModel = viewModel(factory = AccountViewModel.Factory(app.repository))
                        StokAkunNavGraph(viewModel = viewModel)
                    } else {
                        LockScreen(
                            onUnlock = { pin ->
                                val verified = lockManager.verifyPin(pin)
                                if (verified) {
                                    backgroundAt = null
                                    unlocked = true
                                }
                                verified
                            },
                            remainingLockoutSeconds = { lockManager.remainingLockoutSeconds }
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val startedAt = backgroundAt
        if (startedAt != null && SystemClock.elapsedRealtime() - startedAt >= LOCK_AFTER_BACKGROUND_MS) {
            backgroundAt = null
            lockStateSetter?.invoke(true)
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) backgroundAt = SystemClock.elapsedRealtime()
    }

    companion object {
        private const val LOCK_AFTER_BACKGROUND_MS = 30_000L
    }
}
