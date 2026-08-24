package com.rzk.kasirpro.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rzk.kasirpro.R
import com.rzk.kasirpro.core.Formatters
import com.rzk.kasirpro.di.AppViewModelProvider
import com.rzk.kasirpro.data.model.SaleWithDetails
import com.rzk.kasirpro.ui.components.BarDatum
import com.rzk.kasirpro.ui.components.BarChart
import com.rzk.kasirpro.ui.components.HeroBalanceCard
import com.rzk.kasirpro.ui.components.KasirCard
import com.rzk.kasirpro.ui.components.SectionHeader
import com.rzk.kasirpro.ui.components.StatCard
import com.rzk.kasirpro.ui.components.StatusPill
import com.rzk.kasirpro.ui.navigation.Routes
import com.rzk.kasirpro.ui.theme.kasirColors

@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val symbol = state.settings.currencySymbol

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { DashboardHeader(state, onOpenSettings = { onNavigate(Routes.SETTINGS) }) }

        item {
            HeroBalanceCard(
                label = stringResource(R.string.cash_on_hand),
                value = Formatters.money(state.cashOnHand, symbol),
                caption = stringResource(R.string.cash_on_hand_hint),
                leading = Icons.Filled.AccountBalanceWallet,
                trailing = {
                    val shift = state.openShift
                    StatusPill(
                        text = if (shift != null) {
                            stringResource(
                                R.string.shift_open_banner,
                                Formatters.time(shift.openedAt)
                            )
                        } else stringResource(R.string.shift_closed_banner),
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        icon = Icons.Filled.Schedule
                    )
                }
            )
        }

        item {
            SectionHeader(
                title = stringResource(R.string.dashboard_today_summary),
                subtitle = stringResource(R.string.vs_previous)
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    label = stringResource(R.string.today_revenue),
                    value = Formatters.compactMoney(state.today.net, symbol),
                    icon = Icons.Filled.ShoppingBag,
                    accent = MaterialTheme.colorScheme.primary,
                    deltaPercent = state.revenueDelta,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = stringResource(R.string.today_orders),
                    value = state.today.orders.toString(),
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    accent = MaterialTheme.colorScheme.tertiary,
                    deltaPercent = state.orderDelta,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    label = stringResource(R.string.today_profit),
                    value = Formatters.compactMoney(state.today.profit, symbol),
                    icon = Icons.Filled.QueryStats,
                    accent = MaterialTheme.kasirColors.cashIn,
                    deltaPercent = state.profitDelta,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = stringResource(R.string.today_items),
                    value = state.today.itemsSold.toString(),
                    icon = Icons.Filled.Inventory,
                    accent = MaterialTheme.kasirColors.warning,
                    supporting = state.topSellerToday?.productName,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item { SectionHeader(title = stringResource(R.string.quick_actions)) }

        item { QuickActionRow(onNavigate) }

        if (state.lowStock.isNotEmpty()) {
            item { LowStockCard(state.lowStock.size, onClick = { onNavigate(Routes.PRODUCTS) }) }
        }

        if (state.livePromoCount > 0) {
            item { PromoBanner(state.livePromoCount, onClick = { onNavigate(Routes.PROMOS) }) }
        }

        item {
            KasirCard {
                SectionHeader(title = stringResource(R.string.sales_last_7_days))
                Spacer(Modifier.height(12.dp))
                BarChart(
                    data = viewModel.weekChartKeys().map { key ->
                        val row = state.weekTotals.firstOrNull { it.dayKey == key }
                        BarDatum(
                            label = Formatters.dayKeyToDayName(key),
                            value = row?.total ?: 0L,
                            display = Formatters.compactMoney(row?.total ?: 0L, symbol)
                        )
                    },
                    emptyLabel = stringResource(R.string.no_data)
                )
            }
        }

        item {
            SectionHeader(
                title = stringResource(R.string.recent_transactions),
                actionLabel = stringResource(R.string.see_all),
                onAction = { onNavigate(Routes.HISTORY) }
            )
        }

        if (state.recentSales.isEmpty()) {
            item {
                KasirCard {
                    Text(
                        stringResource(R.string.no_transactions_yet),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        stringResource(R.string.no_transactions_yet_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(state.recentSales, key = { it.sale.id }) { sale ->
                RecentSaleRow(sale, symbol) { onNavigate(Routes.saleDetail(sale.sale.id)) }
            }
        }
    }
}

@Composable
private fun DashboardHeader(state: DashboardUiState, onOpenSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.dashboard_greeting),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                state.settings.storeName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
        }
    }
}

@Composable
private fun QuickActionRow(onNavigate: (String) -> Unit) {
    val actions = listOf(
        QuickAction(R.string.action_new_sale, Icons.Filled.PointOfSale, Routes.POS),
        QuickAction(R.string.action_cash_in, Icons.Filled.ArrowDownward, Routes.CASHFLOW),
        QuickAction(R.string.action_cash_out, Icons.Filled.ArrowUpward, Routes.CASHFLOW),
        QuickAction(R.string.action_stock_in, Icons.Filled.AddShoppingCart, Routes.PRODUCTS),
        QuickAction(R.string.action_shift, Icons.Filled.Schedule, Routes.SHIFT),
        QuickAction(R.string.action_statistics, Icons.Filled.QueryStats, Routes.STATISTICS)
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(actions) { action ->
            Column(
                modifier = Modifier
                    .width(84.dp)
                    .clickable { onNavigate(action.route) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(56.dp)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        action.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(action.labelRes),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LowStockCard(count: Int, onClick: () -> Unit) {
    AlertCard(
        icon = Icons.Filled.Warning,
        title = stringResource(R.string.low_stock_alert),
        message = stringResource(R.string.low_stock_items, count),
        container = MaterialTheme.kasirColors.warningContainer,
        content = MaterialTheme.kasirColors.onWarningContainer,
        onClick = onClick
    )
}

@Composable
private fun PromoBanner(count: Int, onClick: () -> Unit) {
    AlertCard(
        icon = Icons.Filled.LocalOffer,
        title = stringResource(R.string.promos_title),
        message = stringResource(R.string.promos_live_now, count),
        container = MaterialTheme.kasirColors.promoContainer,
        content = MaterialTheme.kasirColors.onPromoContainer,
        onClick = onClick
    )
}

@Composable
private fun AlertCard(
    icon: ImageVector,
    title: String,
    message: String,
    container: Color,
    content: Color,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(container, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = content)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = content,
                fontWeight = FontWeight.Bold
            )
            Text(message, style = MaterialTheme.typography.bodySmall, color = content)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = content)
    }
}

@Composable
private fun RecentSaleRow(sale: SaleWithDetails, symbol: String, onClick: () -> Unit) {
    KasirCard(
        onClick = onClick,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    sale.sale.invoiceNo,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${Formatters.time(sale.sale.createdAt)} • " +
                        stringResource(R.string.items_count, sale.itemCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                Formatters.money(sale.sale.total, symbol),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private data class QuickAction(val labelRes: Int, val icon: ImageVector, val route: String)
