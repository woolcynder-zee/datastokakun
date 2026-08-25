package com.stokakun.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.stokakun.app.viewmodel.AccountViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenImageScreen(viewModel: AccountViewModel, accountId: Long, initialIndex: Int, onBack: () -> Unit) {
    val screenshots by viewModel.screenshotsFlow(accountId).collectAsState()
    val safeInitialIndex = remember(screenshots, initialIndex) {
        if (screenshots.isEmpty()) 0 else initialIndex.coerceIn(0, screenshots.size - 1)
    }

    if (screenshots.isEmpty()) {
        LaunchedEffect(accountId) { onBack() }
        return
    }

    val pagerState = rememberPagerState(initialPage = safeInitialIndex) { screenshots.size }
    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("${pagerState.currentPage + 1} / ${screenshots.size}") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(),
            modifier = Modifier.fillMaxSize().background(Color.Black).padding(paddingValues)
        ) { page ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val screenshot = screenshots.getOrNull(page)
                if (screenshot != null) {
                    AsyncImage(
                        model = File(screenshot.filePath),
                        contentDescription = "Screenshot ${page + 1}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
