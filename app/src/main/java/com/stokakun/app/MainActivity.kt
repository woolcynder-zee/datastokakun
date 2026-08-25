package com.stokakun.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stokakun.app.ui.navigation.StokAkunNavGraph
import com.stokakun.app.ui.theme.StokAkunTheme
import com.stokakun.app.viewmodel.AccountViewModel

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as StokAkunApp

        setContent {
            StokAkunTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: AccountViewModel = viewModel(
                        factory = AccountViewModel.Factory(app.repository)
                    )
                    StokAkunNavGraph(viewModel = viewModel)
                }
            }
        }
    }
}
