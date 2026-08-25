package com.stokakun.app.ui.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stokakun.app.ui.screens.AddEditAccountScreen
import com.stokakun.app.ui.screens.DetailScreen
import com.stokakun.app.ui.screens.FullscreenImageScreen
import com.stokakun.app.ui.screens.HomeScreen
import com.stokakun.app.ui.screens.StockListScreen
import com.stokakun.app.viewmodel.AccountViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StokAkunNavGraph(viewModel: AccountViewModel) {
    val navController: NavHostController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(viewModel, { navController.navigate(Routes.ADD) }, { id -> navController.navigate(Routes.detail(id)) }, { navController.navigate(Routes.LIST) })
        }
        composable(Routes.LIST) {
            StockListScreen(viewModel, { id -> navController.navigate(Routes.detail(id)) }, { navController.popBackStack() })
        }
        composable(Routes.ADD) {
            AddEditAccountScreen(viewModel, null, { navController.popBackStack() }, { navController.popBackStack() })
        }
        composable(Routes.EDIT, arguments = listOf(navArgument("accountId") { type = NavType.LongType })) { entry ->
            val accountId = entry.arguments?.getLong("accountId")
            if (accountId != null && accountId > 0) {
                AddEditAccountScreen(viewModel, accountId, { navController.popBackStack() }, { navController.popBackStack() })
            } else {
                navController.popBackStack()
            }
        }
        composable(Routes.DETAIL, arguments = listOf(navArgument("accountId") { type = NavType.LongType })) { entry ->
            val accountId = entry.arguments?.getLong("accountId")
            if (accountId != null && accountId > 0) {
                DetailScreen(
                    viewModel = viewModel,
                    accountId = accountId,
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(Routes.edit(id)) },
                    onDeleted = { navController.popBackStack() },
                    onImageClick = { id, index -> navController.navigate(Routes.fullscreen(id, index)) }
                )
            } else {
                navController.popBackStack()
            }
        }
        composable(
            Routes.FULLSCREEN,
            arguments = listOf(
                navArgument("accountId") { type = NavType.LongType },
                navArgument("index") { type = NavType.IntType }
            )
        ) { entry ->
            val accountId = entry.arguments?.getLong("accountId")
            val index = entry.arguments?.getInt("index") ?: 0
            if (accountId != null && accountId > 0) {
                FullscreenImageScreen(
                    viewModel = viewModel,
                    accountId = accountId,
                    initialIndex = index,
                    onBack = { navController.popBackStack() }
                )
            } else {
                navController.popBackStack()
            }
        }
    }
}
