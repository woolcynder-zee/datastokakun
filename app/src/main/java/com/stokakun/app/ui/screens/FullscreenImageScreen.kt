package com.stokakun.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.stokakun.app.viewmodel.AccountViewModel
import coil.compose.AsyncImage
import java.io.File

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun FullscreenImageScreen(
    viewModel: AccountViewModel,
    accountId: Long,
    initialIndex: Int,
    onBack: () -> Unit
) {
    val screenshots by viewModel.screenshotsFlow(accountId).collectAsState()
    if (screenshots.isEmpty()) {
        onBack()
        return
    }
    val safeInitialIndex = initialIndex.coerceIn(0, screenshots.size - 1)
    val pagerState = rememberPagerState(initialPage = safeInitialIndex) { screenshots.size }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("${pagerState.currentPage + 1} / ${screenshots.size}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Black
                )
            )
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black)
        ) { page ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = File(screenshots[page].filePath),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
