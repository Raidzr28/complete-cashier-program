package com.rzk.kasirpro.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rzk.kasirpro.ui.navigation.Routes
import com.rzk.kasirpro.ui.navigation.SCAN_RESULT_KEY
import com.rzk.kasirpro.ui.navigation.TopLevelDestination
import com.rzk.kasirpro.ui.screens.cashflow.CashFlowScreen
import com.rzk.kasirpro.ui.screens.dashboard.DashboardScreen
import com.rzk.kasirpro.ui.screens.history.HistoryScreen
import com.rzk.kasirpro.ui.screens.history.SaleDetailScreen
import com.rzk.kasirpro.ui.screens.pos.PosScreen
import com.rzk.kasirpro.ui.screens.product.CategoryScreen
import com.rzk.kasirpro.ui.screens.product.ProductEditScreen
import com.rzk.kasirpro.ui.screens.product.ProductListScreen
import com.rzk.kasirpro.ui.screens.promo.PromoEditScreen
import com.rzk.kasirpro.ui.screens.promo.PromoListScreen
import com.rzk.kasirpro.ui.screens.receipt.ReceiptScreen
import com.rzk.kasirpro.ui.screens.report.ReportScreen
import com.rzk.kasirpro.ui.screens.report.StatisticsScreen
import com.rzk.kasirpro.ui.screens.scanner.ScannerScreen
import com.rzk.kasirpro.ui.screens.settings.SettingsScreen
import com.rzk.kasirpro.ui.screens.shift.ShiftScreen
import com.rzk.kasirpro.ui.screens.stock.StockMovementScreen

@Composable
fun KasirApp(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }

    val topLevelRoutes = remember { TopLevelDestination.entries.map { it.route }.toSet() }
    val showBottomBar = currentRoute in topLevelRoutes

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                KasirBottomBar(currentRoute) { destination ->
                    navController.navigate(destination.route) {
                        // Standard bottom-nav behaviour: one entry per tab, state preserved.
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            KasirNavHost(navController, snackbarHostState)
        }
    }
}

@Composable
private fun KasirBottomBar(
    currentRoute: String?,
    onSelect: (TopLevelDestination) -> Unit
) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp
        ) {
            TopLevelDestination.entries.forEach { destination ->
                val selected = currentRoute == destination.route
                NavigationBarItem(
                    selected = selected,
                    onClick = { onSelect(destination) },
                    icon = {
                        Icon(
                            if (selected) destination.selectedIcon else destination.icon,
                            contentDescription = null
                        )
                    },
                    label = { Text(stringResource(destination.labelRes), maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun KasirNavHost(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState
) {
    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD
    ) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(onNavigate = { navController.navigate(it) })
        }

        composable(Routes.POS) { entry ->
            val scanned = entry.rememberScanResult()
            PosScreen(
                scannedBarcode = scanned,
                onScanConsumed = { entry.clearScanResult() },
                onOpenScanner = { navController.navigate(Routes.SCANNER) },
                onCheckoutComplete = { saleId ->
                    navController.navigate(Routes.receipt(saleId))
                },
                onOpenProducts = { navController.navigate(Routes.productEdit(0L)) },
                snackbarHostState = snackbarHostState
            )
        }

        composable(Routes.PRODUCTS) {
            ProductListScreen(
                onAddProduct = { navController.navigate(Routes.productEdit(0L)) },
                onEditProduct = { id -> navController.navigate(Routes.productEdit(id)) },
                onOpenCategories = { navController.navigate(Routes.CATEGORIES) },
                onOpenMovements = { navController.navigate(Routes.MOVEMENTS) },
                snackbarHostState = snackbarHostState
            )
        }

        composable(Routes.CASHFLOW) {
            CashFlowScreen(snackbarHostState = snackbarHostState)
        }

        composable(Routes.REPORTS) {
            ReportScreen(
                onOpenStatistics = { navController.navigate(Routes.STATISTICS) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onOpenShift = { navController.navigate(Routes.SHIFT) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.STATISTICS) {
            StatisticsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                onOpenSale = { id -> navController.navigate(Routes.saleDetail(id)) }
            )
        }

        composable(
            route = Routes.SALE_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_SALE_ID) { type = NavType.LongType })
        ) {
            SaleDetailScreen(
                onBack = { navController.popBackStack() },
                snackbarHostState = snackbarHostState
            )
        }

        composable(
            route = Routes.RECEIPT,
            arguments = listOf(navArgument(Routes.ARG_SALE_ID) { type = NavType.LongType })
        ) {
            ReceiptScreen(
                onDone = {
                    navController.popBackStack(Routes.POS, inclusive = false)
                }
            )
        }

        composable(
            route = Routes.PRODUCT_EDIT,
            arguments = listOf(navArgument(Routes.ARG_PRODUCT_ID) { type = NavType.LongType })
        ) { entry ->
            val scanned = entry.rememberScanResult()
            ProductEditScreen(
                scannedBarcode = scanned,
                onScanConsumed = { entry.clearScanResult() },
                onOpenScanner = { navController.navigate(Routes.SCANNER) },
                onBack = { navController.popBackStack() },
                onGoToExistingProduct = { id ->
                    // Replaces the abandoned "new product" form with the real one, so
                    // back doesn't return to a half-filled duplicate.
                    navController.navigate(Routes.productEdit(id)) {
                        popUpTo(Routes.PRODUCT_EDIT) { inclusive = true }
                    }
                },
                snackbarHostState = snackbarHostState
            )
        }

        composable(Routes.CATEGORIES) {
            CategoryScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.MOVEMENTS) {
            StockMovementScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SHIFT) {
            ShiftScreen(
                onBack = { navController.popBackStack() },
                snackbarHostState = snackbarHostState
            )
        }

        composable(Routes.PROMOS) {
            PromoListScreen(
                onBack = { navController.popBackStack() },
                onEditPromo = { id -> navController.navigate(Routes.promoEdit(id)) }
            )
        }

        composable(
            route = Routes.PROMO_EDIT,
            arguments = listOf(navArgument(Routes.ARG_PROMO_ID) { type = NavType.LongType })
        ) {
            PromoEditScreen(
                onBack = { navController.popBackStack() },
                snackbarHostState = snackbarHostState
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { navController.navigate(it) },
                snackbarHostState = snackbarHostState
            )
        }

        composable(Routes.SCANNER) {
            ScannerScreen(
                onBack = { navController.popBackStack() },
                onBarcodeScanned = { code ->
                    // Hand the result to whoever opened the scanner, then close.
                    navController.previousBackStackEntry
                        ?.savedStateHandle?.set(SCAN_RESULT_KEY, code)
                    navController.popBackStack()
                }
            )
        }
    }
}

/** Reads a barcode handed back by the scanner, as state so the screen recomposes on arrival. */
@Composable
private fun NavBackStackEntry.rememberScanResult(): String? {
    val flow = remember(this) { savedStateHandle.getStateFlow<String?>(SCAN_RESULT_KEY, null) }
    val value by flow.collectAsStateWithLifecycle()
    return value
}

private fun NavBackStackEntry.clearScanResult() {
    savedStateHandle[SCAN_RESULT_KEY] = null
}
