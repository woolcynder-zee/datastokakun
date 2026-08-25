package com.stokakun.app.ui.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
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
            HomeScreen(
                viewModel = viewModel,
                onAddClick = { navController.navigate(Routes.ADD) },
                onAccountClick = { id -> navController.navigate(Routes.detail(id)) },
                onSeeAllClick = { navController.navigate(Routes.LIST) }
            )
        }

        composable(Routes.LIST) {
            StockListScreen(
                viewModel = viewModel,
                onAccountClick = { id -> navController.navigate(Routes.detail(id)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ADD) {
            AddEditAccountScreen(
                viewModel = viewModel,
                accountId = null,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.EDIT,
            arguments = listOf(navArgument("accountId") { type = NavType.LongType })
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getLong("accountId") ?: return@composable
            AddEditAccountScreen(
                viewModel = viewModel,
                accountId = accountId,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("accountId") { type = NavType.LongType })
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getLong("accountId") ?: return@composable
            DetailScreen(
                viewModel = viewModel,
                accountId = accountId,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Routes.edit(id)) },
                onDeleted = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
                onImageClick = { id, index -> navController.navigate(Routes.fullscreen(id, index)) }
            )
        }

        composable(
            route = Routes.FULLSCREEN,
            arguments = listOf(
                navArgument("accountId") { type = NavType.LongType },
                navArgument("index") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getLong("accountId") ?: return@composable
            val index = backStackEntry.arguments?.getInt("index") ?: 0
            FullscreenImageScreen(
                viewModel = viewModel,
                accountId = accountId,
                initialIndex = index,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
