package com.stokakun.app.ui.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stokakun.app.data.AppDatabase
import com.stokakun.app.ui.screens.AddEditAccountScreen
import com.stokakun.app.ui.screens.DetailScreen
import com.stokakun.app.ui.screens.FullscreenImageScreen
import com.stokakun.app.ui.screens.HomeScreen
import com.stokakun.app.ui.screens.SettingsScreen
import com.stokakun.app.ui.screens.StockListScreen
import com.stokakun.app.ui.screens.StorageScreen
import com.stokakun.app.util.AppLockManager
import com.stokakun.app.viewmodel.AccountViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StokAkunNavGraph(viewModel: AccountViewModel) {
    val navController: NavHostController = rememberNavController()
    val context = LocalContext.current
    val lockManager = AppLockManager(context)

    fun NavHostController.navigateSingleTop(route: String) {
        navigate(route) { launchSingleTop = true }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                viewModel,
                { navController.navigateSingleTop(Routes.ADD) },
                { id -> navController.navigateSingleTop(Routes.detail(id)) },
                { navController.navigateSingleTop(Routes.LIST) },
                { navController.navigateSingleTop(Routes.STORAGE) },
                { navController.navigateSingleTop(Routes.SETTINGS) }
            )
        }
        composable(Routes.LIST) {
            StockListScreen(
                viewModel,
                { id -> navController.navigateSingleTop(Routes.detail(id)) },
                { navController.popBackStack() }
            )
        }
        composable(Routes.STORAGE) {
            StorageScreen(
                AppDatabase.getInstance(context).screenshotDao(),
                context,
                { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(lockManager) { navController.popBackStack() }
        }
        composable(Routes.ADD) {
            AddEditAccountScreen(
                viewModel,
                null,
                { navController.popBackStack() },
                { navController.popBackStack() }
            )
        }
        composable(
            Routes.EDIT,
            arguments = listOf(navArgument("accountId") { type = NavType.LongType })
        ) { entry ->
            val accountId = entry.arguments?.getLong("accountId") ?: return@composable
            AddEditAccountScreen(
                viewModel,
                accountId,
                { navController.popBackStack() },
                { navController.popBackStack() }
            )
        }
        composable(
            Routes.DETAIL,
            arguments = listOf(navArgument("accountId") { type = NavType.LongType })
        ) { entry ->
            val accountId = entry.arguments?.getLong("accountId") ?: return@composable
            DetailScreen(
                viewModel,
                accountId,
                { navController.popBackStack() },
                { id -> navController.navigateSingleTop(Routes.edit(id)) },
                { navController.popBackStack() },
                { id, index -> navController.navigateSingleTop(Routes.fullscreen(id, index)) }
            )
        }
        composable(
            Routes.FULLSCREEN,
            arguments = listOf(
                navArgument("accountId") { type = NavType.LongType },
                navArgument("index") { type = NavType.IntType }
            )
        ) { entry ->
            val accountId = entry.arguments?.getLong("accountId") ?: return@composable
            FullscreenImageScreen(
                viewModel,
                accountId,
                entry.arguments?.getInt("index") ?: 0,
                { navController.popBackStack() }
            )
        }
    }
}
